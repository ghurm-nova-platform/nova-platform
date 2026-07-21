package ai.nova.platform.merge.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.approval.dto.ApprovalDtos.ApprovalDecision;
import ai.nova.platform.approval.entity.ApprovalDecisionValue;
import ai.nova.platform.approval.service.ApprovalStorageService;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSource;
import ai.nova.platform.audit.service.AuditRecordingSupport;
import ai.nova.platform.ci.dto.CiDtos.CiObservationOperation;
import ai.nova.platform.ci.service.CiStorageService;
import ai.nova.platform.git.dto.GitDtos.GitOperation;
import ai.nova.platform.git.service.GitStorageService;
import ai.nova.platform.merge.config.MergeProperties;
import ai.nova.platform.merge.dto.MergeDtos.MergeOperation;
import ai.nova.platform.merge.dto.MergeDtos.MergeRunRequest;
import ai.nova.platform.merge.entity.MergeEventType;
import ai.nova.platform.merge.entity.MergeMethod;
import ai.nova.platform.merge.entity.MergeStatus;
import ai.nova.platform.merge.provider.MergeProvider;
import ai.nova.platform.merge.provider.MergeProvider.MergeOutcome;
import ai.nova.platform.merge.provider.MergeProvider.MergeRequest;
import ai.nova.platform.merge.provider.MergeProvider.RemotePullRequestState;
import ai.nova.platform.merge.security.MergeAuthorizationService;
import ai.nova.platform.merge.service.MergeRemoteVerifier.VerificationRequest;
import ai.nova.platform.merge.service.MergeRemoteVerifier.VerificationResult;
import ai.nova.platform.merge.service.MergeValidator.MergeValidationContext;
import ai.nova.platform.merge.service.MergeValidator.ValidationOutcome;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.pullrequest.config.PullRequestProperties;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestOperation;
import ai.nova.platform.pullrequest.provider.RepositoryRef;
import ai.nova.platform.pullrequest.service.PullRequestStorageService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

/**
 * Merge Agent: the sole component allowed to merge pull requests after Approval Gate validation.
 * Never bypasses approvals, modifies evidence, reruns CI, deploys, or stores secrets.
 * SUCCEEDED requires successful remote verification; VERIFY_PASSED only after that verification.
 */
@Service
public class MergeAgentService {

    private final MergeAuthorizationService authorizationService;
    private final MergeProperties properties;
    private final PullRequestProperties pullRequestProperties;
    private final AgentOrchestrationTaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ApprovalStorageService approvalStorageService;
    private final PatchStorageService patchStorageService;
    private final GitStorageService gitStorageService;
    private final PullRequestStorageService pullRequestStorageService;
    private final CiStorageService ciStorageService;
    private final MergeValidator mergeValidator;
    private final MergeRemoteVerifier remoteVerifier;
    private final MergeStorageService storageService;
    private final MergeProvider mergeProvider;
    private final AuditRecordingSupport auditRecordingSupport;

    public MergeAgentService(
            MergeAuthorizationService authorizationService,
            MergeProperties properties,
            PullRequestProperties pullRequestProperties,
            AgentOrchestrationTaskRepository taskRepository,
            ProjectRepository projectRepository,
            ApprovalStorageService approvalStorageService,
            PatchStorageService patchStorageService,
            GitStorageService gitStorageService,
            PullRequestStorageService pullRequestStorageService,
            CiStorageService ciStorageService,
            MergeValidator mergeValidator,
            MergeRemoteVerifier remoteVerifier,
            MergeStorageService storageService,
            MergeProvider mergeProvider,
            AuditRecordingSupport auditRecordingSupport) {
        this.authorizationService = authorizationService;
        this.properties = properties;
        this.pullRequestProperties = pullRequestProperties;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.approvalStorageService = approvalStorageService;
        this.patchStorageService = patchStorageService;
        this.gitStorageService = gitStorageService;
        this.pullRequestStorageService = pullRequestStorageService;
        this.ciStorageService = ciStorageService;
        this.mergeValidator = mergeValidator;
        this.remoteVerifier = remoteVerifier;
        this.storageService = storageService;
        this.mergeProvider = mergeProvider;
        this.auditRecordingSupport = auditRecordingSupport;
    }

