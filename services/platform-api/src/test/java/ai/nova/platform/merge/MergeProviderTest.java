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
    void mergePullRequest() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "number": 7,
                          "html_url": "https://github.com/ghurm-nova-platform/nova-demo/pull/7",
                          "title": "PR",
                          "state": "open",
                          "head": {"ref":"feature","sha":"abc123"},
                          "base": {"ref":"main"}
                        }
                        """));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"sha":"merged123","merged":true,"message":"Pull Request successfully merged"}
                        """));

        MergeOutcome outcome = provider.merge(
                new MergeRequest(repositoryRef, 7L, "https://github.com/ghurm-nova-platform/nova-demo/pull/7", "abc123", "main", MergeMethod.SQUASH),
                "test-token-not-real");
        assertThat(outcome.succeeded()).isTrue();
        assertThat(outcome.alreadyMerged()).isFalse();
        assertThat(outcome.mergedCommitSha()).isEqualTo("merged123");

        RecordedRequest getRequest = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(getRequest.getPath()).isEqualTo("/repos/ghurm-nova-platform/nova-demo/pulls/7");
        RecordedRequest mergeRequest = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(mergeRequest.getPath()).isEqualTo("/repos/ghurm-nova-platform/nova-demo/pulls/7/merge");
        assertThat(mergeRequest.getMethod()).isEqualTo("PUT");
        assertThat(mergeRequest.getBody().readUtf8()).contains("\"merge_method\":\"squash\"");
    }

    @Test
    void alreadyMergedSkipsPut() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "number": 7,
                          "html_url": "https://github.com/ghurm-nova-platform/nova-demo/pull/7",
                          "title": "PR",
                          "state": "merged",
                          "head": {"ref":"feature","sha":"abc123"},
                          "base": {"ref":"main"}
                        }
                        """));

        MergeOutcome outcome = provider.merge(
                new MergeRequest(repositoryRef, 7L, "https://github.com/ghurm-nova-platform/nova-demo/pull/7", "abc123", "main", MergeMethod.SQUASH),
                "test-token-not-real");
        assertThat(outcome.succeeded()).isTrue();
        assertThat(outcome.alreadyMerged()).isTrue();
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void rejectsMissingToken() {
        assertThatThrownBy(() -> provider.merge(
                        new MergeRequest(repositoryRef, 1L, "url", "sha", "main", MergeMethod.SQUASH),
                        ""))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MERGE_PROVIDER_FAILED");
    }
}
