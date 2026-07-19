package ai.nova.platform.approval.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.approval.dto.ApprovalDtos.ApprovalDecision;
import ai.nova.platform.approval.dto.ApprovalDtos.ApprovalEvidenceRef;
import ai.nova.platform.approval.dto.ApprovalDtos.ApprovalHumanActionView;
import ai.nova.platform.approval.dto.ApprovalDtos.ApprovalRequirement;
import ai.nova.platform.approval.dto.ApprovalDtos.ApprovalTimelineEvent;
import ai.nova.platform.approval.entity.ApprovalDecisionEntity;
import ai.nova.platform.approval.entity.ApprovalDecisionEventEntity;
import ai.nova.platform.approval.entity.ApprovalDecisionValue;
import ai.nova.platform.approval.entity.ApprovalEvidenceEntity;
import ai.nova.platform.approval.entity.ApprovalEvidenceType;
import ai.nova.platform.approval.entity.ApprovalGateOperationEntity;
import ai.nova.platform.approval.entity.ApprovalHumanActionEntity;
import ai.nova.platform.approval.entity.ApprovalHumanActionType;
import ai.nova.platform.approval.entity.ApprovalOperationStatus;
import ai.nova.platform.approval.entity.ApprovalPolicyEntity;
import ai.nova.platform.approval.entity.ApprovalRequirementEntity;
import ai.nova.platform.approval.entity.ApprovalRequirementResult;
import ai.nova.platform.approval.policy.RuleEvaluationResult;
import ai.nova.platform.approval.repository.ApprovalDecisionEventRepository;
import ai.nova.platform.approval.repository.ApprovalDecisionRepository;
import ai.nova.platform.approval.repository.ApprovalEvidenceRepository;
import ai.nova.platform.approval.repository.ApprovalGateOperationRepository;
import ai.nova.platform.approval.repository.ApprovalHumanActionRepository;
import ai.nova.platform.approval.repository.ApprovalPolicyRepository;
import ai.nova.platform.approval.repository.ApprovalRequirementRepository;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.web.error.ApiException;

@Service
public class ApprovalStorageService {

    private final ApprovalGateOperationRepository operationRepository;
    private final ApprovalDecisionRepository decisionRepository;
    private final ApprovalEvidenceRepository evidenceRepository;
    private final ApprovalRequirementRepository requirementRepository;
    private final ApprovalHumanActionRepository humanActionRepository;
    private final ApprovalDecisionEventRepository eventRepository;
    private final ApprovalPolicyRepository policyRepository;

    public ApprovalStorageService(
            ApprovalGateOperationRepository operationRepository,
            ApprovalDecisionRepository decisionRepository,
            ApprovalEvidenceRepository evidenceRepository,
            ApprovalRequirementRepository requirementRepository,
            ApprovalHumanActionRepository humanActionRepository,
            ApprovalDecisionEventRepository eventRepository,
            ApprovalPolicyRepository policyRepository) {
        this.operationRepository = operationRepository;
        this.decisionRepository = decisionRepository;
        this.evidenceRepository = evidenceRepository;
        this.requirementRepository = requirementRepository;
        this.humanActionRepository = humanActionRepository;
        this.eventRepository = eventRepository;
        this.policyRepository = policyRepository;
    }

    @Transactional
    public ApprovalGateOperationEntity startPending(
            UUID operationId,
            AgentOrchestrationTask task,
            ApprovalPolicyEntity policy,
            Instant startedAt) {
        Instant now = Instant.now();
        ApprovalGateOperationEntity operation = new ApprovalGateOperationEntity(
                operationId,
                task.getOrganizationId(),
                task.getProjectId(),
                task.getId(),
                policy.getId(),
                policy.getVersion(),
                ApprovalOperationStatus.PENDING,
                null,
                startedAt,
                null,
                null,
                null,
                now,
                now);
        return operationRepository.save(operation);
    }

