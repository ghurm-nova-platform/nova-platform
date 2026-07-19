package ai.nova.platform.merge.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.approval.dto.ApprovalDtos.ApprovalDecision;
import ai.nova.platform.ci.dto.CiDtos.CiObservationOperation;
import ai.nova.platform.git.dto.GitDtos.GitOperation;
import ai.nova.platform.merge.dto.MergeDtos.MergeOperation;
import ai.nova.platform.merge.dto.MergeDtos.MergeResultView;
import ai.nova.platform.merge.dto.MergeDtos.MergeValidationCheck;
import ai.nova.platform.merge.dto.MergeDtos.TimelineEvent;
import ai.nova.platform.merge.entity.MergeEventEntity;
import ai.nova.platform.merge.entity.MergeEventType;
import ai.nova.platform.merge.entity.MergeMethod;
import ai.nova.platform.merge.entity.MergeOperationEntity;
import ai.nova.platform.merge.entity.MergeResultEntity;
import ai.nova.platform.merge.entity.MergeStatus;
import ai.nova.platform.merge.entity.MergeValidationEntity;
import ai.nova.platform.merge.repository.MergeEventRepository;
import ai.nova.platform.merge.repository.MergeOperationRepository;
import ai.nova.platform.merge.repository.MergeResultRepository;
import ai.nova.platform.merge.repository.MergeValidationRepository;
import ai.nova.platform.merge.service.MergeValidator.ValidationCheck;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestOperation;
import ai.nova.platform.web.error.ApiException;

@Service
public class MergeStorageService {

    private final MergeOperationRepository operationRepository;
    private final MergeValidationRepository validationRepository;
    private final MergeResultRepository resultRepository;
    private final MergeEventRepository eventRepository;

