package ai.nova.platform.pullrequest.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.git.config.GitProperties;
import ai.nova.platform.git.dto.GitDtos.GitOperation;
import ai.nova.platform.git.service.GitValidator;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.web.error.ApiException;

@Service
public class PullRequestGitValidator {

    private final GitProperties gitProperties;
    private final GitValidator gitValidator;

    public PullRequestGitValidator(GitProperties gitProperties, GitValidator gitValidator) {
        this.gitProperties = gitProperties;
        this.gitValidator = gitValidator;
    }

    public void validateWorkspace(GitOperation gitOperation, AgentOrchestrationTask task, PatchResult patch) {
        if (gitOperation == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "PR_GIT_OPERATION_NOT_FOUND", "Git operation is required");
        }
        if (gitOperation.status() != ai.nova.platform.git.entity.GitStatus.SUCCEEDED) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PR_GIT_OPERATION_NOT_SUCCEEDED",
                    "Git operation must be SUCCEEDED");
        }
        if (gitOperation.commitHash() == null || gitOperation.commitHash().isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "PR_GIT_OPERATION_NOT_SUCCEEDED", "Git commit hash is missing");
        }
        if (gitOperation.branchName() == null || gitOperation.branchName().isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "PR_GIT_OPERATION_NOT_SUCCEEDED", "Git branch name is missing");
        }
        if (gitOperation.patchHash() == null || gitOperation.patchHash().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PR_PATCH_HASH_MISMATCH", "Git patch hash is missing");
        }

        String expectedBranch = "ai/task-" + task.getId();
        if (!expectedBranch.equals(gitOperation.branchName())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PR_LOCAL_BRANCH_MISMATCH",
                    "Git branch must follow ai/task-{taskId}");
        }

        if (patch != null && patch.patch() != null) {
            String computed = gitValidator.sha256(patch.patch());
            if (!computed.equalsIgnoreCase(gitOperation.patchHash())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "PR_PATCH_HASH_MISMATCH", "Patch hash does not match git operation");
            }
        }

        Path workspaceRoot = Path.of(gitProperties.getWorkspaceRoot()).toAbsolutePath().normalize();
        Path repoPath = Path.of(gitOperation.repositoryPath()).toAbsolutePath().normalize();
        if (!repoPath.startsWith(workspaceRoot)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "PR_GIT_WORKSPACE_MISSING", "Repository path is outside workspace root");
        }

        Path expectedProjectRoot = workspaceRoot
                .resolve(task.getOrganizationId().toString())
                .resolve(task.getProjectId().toString());
        if (!repoPath.startsWith(expectedProjectRoot)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PR_GIT_WORKSPACE_MISSING",
                    "Repository path does not belong to task project");
        }

        if (!Files.isDirectory(repoPath.resolve(".git"))) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "PR_GIT_WORKSPACE_MISSING", "Git workspace repository is missing");
        }

        try (Repository repository = open(repoPath); Git git = new Git(repository)) {
            if (repository.isBare()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "PR_GIT_WORKSPACE_MISSING", "Bare repositories are not supported");
            }
            Status status = git.status().call();
            if (!status.isClean()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "PR_GIT_WORKSPACE_DIRTY", "Git workspace working tree is not clean");
            }
            Ref head = repository.exactRef(Constants.HEAD);
            if (head == null || head.getObjectId() == null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "PR_LOCAL_COMMIT_MISMATCH", "Git workspace HEAD is missing");
            }
            String headHash = ObjectId.toString(head.getObjectId());
            if (!headHash.equalsIgnoreCase(gitOperation.commitHash())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "PR_LOCAL_COMMIT_MISMATCH", "Git workspace HEAD does not match commit");
            }
            String branch = repository.getBranch();
            if (branch == null || !branch.equals(gitOperation.branchName())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "PR_LOCAL_BRANCH_MISMATCH",
                        "Current branch does not match git operation branch");
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PR_GIT_WORKSPACE_MISSING",
                    "Failed to validate git workspace: " + safeMessage(ex));
        }
    }

    private static Repository open(Path repoPath) throws Exception {
        return new FileRepositoryBuilder()
                .setGitDir(repoPath.resolve(".git").toFile())
                .readEnvironment()
                .findGitDir()
                .build();
    }

    private static String safeMessage(Throwable ex) {
        String message = ex.getMessage();
        return message == null ? ex.getClass().getSimpleName() : message;
    }
}
