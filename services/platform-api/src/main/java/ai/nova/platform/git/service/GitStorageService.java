package ai.nova.platform.git.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.git.dto.GitDtos.GitApplyResult;
import ai.nova.platform.git.dto.GitDtos.GitBranch;
import ai.nova.platform.git.dto.GitDtos.GitCommit;
import ai.nova.platform.git.dto.GitDtos.GitOperation;
import ai.nova.platform.git.dto.GitDtos.GitValidation;
import ai.nova.platform.git.dto.GitDtos.TimelineEvent;
import ai.nova.platform.git.entity.GitBranchEntity;
import ai.nova.platform.git.entity.GitCommitEntity;
import ai.nova.platform.git.entity.GitOperationEntity;
import ai.nova.platform.git.entity.GitStatus;
import ai.nova.platform.git.repository.GitBranchRepository;
import ai.nova.platform.git.repository.GitCommitRepository;
import ai.nova.platform.git.repository.GitOperationRepository;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.web.error.ApiException;

@Service
public class GitStorageService {

    private final GitOperationRepository operationRepository;
    private final GitBranchRepository branchRepository;
    private final GitCommitRepository commitRepository;

    public GitStorageService(
            GitOperationRepository operationRepository,
            GitBranchRepository branchRepository,
            GitCommitRepository commitRepository) {
        this.operationRepository = operationRepository;
        this.branchRepository = branchRepository;
        this.commitRepository = commitRepository;
    }

    @Transactional
    public GitOperation startPending(
            UUID operationId,
            AgentOrchestrationTask task,
            UUID patchResultId,
            String branchName,
            String patchHash,
            String repositoryPath,
            String baseRef,
            Instant startedAt,
            List<TimelineEvent> timeline) {
        Instant createdAt = Instant.now();
        GitOperationEntity operation = new GitOperationEntity(
                operationId,
                task.getOrganizationId(),
                task.getProjectId(),
                task.getRunId(),
                task.getId(),
                patchResultId,
                GitStatus.PENDING,
                branchName,
                null,
                patchHash,
                repositoryPath,
                baseRef,
                null,
                "Git operation workspace allocated",
                startedAt,
                null,
                createdAt);
        operationRepository.save(operation);
        return toOperation(operation, List.of(), List.of(), timeline == null ? List.of() : timeline);
    }

    @Transactional
    public GitOperation markSucceeded(
            UUID operationId,
            String commitHash,
            String commitMessage,
            String authorName,
            String authorEmail,
            Instant branchCreatedAt,
            Instant completedAt,
            List<TimelineEvent> timeline) {
        GitOperationEntity operation = operationRepository
                .findById(operationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "GIT_NOT_FOUND", "Git operation not found: " + operationId));
        if (operation.getStatus() != GitStatus.PENDING) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "GIT_REPO_INCONSISTENT", "Operation is not PENDING: " + operationId);
        }
        operation.markSucceeded(commitHash, "Git apply and commit succeeded", completedAt);
        operationRepository.save(operation);

        GitBranchEntity branch = new GitBranchEntity(
                UUID.randomUUID(),
                operationId,
                operation.getOrganizationId(),
                operation.getBranchName(),
                operation.getBaseRef(),
                branchCreatedAt == null ? completedAt : branchCreatedAt);
        branchRepository.save(branch);

        GitCommitEntity commit = new GitCommitEntity(
                UUID.randomUUID(),
                operationId,
                operation.getOrganizationId(),
                commitHash,
                commitMessage,
                authorName,
                authorEmail,
                completedAt);
        commitRepository.save(commit);

        return toOperation(
                operation,
                List.of(toBranch(branch)),
                List.of(toCommit(commit)),
                timeline == null ? List.of() : timeline);
    }

    @Transactional
    public GitOperation markFailed(UUID operationId, String errorCode, String message, Instant completedAt) {
        GitOperationEntity operation = operationRepository
                .findById(operationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "GIT_NOT_FOUND", "Git operation not found: " + operationId));
        if (operation.getStatus() == GitStatus.SUCCEEDED) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "GIT_REPO_INCONSISTENT", "Cannot fail a SUCCEEDED operation");
        }
        // Never persist branch/commit success rows for failures.
        operation.markFailed(
                errorCode == null ? "GIT_REPO_INCONSISTENT" : errorCode,
                message == null ? "Git operation failed" : message,
                completedAt);
        operationRepository.save(operation);
        return toOperation(operation, List.of(), List.of(), buildTimeline(operation, List.of(), List.of()));
    }

    @Transactional(readOnly = true)
    public GitOperation findLatest(UUID taskId, UUID organizationId) {
        return operationRepository
                .findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(taskId, organizationId)
                .map(operation -> {
                    List<GitBranch> branches = branchRepository
                            .findByGitOperationIdOrderByCreatedAtAsc(operation.getId())
                            .stream()
                            .map(GitStorageService::toBranch)
                            .toList();
                    List<GitCommit> commits = commitRepository
                            .findByGitOperationIdOrderByCreatedAtAsc(operation.getId())
                            .stream()
                            .map(GitStorageService::toCommit)
                            .toList();
                    List<TimelineEvent> timeline = buildTimeline(operation, branches, commits);
                    return toOperation(operation, branches, commits, timeline);
                })
                .orElse(null);
    }

    public boolean branchRecorded(UUID organizationId, UUID projectId, String branchName) {
        return operationRepository.existsByOrganizationIdAndProjectIdAndBranchName(
                organizationId, projectId, branchName);
    }

    private static GitOperation toOperation(
            GitOperationEntity operation,
            List<GitBranch> branches,
            List<GitCommit> commits,
            List<TimelineEvent> timeline) {
        boolean ok = operation.getStatus() == GitStatus.SUCCEEDED;
        return new GitOperation(
                operation.getId(),
                operation.getTaskId(),
                operation.getRunId(),
                operation.getProjectId(),
                operation.getPatchResultId(),
                operation.getStatus(),
                operation.getBranchName(),
                operation.getCommitHash(),
                operation.getPatchHash(),
                operation.getRepositoryPath(),
                operation.getBaseRef(),
                operation.getErrorCode(),
                new GitValidation(ok, operation.getValidationMessage() == null ? "" : operation.getValidationMessage()),
                new GitApplyResult(ok, ok ? "Patch applied" : "Patch not applied"),
                branches,
                commits,
                timeline,
                operation.getStartedAt(),
                operation.getCompletedAt(),
                operation.getCreatedAt());
    }

    private static List<TimelineEvent> buildTimeline(
            GitOperationEntity operation, List<GitBranch> branches, List<GitCommit> commits) {
        List<TimelineEvent> events = new ArrayList<>();
        events.add(new TimelineEvent("STARTED", operation.getStartedAt(), "Git integration started"));
        if (!branches.isEmpty()) {
            events.add(new TimelineEvent(
                    "BRANCH_CREATED", branches.get(0).createdAt(), "Created " + branches.get(0).branchName()));
        }
        if (!commits.isEmpty()) {
            events.add(new TimelineEvent(
                    "COMMITTED", commits.get(0).createdAt(), "Commit " + commits.get(0).commitHash()));
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

    private static GitBranch toBranch(GitBranchEntity entity) {
        return new GitBranch(entity.getId(), entity.getBranchName(), entity.getBaseRef(), entity.getCreatedAt());
    }

    private static GitCommit toCommit(GitCommitEntity entity) {
        return new GitCommit(
                entity.getId(),
                entity.getCommitHash(),
                entity.getMessage(),
                entity.getAuthorName(),
                entity.getAuthorEmail(),
                entity.getCreatedAt());
    }
}
