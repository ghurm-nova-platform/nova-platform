package ai.nova.platform.approval.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.approval.config.ApprovalGateProperties;
import ai.nova.platform.approval.dto.ApprovalDtos.ApprovalDecision;
import ai.nova.platform.approval.dto.ApprovalDtos.ApprovalRequirement;
import ai.nova.platform.approval.dto.ApprovalDtos.ApprovalRunRequest;
import ai.nova.platform.approval.dto.ApprovalDtos.HumanActionRequest;
import ai.nova.platform.approval.entity.ApprovalDecisionEntity;
import ai.nova.platform.approval.entity.ApprovalDecisionValue;
import ai.nova.platform.approval.entity.ApprovalHumanActionType;
import ai.nova.platform.approval.entity.ApprovalOperationStatus;
import ai.nova.platform.approval.entity.ApprovalPolicyEntity;
import ai.nova.platform.approval.policy.ApprovalPolicyEvaluator;
import ai.nova.platform.approval.policy.ApprovalPolicyService;
import ai.nova.platform.approval.policy.RuleEvaluationResult;
import ai.nova.platform.approval.repository.ApprovalDecisionRepository;
import ai.nova.platform.approval.security.ApprovalAuthorizationService;
import ai.nova.platform.approval.service.ApprovalDecisionCalculator.DecisionCalculation;
import ai.nova.platform.approval.service.ApprovalStaleGuard.StaleCheckResult;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class ApprovalGateService {

    private final ApprovalAuthorizationService authorizationService;
    private final ApprovalGateProperties properties;
    private final AgentOrchestrationTaskRepository taskRepository;
    private final ApprovalPolicyService policyService;
    private final ApprovalEvidenceCollector evidenceCollector;
    private final ApprovalPolicyEvaluator policyEvaluator;
    private final ApprovalFingerprint fingerprint;
    private final ApprovalDecisionCalculator decisionCalculator;
    private final ApprovalStorageService storageService;
    private final ApprovalStaleGuard staleGuard;
    private final ApprovalCommentSanitizer commentSanitizer;
    private final ApprovalDecisionRepository decisionRepository;

    public ApprovalGateService(
            ApprovalAuthorizationService authorizationService,
            ApprovalGateProperties properties,
            AgentOrchestrationTaskRepository taskRepository,
            ApprovalPolicyService policyService,
            ApprovalEvidenceCollector evidenceCollector,
            ApprovalPolicyEvaluator policyEvaluator,
            ApprovalFingerprint fingerprint,
            ApprovalDecisionCalculator decisionCalculator,
            ApprovalStorageService storageService,
            ApprovalStaleGuard staleGuard,
            ApprovalCommentSanitizer commentSanitizer,
            ApprovalDecisionRepository decisionRepository) {
        this.authorizationService = authorizationService;
        this.properties = properties;
        this.taskRepository = taskRepository;
        this.policyService = policyService;
        this.evidenceCollector = evidenceCollector;
        this.policyEvaluator = policyEvaluator;
        this.fingerprint = fingerprint;
        this.decisionCalculator = decisionCalculator;
        this.storageService = storageService;
        this.staleGuard = staleGuard;
        this.commentSanitizer = commentSanitizer;
        this.decisionRepository = decisionRepository;
    }

    @Transactional
    public ApprovalDecision run(ApprovalRunRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ApprovalAuthorizationService.APPROVAL_GATE_RUN);
        if (!properties.isEnabled()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE, "APPROVAL_GATE_DISABLED", "Approval gate is disabled");
        }

        AgentOrchestrationTask task = requireTask(request.taskId(), user.getOrganizationId());
        ApprovalPolicyEntity policy = policyService.requireActiveDefaultPolicy(
                user.getOrganizationId(), task.getProjectId());

        UUID operationId = UUID.randomUUID();
        Instant startedAt = Instant.now();
        storageService.startPending(operationId, task, policy, startedAt);
        storageService.updateOperationStatus(operationId, ApprovalOperationStatus.COLLECTING, null, null);

        ApprovalEvidenceBundle bundle = evidenceCollector.collect(task);
        storageService.updateOperationStatus(operationId, ApprovalOperationStatus.EVALUATING, null, null);

        List<RuleEvaluationResult> rules = policyEvaluator.evaluate(bundle, policy, properties);
        int requiredHumanApprovals = resolveRequiredHumanApprovals(policy);
        int receivedApprovals =
                storageService.countApprovalsForFingerprint(user.getOrganizationId(), task.getId(), fingerprint.evidenceFingerprint(bundle));
        int rejectionCount =
                storageService.countRejectionsForFingerprint(user.getOrganizationId(), task.getId(), fingerprint.evidenceFingerprint(bundle));

        String evidenceFp = fingerprint.evidenceFingerprint(bundle);
        String decisionFp = fingerprint.decisionFingerprint(evidenceFp, receivedApprovals, rejectionCount, requiredHumanApprovals);

        ApprovalDecision existing = findIdempotentDecision(task, evidenceFp, decisionFp, requiredHumanApprovals, receivedApprovals, rejectionCount);
        if (existing != null) {
            storageService.updateOperationStatus(
                    operationId,
                    existing.decision() == ApprovalDecisionValue.REQUIRES_HUMAN_APPROVAL
                            ? ApprovalOperationStatus.WAITING_FOR_HUMAN
                            : ApprovalOperationStatus.SUCCEEDED,
                    existing.decision(),
                    Instant.now());
            return existing;
        }

        DecisionCalculation calculation =
                decisionCalculator.calculate(rules, requiredHumanApprovals, receivedApprovals, rejectionCount);

        if (!bundle.hasMinimumPersistenceEvidence()) {
            storageService.markFailed(operationId, "APPROVAL_EVIDENCE_INCOMPLETE", calculation.reasonSummary());
            return buildTransientDecision(
                    task, policy, bundle, evidenceFp, decisionFp, calculation, requiredHumanApprovals, receivedApprovals, rejectionCount, rules);
        }

        Instant validUntil = resolveValidUntil(policy, startedAt);
        UUID decisionId = UUID.randomUUID();
        storageService.persistDecision(
                decisionId,
                operationId,
                task,
                policy,
                bundle,
                evidenceFp,
                decisionFp,
                calculation.decision(),
                calculation.eligibleForMerge(),
                requiredHumanApprovals,
                receivedApprovals,
                rejectionCount,
                calculation.reasonSummary(),
                validUntil,
                rules,
                user.getUserId());

        ApprovalOperationStatus opStatus = calculation.decision() == ApprovalDecisionValue.REQUIRES_HUMAN_APPROVAL
                ? ApprovalOperationStatus.WAITING_FOR_HUMAN
                : ApprovalOperationStatus.SUCCEEDED;
        storageService.updateOperationStatus(operationId, opStatus, calculation.decision(), Instant.now());
        return storageService.findLatest(task.getId(), user.getOrganizationId());
    }

    @Transactional(readOnly = true)
    public ApprovalDecision getLatest(UUID taskId, AuthenticatedUser user) {
        authorizationService.require(user, ApprovalAuthorizationService.APPROVAL_GATE_READ);
        requireTask(taskId, user.getOrganizationId());
        return storageService.requireLatest(taskId, user.getOrganizationId());
    }

    @Transactional(readOnly = true)
    public List<ApprovalDecision> getHistory(UUID taskId, AuthenticatedUser user) {
        authorizationService.require(user, ApprovalAuthorizationService.APPROVAL_GATE_READ);
        requireTask(taskId, user.getOrganizationId());
        return storageService.findHistory(taskId, user.getOrganizationId());
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequirement> getRequirements(UUID taskId, AuthenticatedUser user) {
        authorizationService.require(user, ApprovalAuthorizationService.APPROVAL_GATE_READ);
        requireTask(taskId, user.getOrganizationId());
        ApprovalDecision decision = storageService.requireLatest(taskId, user.getOrganizationId());
        return decision.requirements();
    }

    @Transactional
    public ApprovalDecision approve(UUID taskId, HumanActionRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ApprovalAuthorizationService.APPROVAL_GATE_APPROVE);
        AgentOrchestrationTask task = requireTask(taskId, user.getOrganizationId());
        ApprovalDecisionEntity decisionEntity = decisionRepository
                .findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(taskId, user.getOrganizationId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "APPROVAL_DECISION_NOT_FOUND", "No approval decision to approve"));

        StaleCheckResult stale = staleGuard.revalidate(taskId, user.getOrganizationId(), decisionEntity);
        if (!stale.isValid()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    stale.overrideDecision() == ApprovalDecisionValue.EXPIRED
                            ? "APPROVAL_DECISION_EXPIRED"
                            : "APPROVAL_DECISION_INVALIDATED",
                    stale.reason());
        }

        ApprovalPolicyEntity policy = policyService.requirePolicy(decisionEntity.getPolicyId(), user.getOrganizationId());
        if (policy.isProhibitAuthorApproval() && task.getCreatedBy().equals(user.getUserId())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, "APPROVAL_AUTHOR_CANNOT_APPROVE", "Task author cannot approve");
        }

        if (request != null && request.idempotencyKey() != null
                && storageService.idempotencyKeyExists(
                        user.getOrganizationId(), taskId, user.getUserId(), request.idempotencyKey())) {
            return storageService.findLatest(taskId, user.getOrganizationId());
        }

        if (storageService.hasActiveApprovalByActor(
                user.getOrganizationId(), taskId, decisionEntity.getEvidenceFingerprint(), user.getUserId())) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "APPROVAL_DUPLICATE_APPROVAL", "Duplicate approval by same actor");
        }

        commentSanitizer.sanitize(request != null ? request.comment() : null);

        storageService.appendHumanAction(
                UUID.randomUUID(),
                task,
                decisionEntity.getId(),
                user.getUserId(),
                ApprovalHumanActionType.APPROVE,
                request != null ? request.comment() : null,
                decisionEntity.getEvidenceFingerprint(),
                request != null ? request.idempotencyKey() : null);

        return recalculateHumanDecision(task, policy, decisionEntity, user);
    }

    @Transactional
    public ApprovalDecision reject(UUID taskId, HumanActionRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ApprovalAuthorizationService.APPROVAL_GATE_REJECT);
        AgentOrchestrationTask task = requireTask(taskId, user.getOrganizationId());
        ApprovalDecisionEntity decisionEntity = decisionRepository
                .findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(taskId, user.getOrganizationId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "APPROVAL_DECISION_NOT_FOUND", "No approval decision to reject"));

        StaleCheckResult stale = staleGuard.revalidate(taskId, user.getOrganizationId(), decisionEntity);
        if (!stale.isValid()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    stale.overrideDecision() == ApprovalDecisionValue.EXPIRED
                            ? "APPROVAL_DECISION_EXPIRED"
                            : "APPROVAL_DECISION_INVALIDATED",
                    stale.reason());
        }

        String comment = commentSanitizer.sanitizeRequired(request != null ? request.comment() : null, true);
        ApprovalPolicyEntity policy = policyService.requirePolicy(decisionEntity.getPolicyId(), user.getOrganizationId());

        if (request != null && request.idempotencyKey() != null
                && storageService.idempotencyKeyExists(
                        user.getOrganizationId(), taskId, user.getUserId(), request.idempotencyKey())) {
            return storageService.findLatest(taskId, user.getOrganizationId());
        }

        storageService.appendHumanAction(
                UUID.randomUUID(),
                task,
                decisionEntity.getId(),
                user.getUserId(),
                ApprovalHumanActionType.REJECT,
                comment,
                decisionEntity.getEvidenceFingerprint(),
                request != null ? request.idempotencyKey() : null);

        return recalculateHumanDecision(task, policy, decisionEntity, user);
    }

    private ApprovalDecision recalculateHumanDecision(
            AgentOrchestrationTask task,
            ApprovalPolicyEntity policy,
            ApprovalDecisionEntity priorDecision,
            AuthenticatedUser user) {
        ApprovalEvidenceBundle bundle = evidenceCollector.collect(task);
        List<RuleEvaluationResult> rules = policyEvaluator.evaluate(bundle, policy, properties);
        int requiredHumanApprovals = resolveRequiredHumanApprovals(policy);
        int receivedApprovals = storageService.countApprovalsForFingerprint(
                user.getOrganizationId(), task.getId(), priorDecision.getEvidenceFingerprint());
        int rejectionCount = storageService.countRejectionsForFingerprint(
                user.getOrganizationId(), task.getId(), priorDecision.getEvidenceFingerprint());
        String evidenceFp = priorDecision.getEvidenceFingerprint();
        String decisionFp = fingerprint.decisionFingerprint(evidenceFp, receivedApprovals, rejectionCount, requiredHumanApprovals);

        ApprovalDecision existing = findIdempotentDecision(
                task, evidenceFp, decisionFp, requiredHumanApprovals, receivedApprovals, rejectionCount);
        if (existing != null) {
            return existing;
        }

        DecisionCalculation calculation =
                decisionCalculator.calculate(rules, requiredHumanApprovals, receivedApprovals, rejectionCount);
        UUID operationId = priorDecision.getApprovalGateOperationId();
        UUID decisionId = UUID.randomUUID();
        storageService.persistDecision(
                decisionId,
                operationId,
                task,
                policy,
                bundle,
                evidenceFp,
                decisionFp,
                calculation.decision(),
                calculation.eligibleForMerge(),
                requiredHumanApprovals,
                receivedApprovals,
                rejectionCount,
                calculation.reasonSummary(),
                priorDecision.getValidUntil(),
                rules,
                user.getUserId());

        ApprovalOperationStatus opStatus = calculation.decision() == ApprovalDecisionValue.REQUIRES_HUMAN_APPROVAL
                ? ApprovalOperationStatus.WAITING_FOR_HUMAN
                : ApprovalOperationStatus.SUCCEEDED;
        storageService.updateOperationStatus(operationId, opStatus, calculation.decision(), Instant.now());
        return storageService.findLatest(task.getId(), user.getOrganizationId());
    }

    private ApprovalDecision findIdempotentDecision(
            AgentOrchestrationTask task,
            String evidenceFp,
            String decisionFp,
            int requiredHumanApprovals,
            int receivedApprovals,
            int rejectionCount) {
        return decisionRepository
                .findByOrganizationIdAndTaskIdAndEvidenceFingerprintAndDecisionFingerprint(
                        task.getOrganizationId(), task.getId(), evidenceFp, decisionFp)
                .map(entity -> {
                    if (entity.getValidUntil() != null && Instant.now().isAfter(entity.getValidUntil())) {
                        return null;
                    }
                    if (entity.getRequiredHumanApprovals() == requiredHumanApprovals
                            && entity.getReceivedHumanApprovals() == receivedApprovals
                            && entity.getRejectionCount() == rejectionCount) {
                        return storageService.findLatest(task.getId(), task.getOrganizationId());
                    }
                    return null;
                })
                .orElse(null);
    }

    private ApprovalDecision buildTransientDecision(
            AgentOrchestrationTask task,
            ApprovalPolicyEntity policy,
            ApprovalEvidenceBundle bundle,
            String evidenceFp,
            String decisionFp,
            DecisionCalculation calculation,
            int requiredHumanApprovals,
            int receivedApprovals,
            int rejectionCount,
            List<RuleEvaluationResult> rules) {
        List<ApprovalRequirement> requirements = rules.stream()
                .map(r -> new ApprovalRequirement(
                        null,
                        r.ruleCode(),
                        r.description(),
                        r.expectedValue(),
                        r.actualValue(),
                        r.result(),
                        r.blocking(),
                        r.severity(),
                        r.failureReason(),
                        Instant.now()))
                .toList();
        return new ApprovalDecision(
                null,
                task.getId(),
                task.getProjectId(),
                calculation.decision(),
                calculation.eligibleForMerge(),
                policy.getId(),
                policy.getName(),
                policy.getVersion(),
                evidenceFp,
                decisionFp,
                bundle.patch() != null ? bundle.patch().id() : null,
                bundle.computedPatchHash(),
                bundle.git() != null ? bundle.git().id() : null,
                bundle.git() != null ? bundle.git().commitHash() : null,
                bundle.pullRequest() != null ? bundle.pullRequest().id() : null,
                bundle.pullRequest() != null ? bundle.pullRequest().pullRequestNumber() : null,
                bundle.pullRequest() != null ? bundle.pullRequest().pullRequestUrl() : null,
                bundle.ci() != null ? bundle.ci().id() : null,
                bundle.ci() != null && bundle.ci().overallStatus() != null
                        ? bundle.ci().overallStatus().name()
                        : null,
                bundle.repair() != null ? bundle.repair().id() : null,
                requiredHumanApprovals,
                receivedApprovals,
                rejectionCount,
                calculation.reasonSummary(),
                null,
                java.util.Map.of(),
                List.of(),
                List.of(),
                requirements,
                List.of(),
                Instant.now(),
                Instant.now());
    }

    private int resolveRequiredHumanApprovals(ApprovalPolicyEntity policy) {
        return policy.getRequiredHumanApprovals() >= 0
                ? policy.getRequiredHumanApprovals()
                : properties.getDefaultRequiredHumanApprovals();
    }

    private Instant resolveValidUntil(ApprovalPolicyEntity policy, Instant startedAt) {
        Integer minutes = policy.getDecisionValidityMinutes();
        if (minutes == null) {
            minutes = properties.getDefaultDecisionValidityMinutes();
        }
        return startedAt.plus(minutes, ChronoUnit.MINUTES);
    }

    private AgentOrchestrationTask requireTask(UUID taskId, UUID organizationId) {
        return taskRepository
                .findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "APPROVAL_TASK_NOT_FOUND", "Task not found for approval gate"));
    }
}
