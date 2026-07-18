package ai.nova.platform.git.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import ai.nova.platform.git.entity.GitStatus;

public final class GitDtos {

    private GitDtos() {
    }

    public record GitRunRequest(@NotNull UUID taskId) {
    }

    public record GitBranch(UUID id, String branchName, String baseRef, Instant createdAt) {
    }

    public record GitCommit(
            UUID id, String commitHash, String message, String authorName, String authorEmail, Instant createdAt) {
    }

    public record GitValidation(boolean valid, String message) {
    }

    public record GitApplyResult(boolean applied, String details) {
    }

    public record TimelineEvent(String phase, Instant at, String detail) {
    }

    public record GitOperation(
            UUID id,
            UUID taskId,
            UUID runId,
            UUID projectId,
            UUID patchResultId,
            GitStatus status,
            String branchName,
            String commitHash,
            String patchHash,
            String repositoryPath,
            String baseRef,
            GitValidation validation,
            GitApplyResult applyResult,
            List<GitBranch> branches,
            List<GitCommit> commits,
            List<TimelineEvent> timeline,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt) {
    }
}
