package ai.nova.platform.git.service;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.web.error.ApiException;

/**
 * Deterministic branch naming for Git Integration Agent.
 * Never targets protected branches (main/master/develop).
 */
@Component
public class GitBranchStrategy {

    private static final Set<String> PROTECTED = Set.of("main", "master", "develop", "development");

    public String branchNameForTask(UUID taskId) {
        if (taskId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "GIT_INVALID_TASK", "Task id is required");
        }
        return "ai/task-" + taskId;
    }

    public String commitMessageForTask(UUID taskId) {
        if (taskId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "GIT_INVALID_TASK", "Task id is required");
        }
        return "AI: Apply approved patch for Task #" + taskId;
    }

    public void assertSafeBranchName(String branchName) {
        if (branchName == null || branchName.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "GIT_INVALID_BRANCH", "Branch name is required");
        }
        String normalized = branchName.trim();
        if (!normalized.startsWith("ai/task-")) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "GIT_INVALID_BRANCH",
                    "Branch must follow ai/task-{taskId} naming");
        }
        if (normalized.contains("..") || normalized.contains("\\") || normalized.contains(" ")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "GIT_INVALID_BRANCH", "Invalid branch name");
        }
        String taskPart = normalized.substring("ai/task-".length());
        try {
            UUID.fromString(taskPart);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "GIT_INVALID_BRANCH", "Branch task id must be a UUID");
        }
        for (String segment : normalized.toLowerCase(Locale.ROOT).split("/")) {
            if (PROTECTED.contains(segment)) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "GIT_PROTECTED_BRANCH", "Refusing to use protected branch name");
            }
        }
    }

    public void assertSafeBaseRef(String baseRef) {
        if (baseRef == null || baseRef.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "GIT_INVALID_BASE", "Base ref is required");
        }
        String normalized = baseRef.trim().toLowerCase(Locale.ROOT);
        if (!(PROTECTED.contains(normalized) || normalized.startsWith("origin/"))) {
            // Allow main/master/develop as read-only base, and other refs that are not write targets.
            if (normalized.contains("..")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "GIT_INVALID_BASE", "Invalid base ref");
            }
        }
        if (normalized.startsWith("ai/task-")) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "GIT_INVALID_BASE", "Cannot use AI working branch as base");
        }
    }
}
