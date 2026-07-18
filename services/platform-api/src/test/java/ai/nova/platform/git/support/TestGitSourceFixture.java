package ai.nova.platform.git.support;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;

import ai.nova.platform.git.service.ControlledGitService;

/**
 * Test-only helper to create an immutable project source repository.
 * Production Git Integration never initializes synthetic repositories.
 */
public final class TestGitSourceFixture {

    private TestGitSourceFixture() {
    }

    public record SeedFile(String path, String content) {
    }

    public static Path ensureSourceRepository(
            ControlledGitService gitService,
            UUID organizationId,
            UUID projectId,
            String baseRef,
            List<SeedFile> seedFiles)
            throws Exception {
        Path sourcePath = gitService.resolveSourceRepositoryPath(organizationId, projectId);
        Path gitDir = sourcePath.resolve(".git");
        if (Files.isDirectory(gitDir)) {
            return sourcePath;
        }
        Files.createDirectories(sourcePath);
        try (Git git = Git.init().setDirectory(sourcePath.toFile()).setInitialBranch(baseRef).call()) {
            if (seedFiles == null || seedFiles.isEmpty()) {
                Files.writeString(
                        sourcePath.resolve("README.md"), "# Nova test source\n", StandardCharsets.UTF_8);
            } else {
                for (SeedFile seed : seedFiles) {
                    Path target = sourcePath.resolve(seed.path()).normalize();
                    if (!target.startsWith(sourcePath)) {
                        throw new IllegalArgumentException("Seed path escapes repository: " + seed.path());
                    }
                    Path parent = target.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.writeString(
                            target, seed.content() == null ? "" : seed.content(), StandardCharsets.UTF_8);
                }
            }
            git.add().addFilepattern(".").call();
            PersonIdent author = new PersonIdent("Nova Test", "test@nova.local");
            git.commit()
                    .setMessage("Test source repository snapshot")
                    .setAuthor(author)
                    .setCommitter(author)
                    .call();
        }
        return sourcePath;
    }

    public static List<SeedFile> loginServiceSeeds() {
        return List.of(new SeedFile("src/LoginService.java", "class LoginService {}\n"));
    }
}
