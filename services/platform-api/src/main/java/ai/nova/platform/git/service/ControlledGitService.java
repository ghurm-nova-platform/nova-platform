package ai.nova.platform.git.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.git.config.GitProperties;
import ai.nova.platform.web.error.ApiException;

/**
 * Allowlisted Git operations via JGit only.
 * Mutates only isolated per-operation workspaces cloned from an immutable project source.
 * Never merges, pushes, force-pushes, deletes branches, or runs shell commands.
 */
@Service
public class ControlledGitService {

    private final GitProperties properties;
    private final GitBranchStrategy branchStrategy;

    public ControlledGitService(GitProperties properties, GitBranchStrategy branchStrategy) {
        this.properties = properties;
        this.branchStrategy = branchStrategy;
    }

    public Path resolveSourceRepositoryPath(UUID organizationId, UUID projectId) {
        return projectRoot(organizationId, projectId).resolve("source");
    }

    public Path resolveOperationRepositoryPath(UUID organizationId, UUID projectId, UUID operationId) {
        return projectRoot(organizationId, projectId)
                .resolve("operations")
                .resolve(operationId.toString())
                .resolve("repo");
    }

    private Path projectRoot(UUID organizationId, UUID projectId) {
        Path root = Path.of(properties.getWorkspaceRoot()).toAbsolutePath().normalize();
        return root.resolve(organizationId.toString()).resolve(projectId.toString());
    }

    /**
     * Validates that the immutable project source repository exists, is not bare, and has baseRef.
     * Does not initialize or mutate the source.
     */
    public ObjectId requireSourceRepository(Path sourcePath, String baseRef) {
        branchStrategy.assertSafeBaseRef(baseRef);
        Path gitDir = sourcePath.resolve(".git");
        if (!Files.isDirectory(gitDir)) {
            throw code("GIT_REPO_MISSING", "Source git repository does not exist at " + sourcePath);
        }
        try (Repository repository = open(sourcePath)) {
            if (repository.isBare()) {
                throw code("GIT_REPO_INCONSISTENT", "Bare source repositories are not supported");
            }
            ObjectId base = resolveStrictBaseRef(repository, baseRef);
            return base;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw code("GIT_REPO_INCONSISTENT", "Failed to open source repository: " + ex.getMessage());
        }
    }