    public MergeStorageService(
            MergeOperationRepository operationRepository,
            MergeValidationRepository validationRepository,
            MergeResultRepository resultRepository,
            MergeEventRepository eventRepository) {
        this.operationRepository = operationRepository;
        this.validationRepository = validationRepository;
        this.resultRepository = resultRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public MergeOperationEntity startPending(
            UUID operationId,
            AgentOrchestrationTask task,
            ApprovalDecision approval,
            PatchResult patch,
            GitOperation git,
            PullRequestOperation pullRequest,
            CiObservationOperation ci,
            MergeMethod mergeMethod,
            String expectedPrHeadSha,
            Instant startedAt) {
        Instant now = Instant.now();
        MergeOperationEntity entity = new MergeOperationEntity(
                operationId,
                task.getOrganizationId(),
                task.getProjectId(),
                task.getId(),
                approval.id(),
                pullRequest.id(),
                git.id(),
                patch.id(),
                ci != null ? ci.id() : approval.ciObservationOperationId(),
                MergeStatus.PENDING,
                mergeMethod,
                approval.evidenceFingerprint(),
                approval.decisionFingerprint(),
                approval.patchHash(),
                approval.commitHash(),
                expectedPrHeadSha,
                pullRequest.pullRequestNumber() != null ? pullRequest.pullRequestNumber() : approval.pullRequestNumber(),
                pullRequest.repositoryOwner(),
                pullRequest.repositoryName(),
                null,
                null,
                startedAt,
                null,
                now,
                now);
        operationRepository.save(entity);
        appendEvent(operationId, MergeEventType.OPERATION_CREATED, "Merge operation created", now);
        return entity;
    }

    @Transactional
    public MergeOperationEntity updateStatus(UUID operationId, MergeStatus status) {
        MergeOperationEntity operation = requireOperation(operationId);
        operation.setStatus(status);
        operation.setUpdatedAt(Instant.now());
        return operationRepository.save(operation);
    }

    @Transactional
    public void saveValidations(UUID operationId, List<ValidationCheck> checks, Instant evaluatedAt) {
        validationRepository.deleteByMergeOperationId(operationId);
        for (ValidationCheck check : checks) {
            validationRepository.save(new MergeValidationEntity(
                    UUID.randomUUID(),
                    operationId,
                    check.checkCode(),
                    truncate(check.expectedValue(), 2000),
                    truncate(check.actualValue(), 2000),
                    check.result(),
                    truncate(check.failureReason(), 2000),
                    evaluatedAt));
        }
    }

    @Transactional
    public MergeOperation markSucceeded(
            UUID operationId,
            String provider,
            String mergedCommit,
            String pullRequestUrl,
            UUID mergedByUserId,
            String providerMessage,
            boolean alreadyMerged,
            boolean eligibleForMerge) {
        Instant now = Instant.now();
        MergeOperationEntity operation = requireOperation(operationId);
        operation.setStatus(MergeStatus.SUCCEEDED);
        operation.setCompletedAt(now);
        operation.setUpdatedAt(now);
        operationRepository.save(operation);

        MergeResultEntity existing = resultRepository.findByMergeOperationId(operationId).orElse(null);
        if (existing == null) {
            resultRepository.save(new MergeResultEntity(
                    UUID.randomUUID(),
                    operationId,
                    operation.getMergeMethod(),
                    mergedCommit,
                    operation.getPullRequestNumber(),
                    pullRequestUrl,
                    now,
                    mergedByUserId,
                    provider,
                    providerMessage,
                    now));
        }

        appendEvent(
                operationId,
                alreadyMerged ? MergeEventType.ALREADY_MERGED : MergeEventType.MERGE_SUCCEEDED,
                alreadyMerged ? "Pull request was already merged" : "Merge succeeded",
                now);
        appendEvent(operationId, MergeEventType.COMPLETED, "Merge operation completed", now);
        return reloadOperation(operationId, true, eligibleForMerge);
    }

    @Transactional
    public MergeOperation markFailed(
            UUID operationId, String errorCode, String errorMessage, boolean eligibleForMerge) {
        Instant now = Instant.now();
        MergeOperationEntity operation = requireOperation(operationId);
        operation.setStatus(MergeStatus.FAILED);
        operation.setErrorCode(errorCode);
        operation.setErrorMessage(truncate(errorMessage, 2000));
        operation.setCompletedAt(now);
        operation.setUpdatedAt(now);
        operationRepository.save(operation);
        appendEvent(operationId, MergeEventType.MERGE_FAILED, truncate(errorMessage, 2000), now);
        appendEvent(operationId, MergeEventType.COMPLETED, "Merge operation failed", now);
        return reloadOperation(operationId, true, eligibleForMerge);
    }

    @Transactional(readOnly = true)
    public MergeOperation reload(UUID operationId, boolean eligibleForMerge) {
        return reloadOperation(operationId, true, eligibleForMerge);
    }

    @Transactional(readOnly = true)
    public MergeOperation findLatest(UUID taskId, UUID organizationId, boolean eligibleForMerge) {
        return operationRepository
                .findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(taskId, organizationId)
                .map(entity -> reloadOperation(entity.getId(), true, eligibleForMerge))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<MergeOperation> findHistory(UUID taskId, UUID organizationId, boolean eligibleForMerge) {
        return operationRepository.findByTaskIdAndOrganizationIdOrderByCreatedAtDesc(taskId, organizationId).stream()
                .map(entity -> reloadOperation(entity.getId(), true, eligibleForMerge))
                .toList();
    }

    @Transactional(readOnly = true)
    public MergeOperation findSucceededByTaskAndApproval(
            UUID taskId, UUID organizationId, UUID approvalDecisionId, boolean eligibleForMerge) {
        return operationRepository
                .findFirstByOrganizationIdAndTaskIdAndApprovalDecisionIdAndStatus(
                        organizationId, taskId, approvalDecisionId, MergeStatus.SUCCEEDED)
                .map(entity -> reloadOperation(entity.getId(), true, eligibleForMerge))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public MergeOperation findByTaskAndApproval(
            UUID taskId, UUID organizationId, UUID approvalDecisionId, boolean eligibleForMerge) {
        return operationRepository
                .findByOrganizationIdAndTaskIdAndApprovalDecisionId(organizationId, taskId, approvalDecisionId)
                .map(entity -> reloadOperation(entity.getId(), true, eligibleForMerge))
                .orElse(null);
    }

    @Transactional
    public void appendEvent(UUID operationId, MergeEventType eventType, String detail, Instant at) {
        eventRepository.save(new MergeEventEntity(UUID.randomUUID(), operationId, eventType, truncate(detail, 2000), at));
    }

    private MergeOperation reloadOperation(UUID operationId, boolean includeDetails, boolean eligibleForMerge) {
        MergeOperationEntity operation = requireOperation(operationId);
        List<MergeValidationCheck> validations = validationRepository
                .findByMergeOperationIdOrderByEvaluatedAtAsc(operationId)
                .stream()
                .map(v -> new MergeValidationCheck(
                        v.getId(),
                        v.getCheckCode(),
                        v.getExpectedValue(),
                        v.getActualValue(),
                        v.getResult(),
                        v.getFailureReason(),
                        v.getEvaluatedAt()))
                .toList();
        List<TimelineEvent> timeline = eventRepository.findByMergeOperationIdOrderByCreatedAtAsc(operationId).stream()
                .map(e -> new TimelineEvent(e.getEventType().name(), e.getCreatedAt(), e.getDetail()))
                .toList();
        MergeResultView result = resultRepository.findByMergeOperationId(operationId).map(this::toResultView).orElse(null);
        return new MergeOperation(
                operation.getId(),
                operation.getTaskId(),
                operation.getProjectId(),
                operation.getStatus(),
                operation.getMergeMethod(),
                operation.getApprovalDecisionId(),
                eligibleForMerge,
                operation.getPullRequestOperationId(),
                operation.getGitOperationId(),
                operation.getPatchResultId(),
                operation.getCiObservationOperationId(),
                operation.getPullRequestNumber(),
                result != null ? result.pullRequestUrl() : null,
                operation.getRepositoryOwner(),
                operation.getRepositoryName(),
                operation.getEvidenceFingerprint(),
                operation.getDecisionFingerprint(),
                operation.getExpectedPatchHash(),
                operation.getExpectedCommitHash(),
                operation.getExpectedPrHeadSha(),
                operation.getErrorCode(),
                operation.getErrorMessage(),
                validations,
                timeline,
                result,
                operation.getStartedAt(),
                operation.getCompletedAt(),
                operation.getCreatedAt(),
                operation.getUpdatedAt());
    }

    private MergeResultView toResultView(MergeResultEntity entity) {
        return new MergeResultView(
                entity.getId(),
                entity.getMergeOperationId(),
                entity.getMergeMethod(),
                entity.getMergedCommit(),
                entity.getPullRequestNumber(),
                entity.getPullRequestUrl(),
                entity.getMergedAt(),
                entity.getMergedByUserId(),
                entity.getProvider(),
                entity.getProviderMessage(),
                entity.getCreatedAt());
    }

    private MergeOperationEntity requireOperation(UUID operationId) {
        return operationRepository
                .findById(operationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "MERGE_TASK_NOT_FOUND", "Merge operation not found"));
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
