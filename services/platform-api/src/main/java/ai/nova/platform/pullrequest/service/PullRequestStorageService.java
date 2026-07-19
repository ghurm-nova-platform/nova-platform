package ai.nova.platform.pullrequest.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestOperation;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestRecord;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestValidation;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.RemotePushResult;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.TimelineEvent;
import ai.nova.platform.pullrequest.entity.PullRequestOperationEntity;
import ai.nova.platform.pullrequest.entity.PullRequestRecordEntity;
import ai.nova.platform.pullrequest.entity.PullRequestStatus;
import ai.nova.platform.pullrequest.entity.RemotePushEntity;
import ai.nova.platform.pullrequest.entity.RemotePushStatus;
import ai.nova.platform.pullrequest.provider.ProviderPullRequest;
import ai.nova.platform.pullrequest.repository.PullRequestOperationRepository;
import ai.nova.platform.pullrequest.repository.PullRequestRecordRepository;
import ai.nova.platform.pullrequest.repository.RemotePushRepository;
import ai.nova.platform.pullrequest.service.ProjectRepositoryConfigService.ResolvedRepositoryConfig;
import ai.nova.platform.web.error.ApiException;

@Service
public class PullRequestStorageService {

    private final PullRequestOperationRepository operationRepository;
    private final RemotePushRepository remotePushRepository;
    private final PullRequestRecordRepository recordRepository;

    public PullRequestStorageService(
            PullRequestOperationRepository operationRepository,
            RemotePushRepository remotePushRepository,
            PullRequestRecordRepository recordRepository) {
        this.operationRepository = operationRepository;
        this.remotePushRepository = remotePushRepository;
        this.recordRepository = recordRepository;
    }

    @Transactional
    public PullRequestOperation startPending(
            UUID operationId,
            AgentOrchestrationTask task,
            UUID gitOperationId,
            UUID patchResultId,
            ResolvedRepositoryConfig config,
            String sourceBranch,
            String localCommitHash,
            String patchHash,
            Instant startedAt,
            List<TimelineEvent> timeline) {
        Instant now = Instant.now();
        PullRequestOperationEntity operation = new PullRequestOperationEntity(
                operationId,
                task.getOrganizationId(),
                task.getProjectId(),
                task.getId(),
                gitOperationId,
                patchResultId,
                PullRequestStatus.PENDING,
                config.effectiveProvider(),
                config.repositoryRef().owner(),
                config.repositoryRef().name(),
                config.remoteName(),
                config.remoteUrl(),
                sourceBranch,
                config.targetBranch(),
                localCommitHash,
                null,
                patchHash,
                null,
                null,
                null,
                null,
                null,
                startedAt,
                null,
                now,
                now);
        operationRepository.save(operation);
        return toOperation(operation, null, null, timeline == null ? List.of() : timeline);
    }

    @Transactional
    public PullRequestOperation updateStatus(
            UUID operationId, PullRequestStatus status, List<TimelineEvent> timeline) {
        PullRequestOperationEntity operation = requireOperation(operationId);
        operation.updateStatus(status);
        operationRepository.save(operation);
        return reloadOperation(operation.getId(), timeline);
    }

    @Transactional
    public PullRequestOperation markPushed(
            UUID operationId,
            String remoteCommitHash,
            String remoteName,
            String sourceBranch,
            String localCommitHash,
            RemotePushStatus pushStatus,
            Instant pushStartedAt,
            Instant pushCompletedAt,
            List<TimelineEvent> timeline) {
        PullRequestOperationEntity operation = requireOperation(operationId);
        operation.markPushed(remoteCommitHash);
        operationRepository.save(operation);

        RemotePushEntity push = new RemotePushEntity(
                UUID.randomUUID(),
                operationId,
                remoteName,
                sourceBranch,
                localCommitHash,
                remoteCommitHash,
                pushStatus,
                pushStartedAt,
                pushCompletedAt,
                null,
                null);
        remotePushRepository.save(push);
        return reloadOperation(operationId, timeline);
    }

    @Transactional
    public PullRequestOperation markSucceeded(
            UUID operationId,
            ProviderPullRequest providerPullRequest,
            String remoteCommitHash,
            Instant completedAt,
            List<TimelineEvent> timeline) {
        PullRequestOperationEntity operation = requireOperation(operationId);
        operation.markSucceeded(
                providerPullRequest.number(),
                providerPullRequest.url(),
                providerPullRequest.title(),
                remoteCommitHash,
                completedAt);
        operationRepository.save(operation);

        PullRequestRecordEntity record = new PullRequestRecordEntity(
                UUID.randomUUID(),
                operationId,
                operation.getProvider(),
                providerPullRequest.externalId(),
                providerPullRequest.number(),
                providerPullRequest.url(),
                providerPullRequest.title(),
                providerPullRequest.sourceBranch(),
                providerPullRequest.targetBranch(),
                providerPullRequest.state(),
                completedAt);
        recordRepository.save(record);
        return reloadOperation(operationId, timeline);
    }

    @Transactional
    public PullRequestOperation markFailed(
            UUID operationId, String errorCode, String message, Instant completedAt, List<TimelineEvent> timeline) {
        PullRequestOperationEntity operation = requireOperation(operationId);
        if (operation.getStatus() == PullRequestStatus.SUCCEEDED) {
            throw new ApiException(HttpStatus.CONFLICT, "PR_ALREADY_EXISTS", "Pull request operation already succeeded");
        }
        operation.markFailed(errorCode, message, completedAt);
        operationRepository.save(operation);
        return reloadOperation(operationId, timeline);
    }