    @Transactional
    public ApprovalGateOperationEntity updateOperationStatus(
            UUID operationId, ApprovalOperationStatus status, ApprovalDecisionValue decision, Instant completedAt) {
        ApprovalGateOperationEntity operation = requireOperation(operationId);
        operation.updateStatus(status);
        operation.setDecision(decision);
        if (completedAt != null) {
            operation.setCompletedAt(completedAt);
        }
        operation.setUpdatedAt(Instant.now());
        return operationRepository.save(operation);
    }

    @Transactional
    public ApprovalGateOperationEntity markFailed(UUID operationId, String errorCode, String errorMessage) {
        ApprovalGateOperationEntity operation = requireOperation(operationId);
        operation.updateStatus(ApprovalOperationStatus.FAILED);
        operation.setDecision(ApprovalDecisionValue.ERROR);
        operation.setErrorCode(errorCode);
        operation.setErrorMessage(errorMessage);
        operation.setCompletedAt(Instant.now());
        operation.setUpdatedAt(Instant.now());
        return operationRepository.save(operation);
    }

    @Transactional
    public ApprovalDecisionEntity persistDecision(
            UUID decisionId,
            UUID operationId,
            AgentOrchestrationTask task,
            ApprovalPolicyEntity policy,
            ApprovalEvidenceBundle bundle,
            String evidenceFingerprint,
            String decisionFingerprint,
            ApprovalDecisionValue decision,
            boolean eligibleForMerge,
            int requiredHumanApprovals,
            int receivedHumanApprovals,
            int rejectionCount,
            String reasonSummary,
            Instant validUntil,
            List<RuleEvaluationResult> rules,
            UUID actorUserId) {
        Instant now = Instant.now();
        ApprovalDecisionEntity entity = new ApprovalDecisionEntity(
                decisionId,
                task.getOrganizationId(),
                task.getProjectId(),
                task.getId(),
                operationId,
                policy.getId(),
                policy.getVersion(),
                decision,
                eligibleForMerge,
                requiredHumanApprovals,
                receivedHumanApprovals,
                rejectionCount,
                evidenceFingerprint,
                decisionFingerprint,
                bundle.patch().id(),
                bundle.computedPatchHash(),
                bundle.git().id(),
                bundle.git().commitHash(),
                bundle.pullRequest().id(),
                bundle.pullRequest().pullRequestNumber() != null ? bundle.pullRequest().pullRequestNumber() : 0L,
                bundle.pullRequest().pullRequestUrl(),
                bundle.ci() != null ? bundle.ci().id() : null,
                bundle.ci() != null && bundle.ci().overallStatus() != null
                        ? bundle.ci().overallStatus().name()
                        : null,
                bundle.repair() != null ? bundle.repair().id() : null,
                reasonSummary,
                validUntil,
                decision == ApprovalDecisionValue.APPROVED ? now : null,
                decision == ApprovalDecisionValue.REJECTED ? now : null,
                null,
                null,
                now,
                now);
        decisionRepository.save(entity);

        persistEvidence(decisionId, bundle, now);
        persistRequirements(decisionId, rules, now);
        appendEvent(decisionId, "OPERATION_CREATED", "Approval gate operation created", actorUserId, now);
        appendEvent(decisionId, "EVIDENCE_COLLECTED", "Evidence collected from storage", null, now);
        for (RuleEvaluationResult rule : rules) {
            appendEvent(
                    decisionId,
                    "RULE_EVALUATED",
                    rule.ruleCode() + "=" + rule.result(),
                    null,
                    now);
        }
        if (decision == ApprovalDecisionValue.BLOCKED) {
            appendEvent(decisionId, "AUTOMATED_GATE_BLOCKED", reasonSummary, null, now);
        } else if (decision == ApprovalDecisionValue.REQUIRES_HUMAN_APPROVAL
                || decision == ApprovalDecisionValue.APPROVED) {
            appendEvent(decisionId, "AUTOMATED_GATE_PASSED", reasonSummary, null, now);
        }
        if (decision == ApprovalDecisionValue.APPROVED) {
            appendEvent(decisionId, "DECISION_APPROVED", reasonSummary, actorUserId, now);
        }
        if (decision == ApprovalDecisionValue.REJECTED) {
            appendEvent(decisionId, "DECISION_REJECTED", reasonSummary, actorUserId, now);
        }
        return entity;
    }

