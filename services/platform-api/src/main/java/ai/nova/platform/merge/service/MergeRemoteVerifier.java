package ai.nova.platform.merge.service;

import java.time.Instant;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.merge.provider.MergeProvider;
import ai.nova.platform.merge.provider.MergeProvider.MergeOutcome;
import ai.nova.platform.merge.provider.MergeProvider.RemotePullRequestState;
import ai.nova.platform.pullrequest.provider.RepositoryRef;
import ai.nova.platform.web.error.ApiException;

/**
 * Post-merge remote verification. VERIFY_PASSED must only follow a successful check.
 * Never fabricates merge commit SHAs from head SHAs.
 */
@Service
public class MergeRemoteVerifier {

    public record VerificationRequest(
            RepositoryRef repository,
            long pullRequestNumber,
            String approvedHeadSha,
            String expectedOwner,
            String expectedRepoName,
            String mergeResponseSha,
            boolean alreadyMergedHint) {
    }

    public record VerificationResult(
            String mergeCommitSha,
            String headSha,
            Instant mergedAt,
            String mergedBy,
            String pullRequestUrl,
            boolean alreadyMerged) {
    }

    private final MergeProvider mergeProvider;

    public MergeRemoteVerifier(MergeProvider mergeProvider) {
        this.mergeProvider = mergeProvider;
    }

    public VerificationResult verify(VerificationRequest request, String token) {
        RemotePullRequestState remote;
        try {
            remote = mergeProvider.getPullRequest(request.repository(), request.pullRequestNumber(), token);
        } catch (ApiException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "MERGE_VERIFICATION_FAILED",
                    "Remote pull request lookup failed during verification: " + safe(ex.getMessage()));
        } catch (RuntimeException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "MERGE_VERIFICATION_FAILED",
                    "Remote pull request lookup failed during verification");
        }

        if (remote == null) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY, "MERGE_VERIFICATION_FAILED", "Remote pull request not found for verification");
        }

        if (remote.number() != request.pullRequestNumber()) {
            throw mismatch(
                    "MERGE_REMOTE_STATE_MISMATCH",
                    "Remote pull request number mismatch",
                    String.valueOf(request.pullRequestNumber()),
                    String.valueOf(remote.number()));
        }

        if (!ownersMatch(request.expectedOwner(), remote.repositoryOwner())
                || !ownersMatch(request.expectedRepoName(), remote.repositoryName())) {
            throw mismatch(
                    "MERGE_REMOTE_STATE_MISMATCH",
                    "Remote repository identity mismatch",
                    request.expectedOwner() + "/" + request.expectedRepoName(),
                    nullToEmpty(remote.repositoryOwner()) + "/" + nullToEmpty(remote.repositoryName()));
        }

        if (!remote.isMerged()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "MERGE_REMOTE_STATE_MISMATCH",
                    "Remote pull request is not merged after provider merge (state="
                            + nullToEmpty(remote.state())
                            + ")");
        }

        if (request.approvedHeadSha() != null
                && !request.approvedHeadSha().isBlank()
                && remote.headSha() != null
                && !remote.headSha().isBlank()
                && !request.approvedHeadSha().equalsIgnoreCase(remote.headSha())) {
            throw mismatch(
                    "MERGE_REMOTE_HEAD_MISMATCH",
                    "Remote PR head SHA does not match approved pre-merge head",
                    request.approvedHeadSha(),
                    remote.headSha());
        }

        String mergeCommit = firstNonBlank(remote.mergeCommitSha(), request.mergeResponseSha());
        if (mergeCommit == null || mergeCommit.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "MERGE_COMMIT_NOT_VERIFIED",
                    "Remote merge commit SHA is missing; refusing to fabricate from head SHA");
        }

        // Never treat head SHA as merge commit.
        if (remote.headSha() != null
                && !remote.headSha().isBlank()
                && mergeCommit.equalsIgnoreCase(remote.headSha())
                && (remote.mergeCommitSha() == null || remote.mergeCommitSha().isBlank())
                && (request.mergeResponseSha() == null || request.mergeResponseSha().isBlank())) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "MERGE_COMMIT_NOT_VERIFIED",
                    "Only head SHA was available; merge commit SHA was not verified");
        }

        return new VerificationResult(
                mergeCommit,
                remote.headSha(),
                remote.mergedAt(),
                remote.mergedBy(),
                remote.url(),
                request.alreadyMergedHint() || remote.isMerged());
    }

    /**
     * Resolves ambiguous provider outcomes by refetching remote state without re-issuing merge.
     */
    public VerificationResult resolveAmbiguous(VerificationRequest request, String token) {
        try {
            return verify(request, token);
        } catch (ApiException ex) {
            if ("MERGE_REMOTE_STATE_MISMATCH".equals(ex.getCode())
                    && ex.getMessage() != null
                    && ex.getMessage().toLowerCase(Locale.ROOT).contains("not merged")) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "MERGE_OUTCOME_AMBIGUOUS",
                        "Merge outcome is ambiguous: provider response unclear and remote PR is not merged");
            }
            if ("MERGE_VERIFICATION_FAILED".equals(ex.getCode())) {
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "MERGE_OUTCOME_AMBIGUOUS",
                        "Merge outcome is ambiguous: unable to confirm remote state");
            }
            throw ex;
        }
    }

    public static MergeOutcome toPersistedOutcome(VerificationResult verified, MergeOutcome providerOutcome) {
        return new MergeOutcome(
                true,
                verified.alreadyMerged() || (providerOutcome != null && providerOutcome.alreadyMerged()),
                false,
                verified.headSha(),
                verified.mergeCommitSha(),
                verified.mergedAt() != null
                        ? verified.mergedAt()
                        : (providerOutcome != null ? providerOutcome.mergedAt() : null),
                verified.mergedBy() != null
                        ? verified.mergedBy()
                        : (providerOutcome != null ? providerOutcome.mergedBy() : null),
                verified.pullRequestUrl() != null
                        ? verified.pullRequestUrl()
                        : (providerOutcome != null ? providerOutcome.pullRequestUrl() : null),
                providerOutcome != null && providerOutcome.providerMessage() != null
                        ? providerOutcome.providerMessage()
                        : "Merge verified remotely");
    }

    private static boolean ownersMatch(String expected, String actual) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        return actual != null && expected.equalsIgnoreCase(actual.trim());
    }

    private static String firstNonBlank(String primary, String secondary) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (secondary != null && !secondary.isBlank()) {
            return secondary.trim();
        }
        return null;
    }

    private static ApiException mismatch(String code, String message, String expected, String actual) {
        return new ApiException(
                HttpStatus.CONFLICT,
                code,
                message + " (expected=" + nullToEmpty(expected) + ", actual=" + nullToEmpty(actual) + ")");
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String safe(String message) {
        return message == null || message.isBlank() ? "unknown" : message;
    }
}
