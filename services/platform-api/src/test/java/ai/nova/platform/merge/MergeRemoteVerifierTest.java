package ai.nova.platform.merge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import ai.nova.platform.merge.provider.MergeProvider;
import ai.nova.platform.merge.provider.MergeProvider.MergeOutcome;
import ai.nova.platform.merge.provider.MergeProvider.RemotePullRequestState;
import ai.nova.platform.merge.service.MergeRemoteVerifier;
import ai.nova.platform.merge.service.MergeRemoteVerifier.VerificationRequest;
import ai.nova.platform.merge.service.MergeRemoteVerifier.VerificationResult;
import ai.nova.platform.pullrequest.provider.RepositoryRef;
import ai.nova.platform.web.error.ApiException;

@ExtendWith(MockitoExtension.class)
class MergeRemoteVerifierTest {

    @Mock
    private MergeProvider mergeProvider;

    private MergeRemoteVerifier verifier;
    private RepositoryRef repository;

    @BeforeEach
    void setUp() {
        verifier = new MergeRemoteVerifier(mergeProvider);
        repository = new RepositoryRef("github.com", "acme", "demo");
    }

    @Test
    void verifyFailsWhenRemotePrRemainsOpen() {
        when(mergeProvider.getPullRequest(eq(repository), eq(7L), anyString()))
                .thenReturn(openPr("head-abc"));

        assertThatThrownBy(() -> verifier.verify(request("head-abc", "merge-xyz"), "token"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MERGE_REMOTE_STATE_MISMATCH");
    }

    @Test
    void verifyFailsWhenLookupFails() {
        when(mergeProvider.getPullRequest(eq(repository), eq(7L), anyString()))
                .thenThrow(new ApiException(HttpStatus.BAD_GATEWAY, "MERGE_PROVIDER_FAILED", "boom"));

        assertThatThrownBy(() -> verifier.verify(request("head-abc", "merge-xyz"), "token"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MERGE_VERIFICATION_FAILED");
    }

    @Test
    void verifyFailsWhenRemoteHeadDiffers() {
        when(mergeProvider.getPullRequest(eq(repository), eq(7L), anyString()))
                .thenReturn(mergedPr("other-head", "merge-xyz"));

        assertThatThrownBy(() -> verifier.verify(request("approved-head", "merge-xyz"), "token"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MERGE_REMOTE_HEAD_MISMATCH");
    }

    @Test
    void alreadyMergedUsesMergeCommitNotHeadSha() {
        when(mergeProvider.getPullRequest(eq(repository), eq(7L), anyString()))
                .thenReturn(mergedPr("head-abc", "squash-merge-commit"));

        VerificationResult result = verifier.verify(request("head-abc", null), "token");
        assertThat(result.mergeCommitSha()).isEqualTo("squash-merge-commit");
        assertThat(result.mergeCommitSha()).isNotEqualTo(result.headSha());
        assertThat(result.headSha()).isEqualTo("head-abc");
    }

    @Test
    void missingMergeCommitIsNotFabricated() {
        when(mergeProvider.getPullRequest(eq(repository), eq(7L), anyString()))
                .thenReturn(mergedPr("head-abc", null));

        assertThatThrownBy(() -> verifier.verify(request("head-abc", null), "token"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MERGE_COMMIT_NOT_VERIFIED");
    }

    @Test
    void ambiguousOutcomeResolvedThroughRemoteRefetch() {
        when(mergeProvider.getPullRequest(eq(repository), eq(7L), anyString()))
                .thenReturn(mergedPr("head-abc", "merge-from-refetch"));

        VerificationResult result = verifier.resolveAmbiguous(request("head-abc", null), "token");
        assertThat(result.mergeCommitSha()).isEqualTo("merge-from-refetch");
        verify(mergeProvider, times(1)).getPullRequest(any(), anyLong(), anyString());
        verify(mergeProvider, never()).merge(any(), anyString());
    }

    @Test
    void ambiguousOutcomeWhenStillOpen() {
        when(mergeProvider.getPullRequest(eq(repository), eq(7L), anyString()))
                .thenReturn(openPr("head-abc"));

        assertThatThrownBy(() -> verifier.resolveAmbiguous(request("head-abc", null), "token"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MERGE_OUTCOME_AMBIGUOUS");
    }

    @Test
    void toPersistedOutcomeKeepsVerifiedMergeCommit() {
        VerificationResult verified = new VerificationResult(
                "merge-sha", "head-sha", Instant.parse("2026-07-19T12:00:00Z"), "bot", "https://pr/7", true);
        MergeOutcome provider = MergeOutcome.alreadyMerged("head-sha", null, null, null, "https://pr/7", "already");
        MergeOutcome persisted = MergeRemoteVerifier.toPersistedOutcome(verified, provider);
        assertThat(persisted.mergeCommitSha()).isEqualTo("merge-sha");
        assertThat(persisted.headSha()).isEqualTo("head-sha");
        assertThat(persisted.alreadyMerged()).isTrue();
    }

    private VerificationRequest request(String approvedHead, String mergeResponseSha) {
        return new VerificationRequest(repository, 7L, approvedHead, "acme", "demo", mergeResponseSha, false);
    }

    private static RemotePullRequestState openPr(String headSha) {
        return new RemotePullRequestState(
                7L,
                "https://github.com/acme/demo/pull/7",
                "PR",
                "feature",
                "main",
                "open",
                false,
                headSha,
                null,
                null,
                null,
                "acme",
                "demo");
    }

    private static RemotePullRequestState mergedPr(String headSha, String mergeCommitSha) {
        return new RemotePullRequestState(
                7L,
                "https://github.com/acme/demo/pull/7",
                "PR",
                "feature",
                "main",
                "closed",
                true,
                headSha,
                mergeCommitSha,
                Instant.parse("2026-07-19T12:00:00Z"),
                "octocat",
                "acme",
                "demo");
    }
}