    @Transactional
    public ApprovalHumanActionEntity appendHumanAction(
            UUID actionId,
            AgentOrchestrationTask task,
            UUID decisionId,
            UUID actorUserId,
            ApprovalHumanActionType action,
            String comment,
            String evidenceFingerprint,
            String idempotencyKey) {
        Instant now = Instant.now();
        ApprovalHumanActionEntity entity = new ApprovalHumanActionEntity(
                actionId,
                task.getOrganizationId(),
                task.getProjectId(),
                task.getId(),
                decisionId,
                actorUserId,
                action,
                comment,
                evidenceFingerprint,
                idempotencyKey,
                now);
        humanActionRepository.save(entity);
        String eventType =
                switch (action) {
                    case APPROVE -> "HUMAN_APPROVAL_ADDED";
                    case REJECT -> "HUMAN_REJECTION_ADDED";
                    case WITHDRAW_APPROVAL -> "HUMAN_APPROVAL_WITHDRAWN";
                };
        appendEvent(decisionId, eventType, action.name(), actorUserId, now);
        return entity;
    }

    @Transactional(readOnly = true)
    public ApprovalDecision findLatest(UUID taskId, UUID organizationId) {
        return decisionRepository
                .findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(taskId, organizationId)
                .map(this::reloadDecision)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ApprovalDecision> findHistory(UUID taskId, UUID organizationId) {
        return decisionRepository.findByTaskIdAndOrganizationIdOrderByCreatedAtDesc(taskId, organizationId).stream()
                .map(this::reloadDecision)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApprovalDecision requireLatest(UUID taskId, UUID organizationId) {
        ApprovalDecision decision = findLatest(taskId, organizationId);
        if (decision == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND, "APPROVAL_DECISION_NOT_FOUND", "No approval decision found for task");
        }
        return decision;
    }

    @Transactional(readOnly = true)
    public ApprovalDecisionEntity requireDecisionEntity(UUID decisionId, UUID organizationId) {
        return decisionRepository
                .findByIdAndOrganizationId(decisionId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "APPROVAL_DECISION_NOT_FOUND", "Approval decision not found"));
    }

    @Transactional(readOnly = true)
    public int countApprovalsForFingerprint(UUID organizationId, UUID taskId, String evidenceFingerprint) {
        return (int) humanActionRepository
                .findByOrganizationIdAndTaskIdAndEvidenceFingerprintOrderByCreatedAtAsc(
                        organizationId, taskId, evidenceFingerprint)
                .stream()
                .filter(a -> a.getAction() == ApprovalHumanActionType.APPROVE)
                .map(ApprovalHumanActionEntity::getActorUserId)
                .distinct()
                .count();
    }

    @Transactional(readOnly = true)
    public int countRejectionsForFingerprint(UUID organizationId, UUID taskId, String evidenceFingerprint) {
        return (int) humanActionRepository
                .findByOrganizationIdAndTaskIdAndEvidenceFingerprintOrderByCreatedAtAsc(
                        organizationId, taskId, evidenceFingerprint)
                .stream()
                .filter(a -> a.getAction() == ApprovalHumanActionType.REJECT)
                .count();
    }

    @Transactional(readOnly = true)
    public boolean hasActiveApprovalByActor(
            UUID organizationId, UUID taskId, String evidenceFingerprint, UUID actorUserId) {
        return humanActionRepository.existsByOrganizationIdAndTaskIdAndEvidenceFingerprintAndActorUserIdAndAction(
                organizationId, taskId, evidenceFingerprint, actorUserId, ApprovalHumanActionType.APPROVE);
    }

