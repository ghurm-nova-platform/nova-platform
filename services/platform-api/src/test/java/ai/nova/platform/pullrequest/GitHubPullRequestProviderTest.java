package ai.nova.platform.pullrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.pullrequest.config.PullRequestProperties;
import ai.nova.platform.pullrequest.provider.CreatePullRequestRequest;
import ai.nova.platform.pullrequest.provider.GitHubPullRequestProvider;
import ai.nova.platform.pullrequest.provider.ProviderPullRequest;
import ai.nova.platform.pullrequest.provider.RepositoryRef;
import ai.nova.platform.web.error.ApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class GitHubPullRequestProviderTest {

    private MockWebServer server;
    private GitHubPullRequestProvider provider;
    private RepositoryRef repositoryRef;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        PullRequestProperties properties = new PullRequestProperties();
        properties.setGithubApiBaseUrl(server.url("/").toString().replaceAll("/$", ""));
        properties.setGithubToken("test-token-not-real");
        provider = new GitHubPullRequestProvider(properties, new ObjectMapper());
        repositoryRef = new RepositoryRef("github.com", "ghurm-nova-platform", "nova-demo");
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void validateRepository() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"full_name":"ghurm-nova-platform/nova-demo"}
                        """));

        provider.validateRepository(repositoryRef, "test-token-not-real");

        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request.getPath()).isEqualTo("/repos/ghurm-nova-platform/nova-demo");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-token-not-real");
    }

    @Test
    void findExistingPullRequest() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [{
                          "id": 42,
                          "number": 7,
                          "html_url": "https://github.com/ghurm-nova-platform/nova-demo/pull/7",
                          "title": "Existing PR",
                          "state": "open",
                          "head": {"ref":"ai/task-1","sha":"abc123"},
                          "base": {"ref":"main"}
                        }]
                        """));

        Optional<ProviderPullRequest> found =
                provider.findExistingPullRequest(repositoryRef, "ai/task-1", "main", "test-token-not-real");
        assertThat(found).isPresent();
        assertThat(found.get().number()).isEqualTo(7L);
        assertThat(found.get().url()).contains("/pull/7");

        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request.getPath()).contains("/pulls");
        assertThat(request.getBody().readUtf8()).doesNotContain("test-token-not-real");
    }

    @Test
    void createPullRequest() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "id": 99,
                          "number": 12,
                          "html_url": "https://github.com/ghurm-nova-platform/nova-demo/pull/12",
                          "title": "Nova PR",
                          "state": "open",
                          "head": {"ref":"ai/task-2","sha":"def456"},
                          "base": {"ref":"main"}
                        }
                        """));

        ProviderPullRequest created = provider.createPullRequest(
                new CreatePullRequestRequest(
                        repositoryRef, "Nova PR", "body", "ai/task-2", "main", true),
                "test-token-not-real");
        assertThat(created.number()).isEqualTo(12L);
        assertThat(created.sourceBranch()).isEqualTo("ai/task-2");

        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody().readUtf8()).doesNotContain("test-token-not-real");
    }

    @Test
    void rejectsMissingToken() {
        assertThatThrownBy(() -> provider.validateRepository(repositoryRef, ""))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PR_CREDENTIALS_MISSING");
    }
}
