package ai.nova.platform.pullrequest.service;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.pullrequest.config.PullRequestProperties;
import ai.nova.platform.web.error.ApiException;

/**
 * Allowlisted remote Git operations via JGit only.
 * Pushes a single branch with an explicit refspec. Never force-pushes or pushes tags.
 */
@Service
public class PullRequestRemoteGitService {

    private final PullRequestProperties properties;

    public PullRequestRemoteGitService(PullRequestProperties properties) {
        this.properties = properties;
    }

    public String pushExactBranch(
            Path localRepo,
            String remoteUrl,
            String branchName,
            String expectedCommit,
            String tokenOrNull) {
        if (properties.isAllowForcePush()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "PR_PUSH_FAILED", "Force push is disabled by platform policy");
        }
        if (branchName == null || branchName.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PR_INVALID_REQUEST", "Branch name is required");
        }
        if (expectedCommit == null || expectedCommit.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PR_INVALID_REQUEST", "Expected commit hash is required");
        }
        String refSpec = "refs/heads/" + branchName + ":refs/heads/" + branchName;
        try (Git git = Git.open(localRepo.toFile())) {
            // Explicit single-branch refspec only. Never setPushAll, never setPushTags, never force.
            var pushCommand = git.push()
                    .setRemote(remoteUrl)
                    .setForce(false)
                    .setRefSpecs(new RefSpec(refSpec));
            if (tokenOrNull != null && !tokenOrNull.isBlank() && !isFileRemote(remoteUrl)) {
                pushCommand.setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider("x-access-token", tokenOrNull.trim()));
            }
            pushCommand.call();

            String remoteCommit = resolveRemoteHeadCommit(remoteUrl, branchName, tokenOrNull);
            if (!normalizeHash(remoteCommit).equals(normalizeHash(expectedCommit))) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "PR_REMOTE_COMMIT_MISMATCH",
                        "Remote branch commit does not match expected commit");
            }
            return remoteCommit;
        } catch (ApiException ex) {
            throw ex;
        } catch (TransportException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "PR_PUSH_FAILED", "Remote push failed: " + safeMessage(ex));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "PR_PUSH_FAILED", "Remote push failed: " + safeMessage(ex));
        }
    }

    public Optional<String> findRemoteHeadCommit(String remoteUrl, String branchName, String tokenOrNull) {
        try {
            Collection<Ref> refs = lsRemoteHeads(remoteUrl, tokenOrNull);
            String wanted = Constants.R_HEADS + branchName;
            for (Ref ref : refs) {
                if (wanted.equals(ref.getName()) && ref.getObjectId() != null) {
                    return Optional.of(ref.getObjectId().getName());
                }
            }
            return Optional.empty();
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "PR_REMOTE_COMMIT_MISMATCH",
                    "Failed to inspect remote branch: " + safeMessage(ex));
        }
    }

    public String resolveRemoteHeadCommit(String remoteUrl, String branchName, String tokenOrNull) {
        return findRemoteHeadCommit(remoteUrl, branchName, tokenOrNull)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT, "PR_REMOTE_COMMIT_MISMATCH", "Remote branch not found after push"));
    }

    private Collection<Ref> lsRemoteHeads(String remoteUrl, String tokenOrNull) throws Exception {
        var lsRemote = Git.lsRemoteRepository().setRemote(remoteUrl).setHeads(true).setTags(false);
        if (tokenOrNull != null && !tokenOrNull.isBlank() && !isFileRemote(remoteUrl)) {
            lsRemote.setCredentialsProvider(
                    new UsernamePasswordCredentialsProvider("x-access-token", tokenOrNull.trim()));
        }
        return lsRemote.call();
    }

    public boolean isFileRemote(String remoteUrl) {
        if (remoteUrl == null) {
            return false;
        }
        String normalized = remoteUrl.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("file:") || normalized.startsWith("file://");
    }

    private static String normalizeHash(String hash) {
        return hash == null ? "" : hash.trim().toLowerCase(Locale.ROOT);
    }

    private static String safeMessage(Throwable ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message.toLowerCase(Locale.ROOT).contains("token") ? "remote git operation failed" : message;
    }
}