    @Transactional(readOnly = true)
    public boolean idempotencyKeyExists(
            UUID organizationId, UUID taskId, UUID actorUserId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }
        return humanActionRepository.existsByOrganizationIdAndTaskIdAndActorUserIdAndIdempotencyKey(
                organizationId, taskId, actorUserId, idempotencyKey);
    }

    private ApprovalDecision reloadDecision(ApprovalDecisionEntity entity) {
        UUID decisionId = entity.getId();
        List<ApprovalRequirement> requirements = requirementRepository
                .findByApprovalDecisionIdOrderByEvaluatedAtAsc(decisionId)
                .stream()
                .map(this::toRequirement)
                .toList();
        List<ApprovalEvidenceRef> evidence = evidenceRepository
                .findByApprovalDecisionIdOrderByCreatedAtAsc(decisionId)
                .stream()
                .map(this::toEvidence)
                .toList();
        List<ApprovalTimelineEvent> events = eventRepository
                .findByApprovalDecisionIdOrderByCreatedAtAsc(decisionId)
                .stream()
                .map(e -> new ApprovalTimelineEvent(e.getEventType(), e.getDetail(), e.getActorUserId(), e.getCreatedAt()))
                .toList();
        List<ApprovalHumanActionView> humanActions = humanActionRepository
                .findByApprovalDecisionIdOrderByCreatedAtAsc(decisionId)
                .stream()
                .map(a -> new ApprovalHumanActionView(
                        a.getId(),
                        a.getActorUserId(),
                        a.getAction(),
                        a.getCommentText(),
                        a.getEvidenceFingerprint(),
                        a.getCreatedAt()))
                .toList();

        String policyName = policyRepository
                .findById(entity.getPolicyId())
                .map(ApprovalPolicyEntity::getName)
                .orElse(null);

        Map<String, Long> summaryCounts = new HashMap<>();
        for (ApprovalRequirementResult result : ApprovalRequirementResult.values()) {
            long count = requirements.stream().filter(r -> r.result() == result).count();
            summaryCounts.put(result.name(), count);
        }

        return new ApprovalDecision(
                entity.getId(),
                entity.getTaskId(),
                entity.getProjectId(),
                entity.getDecision(),
                entity.isEligibleForMerge(),
                entity.getPolicyId(),
                policyName,
                entity.getPolicyVersion(),
                entity.getEvidenceFingerprint(),
                entity.getDecisionFingerprint(),
                entity.getPatchResultId(),
                entity.getPatchHash(),
                entity.getGitOperationId(),
                entity.getCommitHash(),
                entity.getPullRequestOperationId(),
                entity.getPullRequestNumber(),
                entity.getPullRequestUrl(),
                entity.getCiObservationOperationId(),
                entity.getCiOverallStatus(),
                entity.getRepairOperationId(),
                entity.getRequiredHumanApprovals(),
                entity.getReceivedHumanApprovals(),
                entity.getRejectionCount(),
                entity.getReasonSummary(),
                entity.getValidUntil(),
                summaryCounts,
                events,
                humanActions,
                requirements,
                evidence,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private void persistEvidence(UUID decisionId, ApprovalEvidenceBundle bundle, Instant now) {
        if (bundle.review() != null) {
            saveEvidence(decisionId, ApprovalEvidenceType.REVIEW, bundle.review().id(), bundle.review().id(), null, null,
                    String.valueOf(bundle.review().approved()), String.valueOf(bundle.review().score()), now);
        }
        if (bundle.testing() != null) {
            saveEvidence(decisionId, ApprovalEvidenceType.TESTING, bundle.testing().id(), bundle.testing().id(), null,
                    null, String.valueOf(bundle.testing().validated()), bundle.testing().summary(), now);
        }
        if (bundle.patch() != null) {
            saveEvidence(decisionId, ApprovalEvidenceType.PATCH, bundle.patch().id(), bundle.patch().id(), null,
                    bundle.computedPatchHash(), bundle.patch().status().name(), null, now);
        }
        if (bundle.git() != null) {
            saveEvidence(decisionId, ApprovalEvidenceType.GIT, bundle.git().id(), bundle.git().patchResultId(), null,
                    bundle.git().patchHash(), bundle.git().status().name(), bundle.git().commitHash(), now);
        }
        if (bundle.pullRequest() != null) {
            saveEvidence(decisionId, ApprovalEvidenceType.PULL_REQUEST, bundle.pullRequest().id(),
                    bundle.pullRequest().patchResultId(), null, bundle.pullRequest().patchHash(),
                    bundle.pullRequest().status().name(),
                    bundle.pullRequest().pullRequestRecord() != null
                            ? bundle.pullRequest().pullRequestRecord().state()
                            : null,
                    now);
        }
        if (bundle.ci() != null) {
            saveEvidence(decisionId, ApprovalEvidenceType.CI, bundle.ci().id(), bundle.ci().pullRequestOperationId(),
                    null, bundle.ci().commitHash(),
                    bundle.ci().overallStatus() != null ? bundle.ci().overallStatus().name() : null, null, now);
        }
        if (bundle.repair() != null) {
            saveEvidence(decisionId, ApprovalEvidenceType.REPAIR, bundle.repair().id(), bundle.repair().priorPatchResultId(),
                    null, null, bundle.repair().status().name(), bundle.repair().summary(), now);
        }
    }

    private void saveEvidence(
            UUID decisionId,
            ApprovalEvidenceType type,
            UUID sourceOperationId,
            UUID sourceResultId,
            String sourceVersion,
            String sourceHash,
            String observedStatus,
            String observedValue,
            Instant now) {
        evidenceRepository.save(new ApprovalEvidenceEntity(
                UUID.randomUUID(),
                decisionId,
                type,
                sourceOperationId,
                sourceResultId,
                sourceVersion,
                sourceHash,
                observedStatus,
                observedValue,
                now));
    }

    private void persistRequirements(UUID decisionId, List<RuleEvaluationResult> rules, Instant now) {
        for (RuleEvaluationResult rule : rules) {
            requirementRepository.save(new ApprovalRequirementEntity(
                    UUID.randomUUID(),
                    decisionId,
                    rule.ruleCode(),
                    rule.description(),
                    rule.expectedValue(),
                    rule.actualValue(),
                    rule.result(),
                    rule.blocking(),
                    rule.severity(),
                    rule.failureReason(),
                    now));
        }
    }

    private void appendEvent(UUID decisionId, String eventType, String detail, UUID actorUserId, Instant now) {
        eventRepository.save(new ApprovalDecisionEventEntity(
                UUID.randomUUID(), decisionId, eventType, detail, actorUserId, now));
    }

    private ApprovalRequirement toRequirement(ApprovalRequirementEntity entity) {
        return new ApprovalRequirement(
                entity.getId(),
                entity.getRuleCode(),
                entity.getDescription(),
                entity.getExpectedValue(),
                entity.getActualValue(),
                entity.getResult(),
                entity.isBlocking(),
                entity.getSeverity(),
                entity.getFailureReason(),
                entity.getEvaluatedAt());
    }

    private ApprovalEvidenceRef toEvidence(ApprovalEvidenceEntity entity) {
        return new ApprovalEvidenceRef(
                entity.getId(),
                entity.getEvidenceType(),
                entity.getSourceOperationId(),
                entity.getSourceResultId(),
                entity.getSourceVersion(),
                entity.getSourceHash(),
                entity.getObservedStatus(),
                entity.getObservedValue(),
                entity.getCreatedAt());
    }

    private ApprovalGateOperationEntity requireOperation(UUID operationId) {
        return operationRepository
                .findById(operationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "APPROVAL_OPERATION_NOT_FOUND", "Approval operation not found"));
    }
}