    @Transactional(readOnly = true)
    public PullRequestOperation findLatest(UUID taskId, UUID organizationId) {
        return operationRepository
                .findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(taskId, organizationId)
                .map(operation -> reloadOperation(operation.getId(), null))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<PullRequestOperation> findHistory(UUID taskId, UUID organizationId) {
        return operationRepository.findByTaskIdAndOrganizationIdOrderByCreatedAtDesc(taskId, organizationId).stream()
                .map(operation -> reloadOperation(operation.getId(), null))
                .toList();
    }

    @Transactional(readOnly = true)
    public PullRequestOperation findSucceededByGitOperationId(UUID gitOperationId) {
        return operationRepository
                .findFirstByGitOperationIdAndStatus(gitOperationId, PullRequestStatus.SUCCEEDED)
                .map(operation -> reloadOperation(operation.getId(), null))
                .orElse(null);
    }

    private PullRequestOperation reloadOperation(UUID operationId, List<TimelineEvent> timelineOverride) {
        PullRequestOperationEntity operation = requireOperation(operationId);
        RemotePushResult remotePush = remotePushRepository
                .findFirstByPullRequestOperationIdOrderByStartedAtDesc(operationId)
                .map(PullRequestStorageService::toRemotePush)
                .orElse(null);
        PullRequestRecord record = recordRepository
                .findFirstByPullRequestOperationIdOrderByCreatedAtDesc(operationId)
                .map(PullRequestStorageService::toRecord)
                .orElse(null);
        List<TimelineEvent> timeline = timelineOverride == null ? buildTimeline(operation, remotePush, record) : timelineOverride;
        return toOperation(operation, remotePush, record, timeline);
    }

    private PullRequestOperationEntity requireOperation(UUID operationId) {
        return operationRepository
                .findById(operationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PR_NOT_FOUND", "Pull request operation not found"));
    }

    private static PullRequestOperation toOperation(
            PullRequestOperationEntity operation,
            RemotePushResult remotePush,
            PullRequestRecord record,
            List<TimelineEvent> timeline) {
        boolean ok = operation.getStatus() == PullRequestStatus.SUCCEEDED;
        return new PullRequestOperation(
                operation.getId(),
                operation.getTaskId(),
                operation.getProjectId(),
                operation.getGitOperationId(),
                operation.getPatchResultId(),
                operation.getStatus(),
                operation.getProvider(),
                operation.getRepositoryOwner(),
                operation.getRepositoryName(),
                operation.getRemoteName(),
                operation.getRemoteUrl(),
                operation.getSourceBranch(),
                operation.getTargetBranch(),
                operation.getLocalCommitHash(),
                operation.getRemoteCommitHash(),
                operation.getPatchHash(),
                operation.getPullRequestNumber(),
                operation.getPullRequestUrl(),
                operation.getPullRequestTitle(),
                operation.getErrorCode(),
                new PullRequestValidation(ok, ok ? "Pull request created" : safeMessage(operation)),
                remotePush,
                record,
                timeline,
                operation.getStartedAt(),
                operation.getCompletedAt(),
                operation.getCreatedAt());
    }

    private static List<TimelineEvent> buildTimeline(
            PullRequestOperationEntity operation, RemotePushResult remotePush, PullRequestRecord record) {
        List<TimelineEvent> events = new ArrayList<>();
        events.add(new TimelineEvent("STARTED", operation.getStartedAt(), "Pull request agent started"));
        if (operation.getStatus().ordinal() >= PullRequestStatus.VALIDATING.ordinal()) {
            events.add(new TimelineEvent("VALIDATING", operation.getStartedAt(), "Validating git workspace"));
        }
        if (remotePush != null) {
            events.add(new TimelineEvent(
                    remotePush.status().name(),
                    remotePush.completedAt() == null ? remotePush.startedAt() : remotePush.completedAt(),
                    "Remote push " + remotePush.status().name().toLowerCase()));
        }
        if (record != null) {
            events.add(new TimelineEvent("PULL_REQUEST", record.createdAt(), "Pull request #" + record.pullRequestNumber()));
        }
        if (operation.getCompletedAt() != null) {
            String detail = operation.getStatus().name();
            if (operation.getErrorCode() != null) {
                detail = detail + " " + operation.getErrorCode();
            }
            events.add(new TimelineEvent("COMPLETED", operation.getCompletedAt(), detail));
        }
        return List.copyOf(events);
    }

    private static RemotePushResult toRemotePush(RemotePushEntity entity) {
        return new RemotePushResult(
                entity.getId(),
                entity.getRemoteName(),
                entity.getSourceBranch(),
                entity.getLocalCommitHash(),
                entity.getRemoteCommitHash(),
                entity.getStatus(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getStartedAt(),
                entity.getCompletedAt());
    }

    private static PullRequestRecord toRecord(PullRequestRecordEntity entity) {
        return new PullRequestRecord(
                entity.getId(),
                entity.getProvider(),
                entity.getExternalId(),
                entity.getPullRequestNumber(),
                entity.getPullRequestUrl(),
                entity.getTitle(),
                entity.getSourceBranch(),
                entity.getTargetBranch(),
                entity.getState(),
                entity.getCreatedAt());
    }

    private static String safeMessage(PullRequestOperationEntity operation) {
        if (operation.getErrorMessage() != null && !operation.getErrorMessage().isBlank()) {
            return operation.getErrorMessage();
        }
        return operation.getStatus().name();
    }
}