    public MergeOperation run(MergeRunRequest request, AuthenticatedUser user) {
        authorizationService.require(user, MergeAuthorizationService.MERGE_RUN);
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "MERGE_DISABLED", "Merge agent is disabled");
        }
        if (request == null || request.taskId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MERGE_INVALID_REQUEST", "Task id is required");
        }

        AgentOrchestrationTask task = requireTask(request.taskId(), user.getOrganizationId());
        projectRepository
                .findByIdAndOrganizationId(task.getProjectId(), user.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MERGE_TASK_NOT_FOUND", "Project not found"));

        ApprovalDecision approval = approvalStorageService.findLatest(task.getId(), user.getOrganizationId());
        requireApprovedDecision(approval);

        PatchResult patch = patchStorageService.findLatest(task.getId(), user.getOrganizationId());
        GitOperation git = gitStorageService.findLatest(task.getId(), user.getOrganizationId());
        PullRequestOperation pullRequest = pullRequestStorageService.findLatest(task.getId(), user.getOrganizationId());
        CiObservationOperation ci = ciStorageService.findLatest(task.getId(), user.getOrganizationId());

        if (patch == null || git == null || pullRequest == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "MERGE_VALIDATION_FAILED", "Required pipeline evidence is missing");
        }

        MergeMethod mergeMethod = resolveMergeMethod(request.mergeMethod());
        boolean eligible = approval.eligibleForMerge();

        MergeOperation existingSucceeded =
                storageService.findSucceededByTaskAndApproval(task.getId(), user.getOrganizationId(), approval.id(), eligible);
        if (existingSucceeded != null) {
            return existingSucceeded;
        }

        MergeOperation alreadyMerged = detectAlreadyMerged(task, approval, pullRequest, mergeMethod, user, eligible);
        if (alreadyMerged != null) {
            return alreadyMerged;
        }

        UUID operationId = resolveOperationId(task, approval, user.getOrganizationId());
        Instant startedAt = Instant.now();
        String headSha = resolveHeadSha(pullRequest);
        if (operationId == null) {
            operationId = UUID.randomUUID();
            storageService.startPending(
                    operationId,
                    task,
                    approval,
                    patch,
                    git,
                    pullRequest,
                    ci,
                    mergeMethod,
                    headSha,
                    startedAt);
        } else {
            storageService.updateStatus(operationId, MergeStatus.PENDING);
        }

        Instant now = Instant.now();
        storageService.appendEvent(operationId, MergeEventType.VALIDATION_STARTED, "Pre-merge validation started", now);
        storageService.updateStatus(operationId, MergeStatus.VALIDATING);

        ValidationOutcome validation = mergeValidator.validate(new MergeValidationContext(
                task,
                approval,
                patch,
                git,
                pullRequest,
                ci,
                mergeMethod,
                properties,
                mergeProvider,
                resolveGithubToken(),
                now));
        storageService.saveValidations(operationId, validation.checks(), now);

        if (!validation.passed()) {
            storageService.appendEvent(
                    operationId, MergeEventType.VALIDATION_FAILED, validation.errorMessage(), Instant.now());
            publishAudit(user, task, operationId, AuditAction.FAIL, AuditResult.FAILURE, Map.of("errorCode", validation.errorCode()));
            return storageService.markFailed(operationId, validation.errorCode(), validation.errorMessage(), eligible);
        }

        storageService.appendEvent(operationId, MergeEventType.VALIDATION_PASSED, "All blocking checks passed", Instant.now());
        publishAudit(user, task, operationId, AuditAction.VALIDATE, AuditResult.SUCCESS, Map.of("operationId", operationId.toString()));
        storageService.updateStatus(operationId, MergeStatus.MERGING);
        storageService.appendEvent(operationId, MergeEventType.MERGE_STARTED, "Calling merge provider", Instant.now());

        RepositoryRef repository =
                new RepositoryRef("github.com", pullRequest.repositoryOwner(), pullRequest.repositoryName());
        long prNumber = pullRequest.pullRequestNumber() != null
                ? pullRequest.pullRequestNumber()
                : approval.pullRequestNumber();
        String prUrl = pullRequest.pullRequestUrl() != null ? pullRequest.pullRequestUrl() : approval.pullRequestUrl();

        MergeOutcome outcome;
        try {
            outcome = mergeProvider.merge(
                    new MergeRequest(repository, prNumber, prUrl, headSha, pullRequest.targetBranch(), mergeMethod),
                    resolveGithubToken());
        } catch (ApiException ex) {
            storageService.appendEvent(operationId, MergeEventType.MERGE_FAILED, ex.getMessage(), Instant.now());
            return storageService.markFailed(
                    operationId,
                    ex.getCode() != null ? ex.getCode() : "MERGE_PROVIDER_FAILED",
                    ex.getMessage(),
                    eligible);
        }

        storageService.updateStatus(operationId, MergeStatus.VERIFYING);
        storageService.appendEvent(operationId, MergeEventType.VERIFY_STARTED, "Verifying merge outcome remotely", Instant.now());

        if (outcome == null) {
            return failVerification(operationId, "MERGE_VERIFICATION_FAILED", "Merge provider returned null outcome", eligible);
        }

        VerificationRequest verificationRequest = new VerificationRequest(
                repository,
                prNumber,
                headSha,
                pullRequest.repositoryOwner(),
                pullRequest.repositoryName(),
                outcome.mergeCommitSha(),
                outcome.alreadyMerged());

        VerificationResult verified;
        try {
            if (outcome.ambiguous()) {
                verified = remoteVerifier.resolveAmbiguous(verificationRequest, resolveGithubToken());
            } else if (!outcome.succeeded() && !outcome.alreadyMerged()) {
                return failVerification(
                        operationId,
                        "MERGE_PROVIDER_FAILED",
                        outcome.providerMessage() != null
                                ? outcome.providerMessage()
                                : "Merge provider did not succeed",
                        eligible);
            } else {
                // Always perform remote verification before SUCCEEDED / VERIFY_PASSED.
                verified = remoteVerifier.verify(verificationRequest, resolveGithubToken());
            }
        } catch (ApiException ex) {
            storageService.appendEvent(operationId, MergeEventType.VERIFY_FAILED, ex.getMessage(), Instant.now());
            return storageService.markFailed(
                    operationId,
                    ex.getCode() != null ? ex.getCode() : "MERGE_VERIFICATION_FAILED",
                    ex.getMessage(),
                    eligible);
        }

        // VERIFY_PASSED only after successful remote verification.
        storageService.appendEvent(
                operationId,
                MergeEventType.VERIFY_PASSED,
                "Remote merge verified (mergeCommit=" + verified.mergeCommitSha() + ")",
                Instant.now());

        MergeOutcome persisted = MergeRemoteVerifier.toPersistedOutcome(verified, outcome);
        MergeOperation result = storageService.markSucceeded(
                operationId,
                mergeProvider.providerId(),
                persisted.mergeCommitSha(),
                persisted.pullRequestUrl() != null ? persisted.pullRequestUrl() : prUrl,
                user.getUserId(),
                persisted.providerMessage(),
                persisted.alreadyMerged(),
                eligible);
        publishAudit(user, task, operationId, AuditAction.MERGE, AuditResult.SUCCESS, Map.of("operationId", operationId.toString()));
        return result;
    }

    public MergeOperation getLatest(UUID taskId, AuthenticatedUser user) {
        authorizationService.require(user, MergeAuthorizationService.MERGE_READ);
        requireTask(taskId, user.getOrganizationId());
        ApprovalDecision approval = approvalStorageService.findLatest(taskId, user.getOrganizationId());
        boolean eligible = approval != null && approval.eligibleForMerge();
        MergeOperation latest = storageService.findLatest(taskId, user.getOrganizationId(), eligible);
        if (latest == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MERGE_TASK_NOT_FOUND", "No merge operation found for task");
        }
        return latest;
    }

    public List<MergeOperation> getHistory(UUID taskId, AuthenticatedUser user) {
        authorizationService.require(user, MergeAuthorizationService.MERGE_READ);
        requireTask(taskId, user.getOrganizationId());
        ApprovalDecision approval = approvalStorageService.findLatest(taskId, user.getOrganizationId());
        boolean eligible = approval != null && approval.eligibleForMerge();
        return storageService.findHistory(taskId, user.getOrganizationId(), eligible);
    }

    private MergeOperation detectAlreadyMerged(
            AgentOrchestrationTask task,
            ApprovalDecision approval,
            PullRequestOperation pullRequest,
            MergeMethod mergeMethod,
            AuthenticatedUser user,
            boolean eligible) {
        if (pullRequest.pullRequestNumber() == null
                || pullRequest.repositoryOwner() == null
                || pullRequest.repositoryName() == null) {
            return null;
        }
        RepositoryRef repository =
                new RepositoryRef("github.com", pullRequest.repositoryOwner(), pullRequest.repositoryName());
        try {
            RemotePullRequestState remote = mergeProvider.getPullRequest(
                    repository, pullRequest.pullRequestNumber(), resolveGithubToken());
            if (remote == null || !remote.isMerged()) {
                return null;
            }
            MergeOperation existing =
                    storageService.findByTaskAndApproval(task.getId(), user.getOrganizationId(), approval.id(), eligible);
            if (existing != null && existing.status() == MergeStatus.SUCCEEDED) {
                return existing;
            }

            String approvedHead = resolveHeadSha(pullRequest);
            UUID operationId = existing != null ? existing.id() : UUID.randomUUID();
            Instant startedAt = Instant.now();
            PatchResult patch = patchStorageService.findLatest(task.getId(), user.getOrganizationId());
            GitOperation git = gitStorageService.findLatest(task.getId(), user.getOrganizationId());
            CiObservationOperation ci = ciStorageService.findLatest(task.getId(), user.getOrganizationId());
            if (existing == null) {
                storageService.startPending(
                        operationId,
                        task,
                        approval,
                        patch,
                        git,
                        pullRequest,
                        ci,
                        mergeMethod,
                        approvedHead,
                        startedAt);
            }
            storageService.appendEvent(
                    operationId, MergeEventType.ALREADY_MERGED, "Pull request already merged remotely", Instant.now());
            storageService.updateStatus(operationId, MergeStatus.VERIFYING);
            storageService.appendEvent(operationId, MergeEventType.VERIFY_STARTED, "Verifying already-merged remote state", Instant.now());

            try {
                VerificationResult verified = remoteVerifier.verify(
                        new VerificationRequest(
                                repository,
                                pullRequest.pullRequestNumber(),
                                approvedHead,
                                pullRequest.repositoryOwner(),
                                pullRequest.repositoryName(),
                                remote.mergeCommitSha(),
                                true),
                        resolveGithubToken());

                storageService.appendEvent(
                        operationId,
                        MergeEventType.VERIFY_PASSED,
                        "Already-merged remote state verified (mergeCommit=" + verified.mergeCommitSha() + ")",
                        Instant.now());
                return storageService.markSucceeded(
                        operationId,
                        mergeProvider.providerId(),
                        verified.mergeCommitSha(),
                        verified.pullRequestUrl() != null ? verified.pullRequestUrl() : remote.url(),
                        user.getUserId(),
                        "Pull request already merged",
                        true,
                        eligible);
            } catch (ApiException ex) {
                storageService.appendEvent(operationId, MergeEventType.VERIFY_FAILED, ex.getMessage(), Instant.now());
                return storageService.markFailed(
                        operationId,
                        ex.getCode() != null ? ex.getCode() : "MERGE_VERIFICATION_FAILED",
                        ex.getMessage(),
                        eligible);
            }
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private MergeOperation failVerification(UUID operationId, String code, String message, boolean eligible) {
        storageService.appendEvent(operationId, MergeEventType.VERIFY_FAILED, message, Instant.now());
        MergeOperation failed = storageService.markFailed(operationId, code, message, eligible);
        return failed;
    }

    private void publishAudit(
            AuthenticatedUser user,
            AgentOrchestrationTask task,
            UUID operationId,
            AuditAction action,
            AuditResult result,
            Map<String, Object> details) {
        try {
            Map<String, Object> enriched = new java.util.LinkedHashMap<>(details);
            if (operationId != null && !enriched.containsKey("operationId")) {
                enriched.put("operationId", operationId.toString());
            }
            auditRecordingSupport.recordDomainEvent(
                    user,
                    task.getProjectId(),
                    AuditEntityType.MERGE,
                    operationId,
                    task.getDisplayName(),
                    action,
                    result,
                    AuditSource.MERGE_AGENT,
                    enriched);
        } catch (RuntimeException ignored) {
            // AuditPublisher swallows failures; guard against unexpected propagation.
        }
    }

    private UUID resolveOperationId(AgentOrchestrationTask task, ApprovalDecision approval, UUID organizationId) {
        MergeOperation existing =
                storageService.findByTaskAndApproval(task.getId(), organizationId, approval.id(), approval.eligibleForMerge());
        if (existing == null || existing.status() == MergeStatus.SUCCEEDED) {
            return null;
        }
        return existing.id();
    }

    private void requireApprovedDecision(ApprovalDecision approval) {
        if (approval == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "MERGE_APPROVAL_REQUIRED", "Approved decision is required to merge");
        }
        if (approval.decision() == ApprovalDecisionValue.EXPIRED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MERGE_APPROVAL_EXPIRED", "Approval decision has expired");
        }
        if (approval.decision() == ApprovalDecisionValue.INVALIDATED) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "MERGE_APPROVAL_INVALIDATED", "Approval decision was invalidated");
        }
        if (approval.decision() == ApprovalDecisionValue.SUPERSEDED) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "MERGE_APPROVAL_SUPERSEDED", "Approval decision was superseded");
        }
        if (approval.decision() != ApprovalDecisionValue.APPROVED || !approval.eligibleForMerge()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "MERGE_APPROVAL_REQUIRED", "Approved decision is required to merge");
        }
        if (approval.validUntil() != null && !Instant.now().isBefore(approval.validUntil())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MERGE_APPROVAL_EXPIRED", "Approval decision has expired");
        }
    }

    private MergeMethod resolveMergeMethod(MergeMethod requested) {
        if (requested != null) {
            return requested;
        }
        return properties.getDefaultMethod() != null ? properties.getDefaultMethod() : MergeMethod.SQUASH;
    }

    private String resolveHeadSha(PullRequestOperation pullRequest) {
        if (pullRequest.remoteCommitHash() != null && !pullRequest.remoteCommitHash().isBlank()) {
            return pullRequest.remoteCommitHash();
        }
        return pullRequest.localCommitHash();
    }

    private String resolveGithubToken() {
        String token = properties.getGithubToken();
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        String prToken = pullRequestProperties.getGithubToken();
        if (prToken != null && !prToken.isBlank()) {
            return prToken.trim();
        }
        return "";
    }

    private AgentOrchestrationTask requireTask(UUID taskId, UUID organizationId) {
        return taskRepository
                .findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MERGE_TASK_NOT_FOUND", "Task not found"));
    }
}