    /**
     * Clones the project source into a dedicated operation workspace via JGit.
     * The source working tree is never checked out for task branches.
     */
    public void cloneSourceToOperationWorkspace(Path sourcePath, Path operationRepoPath) {
        if (Files.exists(operationRepoPath)) {
            throw code("GIT_REPO_INCONSISTENT", "Operation workspace already exists: " + operationRepoPath);
        }
        try {
            Files.createDirectories(operationRepoPath.getParent());
            try (Git ignored = Git.cloneRepository()
                    .setURI(sourcePath.toUri().toString())
                    .setDirectory(operationRepoPath.toFile())
                    .setCloneAllBranches(true)
                    .setBare(false)
                    .call()) {
                // clone complete
            }
            try (Repository repository = open(operationRepoPath); Git git = new Git(repository)) {
                Status status = git.status().call();
                if (!status.isClean()) {
                    throw code("GIT_REPO_INCONSISTENT", "Isolated workspace is not clean after clone");
                }
                if (repository.isBare()) {
                    throw code("GIT_REPO_INCONSISTENT", "Operation workspace must not be bare");
                }
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw code("GIT_REPO_INCONSISTENT", "Failed to clone source into operation workspace: " + ex.getMessage());
        }
    }

    public boolean branchExists(Path repoPath, String branchName) {
        branchStrategy.assertSafeBranchName(branchName);
        try (Repository repository = open(repoPath)) {
            Ref ref = repository.findRef(Constants.R_HEADS + branchName);
            return ref != null;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw code("GIT_REPO_INCONSISTENT", "Failed to inspect branches: " + ex.getMessage());
        }
    }

    /**
     * Creates ai/task-{taskId} from the resolved baseRef inside the isolated operation workspace only.
     * Never falls back to HEAD when baseRef is missing.
     */
    public Instant createIsolatedBranch(Path operationRepoPath, String branchName, String baseRef) {
        branchStrategy.assertSafeBranchName(branchName);
        branchStrategy.assertSafeBaseRef(baseRef);
        if (branchExists(operationRepoPath, branchName)) {
            throw code("GIT_BRANCH_EXISTS", "Branch already exists: " + branchName);
        }
        try (Repository repository = open(operationRepoPath); Git git = new Git(repository)) {
            ObjectId start = resolveStrictBaseRef(repository, baseRef);
            Status before = git.status().call();
            if (!before.isClean()) {
                throw code("GIT_REPO_INCONSISTENT", "Isolated workspace must be clean before branch creation");
            }
            git.checkout()
                    .setCreateBranch(true)
                    .setName(branchName)
                    .setStartPoint(start.getName())
                    .call();
            return Instant.now();
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw code("GIT_BRANCH_FAILED", "Failed to create branch: " + ex.getMessage());
        }
    }

    public void applyPatch(Path repoPath, String patchContent) {
        if (patchContent == null || patchContent.isBlank()) {
            throw code("GIT_APPLY_FAILED", "Patch content is empty");
        }
        try (Repository repository = open(repoPath); Git git = new Git(repository)) {
            try (ByteArrayInputStream in =
                    new ByteArrayInputStream(patchContent.getBytes(StandardCharsets.UTF_8))) {
                git.apply().setPatch(in).call();
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw code("GIT_APPLY_FAILED", "Git apply failed: " + ex.getMessage());
        }
    }

    public String commitAll(Path repoPath, String message) {
        try (Repository repository = open(repoPath); Git git = new Git(repository)) {
            Status status = git.status().call();
            if (status.isClean()) {
                throw code("GIT_COMMIT_FAILED", "No changes to commit after applying patch");
            }
            git.add().addFilepattern(".").call();
            for (String missing : status.getMissing()) {
                git.rm().addFilepattern(missing).call();
            }
            PersonIdent author = author();
            RevCommit commit = git.commit()
                    .setMessage(message)
                    .setAuthor(author)
                    .setCommitter(author)
                    .call();
            return ObjectId.toString(commit.getId());
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw code("GIT_COMMIT_FAILED", "Commit failed: " + ex.getMessage());
        }
    }

    /**
     * Post-commit validation: clean tree, HEAD == commit, branch == expected, parent based on baseRef.
     */
    public void verifySuccessfulCommit(
            Path operationRepoPath, String branchName, String commitHash, String baseRef) {
        try (Repository repository = open(operationRepoPath); Git git = new Git(repository)) {
            Status status = git.status().call();
            if (!status.isClean()) {
                throw code(
                        "GIT_REPO_INCONSISTENT",
                        "Repository has uncommitted or conflicting changes after commit");
            }
            ObjectId head = repository.resolve(Constants.HEAD);
            if (head == null) {
                throw code("GIT_REPO_INCONSISTENT", "Repository HEAD is missing");
            }
            if (!ObjectId.toString(head).equals(commitHash)) {
                throw code("GIT_REPO_INCONSISTENT", "HEAD does not equal created commit");
            }
            String currentBranch = repository.getBranch();
            if (!branchName.equals(currentBranch)) {
                throw code(
                        "GIT_REPO_INCONSISTENT",
                        "Current branch is " + currentBranch + ", expected " + branchName);
            }
            ObjectId expectedBase = resolveStrictBaseRef(repository, baseRef);
            try (RevWalk walk = new RevWalk(repository)) {
                RevCommit commit = walk.parseCommit(head);
                if (commit.getParentCount() < 1) {
                    throw code("GIT_REPO_INCONSISTENT", "Commit has no parent; expected baseRef parent");
                }
                ObjectId parent = commit.getParent(0).getId();
                if (!parent.equals(expectedBase)) {
                    throw code(
                            "GIT_REPO_INCONSISTENT",
                            "Commit parent does not match resolved baseRef commit");
                }
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw code("GIT_REPO_INCONSISTENT", "Repository consistency check failed: " + ex.getMessage());
        }
    }

    /** Strict baseRef resolution — never falls back to HEAD. */
    public ObjectId resolveStrictBaseRef(Repository repository, String baseRef) throws IOException {
        ObjectId resolved = repository.resolve(baseRef);
        if (resolved == null) {
            resolved = repository.resolve(Constants.R_HEADS + baseRef);
        }
        if (resolved == null) {
            throw code("GIT_INVALID_BASE", "Base ref not found: " + baseRef);
        }
        return resolved;
    }

    private Repository open(Path repoPath) throws IOException {
        Path gitDir = repoPath.resolve(".git");
        if (!Files.exists(gitDir)) {
            throw code("GIT_REPO_MISSING", "Git repository does not exist at " + repoPath);
        }
        return new FileRepositoryBuilder().setGitDir(gitDir.toFile()).setMustExist(true).build();
    }

    private PersonIdent author() {
        return new PersonIdent(properties.getAuthorName(), properties.getAuthorEmail());
    }

    private static ApiException code(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }
}
