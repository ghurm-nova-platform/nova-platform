package ai.nova.platform.merge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.merge.config.MergeProperties;
import ai.nova.platform.merge.entity.MergeMethod;
import ai.nova.platform.merge.provider.GitHubMergeProvider;
import ai.nova.platform.merge.provider.MergeProvider.MergeOutcome;
import ai.nova.platform.merge.provider.MergeProvider.MergeRequest;
import ai.nova.platform.merge.provider.MergeProvider.RemotePullRequestState;
import ai.nova.platform.pullrequest.config.PullRequestProperties;
import ai.nova.platform.pullrequest.provider.RepositoryRef;
import ai.nova.platform.web.error.ApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class MergeProviderTest {

    private MockWebServer server;
    private GitHubMergeProvider provider;
    private RepositoryRef repositoryRef;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        MergeProperties mergeProperties = new MergeProperties();
        mergeProperties.setGithubApiBaseUrl(server.url("/").toString().replaceAll("/$", ""));
        mergeProperties.setGithubToken("test-token-not-real");
        PullRequestProperties pullRequestProperties = new PullRequestProperties();
        provider = new GitHubMergeProvider(mergeProperties, pullRequestProperties, new ObjectMapper());
        repositoryRef = new RepositoryRef("github.com", "ghurm-nova-platform", "nova-demo");
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void mergePullRequestReturnsDistinctMergeCommit() throws Exception {
        server.enqueue(openPrBody("abc123"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"sha":"merged123","merged":true,"message":"Pull Request successfully merged"}
                        """));

        MergeOutcome outcome = provider.merge(
                new MergeRequest(
                        repositoryRef,
                        7L,
                        "https://github.com/ghurm-nova-platform/nova-demo/pull/7",
                        "abc123",
                        "main",
                        MergeMethod.SQUASH),
                "test-token-not-real");
        assertThat(outcome.succeeded()).isTrue();
        assertThat(outcome.alreadyMerged()).isFalse();
        assertThat(outcome.headSha()).isEqualTo("abc123");
        assertThat(outcome.mergeCommitSha()).isEqualTo("merged123");
        assertThat(outcome.mergeCommitSha()).isNotEqualTo(outcome.headSha());

        RecordedRequest getRequest = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(getRequest.getPath()).isEqualTo("/repos/ghurm-nova-platform/nova-demo/pulls/7");
        RecordedRequest mergeRequest = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(mergeRequest.getPath()).isEqualTo("/repos/ghurm-nova-platform/nova-demo/pulls/7/merge");
        assertThat(mergeRequest.getMethod()).isEqualTo("PUT");
        assertThat(mergeRequest.getBody().readUtf8()).contains("\"merge_method\":\"squash\"");
    }

    @Test
    void alreadyMergedDoesNotUseHeadAsMergeCommit() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "number": 7,
                          "html_url": "https://github.com/ghurm-nova-platform/nova-demo/pull/7",
                          "title": "PR",
                          "state": "closed",
                          "merged": true,
                          "merge_commit_sha": "squash-merge-sha",
                          "merged_at": "2026-07-19T12:00:00Z",
                          "merged_by": {"login":"octocat"},
                          "head": {"ref":"feature","sha":"abc123"},
                          "base": {"ref":"main","repo":{"full_name":"ghurm-nova-platform/nova-demo"}}
                        }
                        """));

        MergeOutcome outcome = provider.merge(
                new MergeRequest(
                        repositoryRef,
                        7L,
                        "https://github.com/ghurm-nova-platform/nova-demo/pull/7",
                        "abc123",
                        "main",
                        MergeMethod.SQUASH),
                "test-token-not-real");
        assertThat(outcome.succeeded()).isTrue();
        assertThat(outcome.alreadyMerged()).isTrue();
        assertThat(outcome.headSha()).isEqualTo("abc123");
        assertThat(outcome.mergeCommitSha()).isEqualTo("squash-merge-sha");
        assertThat(outcome.mergeCommitSha()).isNotEqualTo(outcome.headSha());
        assertThat(outcome.mergedBy()).isEqualTo("octocat");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void alreadyMergedWithoutMergeCommitLeavesNull() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "number": 7,
                          "html_url": "https://github.com/ghurm-nova-platform/nova-demo/pull/7",
                          "title": "PR",
                          "state": "closed",
                          "merged": true,
                          "head": {"ref":"feature","sha":"abc123"},
                          "base": {"ref":"main"}
                        }
                        """));

        MergeOutcome outcome = provider.merge(
                new MergeRequest(repositoryRef, 7L, "url", "abc123", "main", MergeMethod.SQUASH),
                "test-token-not-real");
        assertThat(outcome.alreadyMerged()).isTrue();
        assertThat(outcome.mergeCommitSha()).isNull();
        assertThat(outcome.headSha()).isEqualTo("abc123");
    }

    @Test
    void timeoutReturnsAmbiguousWithoutFabricatingCommit() throws Exception {
        server.enqueue(openPrBody("abc123"));
        server.enqueue(new MockResponse().setResponseCode(504).setBody("gateway timeout"));

        MergeOutcome outcome = provider.merge(
                new MergeRequest(repositoryRef, 7L, "url", "abc123", "main", MergeMethod.SQUASH),
                "test-token-not-real");
        assertThat(outcome.ambiguous()).isTrue();
        assertThat(outcome.succeeded()).isFalse();
        assertThat(outcome.mergeCommitSha()).isNull();
    }

    @Test
    void getPullRequestParsesMergeMetadata() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "number": 7,
                          "html_url": "https://github.com/ghurm-nova-platform/nova-demo/pull/7",
                          "title": "PR",
                          "state": "closed",
                          "merged": true,
                          "merge_commit_sha": "mc1",
                          "merged_at": "2026-07-19T12:00:00Z",
                          "merged_by": {"login":"bot"},
                          "head": {"ref":"feature","sha":"h1"},
                          "base": {"ref":"main","repo":{"full_name":"ghurm-nova-platform/nova-demo"}}
                        }
                        """));
        RemotePullRequestState state = provider.getPullRequest(repositoryRef, 7L, "test-token-not-real");
        assertThat(state.isMerged()).isTrue();
        assertThat(state.mergeCommitSha()).isEqualTo("mc1");
        assertThat(state.headSha()).isEqualTo("h1");
        assertThat(state.mergedBy()).isEqualTo("bot");
        assertThat(state.repositoryOwner()).isEqualTo("ghurm-nova-platform");
    }

    @Test
    void rejectsMissingToken() {
        assertThatThrownBy(() -> provider.merge(
                        new MergeRequest(repositoryRef, 1L, "url", "sha", "main", MergeMethod.SQUASH), ""))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MERGE_PROVIDER_FAILED");
    }

    private static MockResponse openPrBody(String headSha) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "number": 7,
                          "html_url": "https://github.com/ghurm-nova-platform/nova-demo/pull/7",
                          "title": "PR",
                          "state": "open",
                          "merged": false,
                          "head": {"ref":"feature","sha":"%s"},
                          "base": {"ref":"main"}
                        }
                        """.formatted(headSha));
    }
}
