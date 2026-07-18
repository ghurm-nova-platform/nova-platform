package ai.nova.platform.git.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.git.config.GitProperties;
import ai.nova.platform.web.error.ApiException;

/**
 * Allowlisted Git operations via JGit only.
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

    public Path resolveRepositoryPath(java.util.UUID organizationId, java.util.UUID projectId) {
        Path root = Path.of(properties.getWorkspaceRoot()).toAbsolutePath().normalize();
        return root.resolve(organizationId.toString()).resolve(projectId.toString()).resolve("repo");
    }

    public void ensureBaseRepository(Path repoPath, String baseRef, List<SeedFile> seedFiles) {
        branchStrategy.assertSafeBaseRef(baseRef);
        try {
            Files.createDirectories(repoPath);
            Path gitDir = repoPath.resolve(".git");
            if (!Files.exists(gitDir)) {
                if (!properties.isAllowInitRepository()) {
                    throw code("GIT_REPO_MISSING", "Git repository does not exist at " + repoPath);
                }
                try (Git git = Git.init().setDirectory(repoPath.toFile()).setInitialBranch(baseRef).call()) {
                    writeSeeds(repoPath, seedFiles);
                    git.add().addFilepattern(".").call();
                    PersonIdent author = author();
                    git.commit()
                            .setMessage("Initial repository state for Nova Git Integration")
                            .setAuthor(author)
                            .setCommitter(author)
                            .call();
                }
                return;
            }
            try (Repository repository = open(repoPath); Git git = new Git(repository)) {
                if (repository.resolve(baseRef) == null && repository.resolve(Constants.HEAD) != null) {
                    // Existing repo without named baseRef — still usable if HEAD exists.
                    return;
                }
                if (repository.resolve(Constants.HEAD) == null) {
                    writeSeeds(repoPath, seedFiles);
                    git.add().addFilepattern(".").call();
                    PersonIdent author = author();
                    git.commit()
                            .setMessage("Initial repository state for Nova Git Integration")
                            .setAuthor(author)
                            .setCommitter(author)
                            .call();
                }
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw code("GIT_REPO_INCONSISTENT", "Failed to prepare repository: " + ex.getMessage());
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

    public Instant createIsolatedBranch(Path repoPath, String branchName, String baseRef) {
        branchStrategy.assertSafeBranchName(branchName);
        branchStrategy.assertSafeBaseRef(baseRef);
        if (branchExists(repoPath, branchName)) {
            throw code("GIT_BRANCH_EXISTS", "Branch already exists: " + branchName);
        }
        try (Repository repository = open(repoPath); Git git = new Git(repository)) {
            ObjectId start = repository.resolve(baseRef);
            if (start == null) {
                start = repository.resolve(Constants.HEAD);
            }
            if (start == null) {
                throw code("GIT_INVALID_BASE", "Base ref not found: " + baseRef);
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
            // Stage deletions as well.
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

    public void verifyConsistent(Path repoPath) {
        try (Repository repository = open(repoPath); Git git = new Git(repository)) {
            Status status = git.status().call();
            if (!status.isClean()) {
                throw code(
                        "GIT_REPO_INCONSISTENT",
                        "Repository has uncommitted or conflicting changes after commit");
            }
            if (repository.resolve(Constants.HEAD) == null) {
                throw code("GIT_REPO_INCONSISTENT", "Repository HEAD is missing");
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw code("GIT_REPO_INCONSISTENT", "Repository consistency check failed: " + ex.getMessage());
        }
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

    private static void writeSeeds(Path repoPath, List<SeedFile> seedFiles) throws IOException {
        if (seedFiles == null || seedFiles.isEmpty()) {
            Path readme = repoPath.resolve("README.md");
            Files.writeString(readme, "# Nova workspace\n", StandardCharsets.UTF_8);
            return;
        }
        for (SeedFile seed : seedFiles) {
            Path target = repoPath.resolve(seed.path()).normalize();
            if (!target.startsWith(repoPath)) {
                throw code("GIT_INVALID_PATH", "Seed path escapes repository: " + seed.path());
            }
            Files.createDirectories(target.getParent() == null ? repoPath : target.getParent());
            Files.writeString(target, seed.content() == null ? "" : seed.content(), StandardCharsets.UTF_8);
        }
    }

    private static ApiException code(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public record SeedFile(String path, String content) {
    }
}
