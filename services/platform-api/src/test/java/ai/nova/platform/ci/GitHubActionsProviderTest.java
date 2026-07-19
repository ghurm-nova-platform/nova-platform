package ai.nova.platform.ci;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ai.nova.platform.ci.config.CiObservationProperties;
import ai.nova.platform.ci.provider.GitHubActionsProvider;
import ai.nova.platform.ci.provider.ProviderJob;
import ai.nova.platform.ci.provider.ProviderWorkflowRun;
import ai.nova.platform.ci.provider.WorkflowRunQuery;
import ai.nova.platform.pullrequest.provider.RepositoryRef;
import ai.nova.platform.web.error.ApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class GitHubActionsProviderTest {

    private MockWebServer server;
    private GitHubActionsProvider provider;
    private RepositoryRef repositoryRef;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        CiObservationProperties properties = new CiObservationProperties();
        properties.setGithubApiBaseUrl(server.url("/").toString().replaceAll("/$", ""));
        properties.setGithubToken("test-token-not-real");
        provider = new GitHubActionsProvider(properties);
        repositoryRef = new RepositoryRef("github.com", "ghurm-nova-platform", "nova-demo");
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void listWorkflowRuns() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "workflow_runs": [{
                            "id": 1001,
                            "workflow_id": 42,
                            "name": "CI",
                            "html_url": "https://github.com/ghurm-nova-platform/nova-demo/actions/runs/1001",
                            "status": "completed",
                            "conclusion": "success",
                            "event": "pull_request",
                            "head_sha": "abc123",
                            "head_branch": "feature/ci",
                            "run_started_at": "2026-01-01T00:00:00Z",
                            "updated_at": "2026-01-01T00:05:00Z",
                            "pull_requests": [{"number": 7}]
                          }]
                        }
                        """));

        List<ProviderWorkflowRun> runs = provider.listWorkflowRuns(
                repositoryRef,
                new WorkflowRunQuery("feature/ci", "abc123", 7L, 20),
                "test-token-not-real");

        assertThat(runs).hasSize(1);
        assertThat(runs.get(0).externalRunId()).isEqualTo("1001");
        assertThat(runs.get(0).workflowName()).isEqualTo("CI");
        assertThat(runs.get(0).conclusion()).isEqualTo("success");

        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request.getPath()).contains("/actions/runs");
        assertThat(request.getPath()).containsAnyOf("branch=feature%2Fci", "branch=feature/ci");
        assertThat(request.getPath()).contains("head_sha=abc123");
        assertThat(request.getPath()).contains("event=pull_request");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-token-not-real");
        assertThat(request.getMethod()).isEqualTo("GET");
    }

    @Test
    void listJobsWithSteps() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "jobs": [{
                            "id": 2001,
                            "name": "build",
                            "status": "completed",
                            "conclusion": "failure",
                            "started_at": "2026-01-01T00:01:00Z",
                            "completed_at": "2026-01-01T00:02:00Z",
                            "steps": [{
                              "name": "compile",
                              "status": "completed",
                              "conclusion": "failure",
                              "started_at": "2026-01-01T00:01:10Z",
                              "completed_at": "2026-01-01T00:01:50Z"
                            }]
                          }]
                        }
                        """));

        List<ProviderJob> jobs = provider.listJobs(repositoryRef, "1001", "test-token-not-real");

        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).jobName()).isEqualTo("build");
        assertThat(jobs.get(0).conclusion()).isEqualTo("failure");
        assertThat(jobs.get(0).steps()).hasSize(1);
        assertThat(jobs.get(0).steps().get(0).stepName()).isEqualTo("compile");

        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request.getPath()).isEqualTo("/repos/ghurm-nova-platform/nova-demo/actions/runs/1001/jobs");
        assertThat(request.getMethod()).isEqualTo("GET");
    }

    @Test
    void rejectsMissingToken() {
        assertThatThrownBy(() -> provider.listWorkflowRuns(
                        repositoryRef, new WorkflowRunQuery("main", "abc", 1L, 10), ""))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("CI_CREDENTIALS_MISSING");
    }

    @Test
    void mapsUnauthorizedToCredentialsMissing() {
        server.enqueue(new MockResponse().setResponseCode(401));

        assertThatThrownBy(() -> provider.listWorkflowRuns(
                        repositoryRef, new WorkflowRunQuery("main", "abc", 1L, 10), "test-token-not-real"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("CI_CREDENTIALS_MISSING");
    }

    @Test
    void mapsNotFound() {
        server.enqueue(new MockResponse().setResponseCode(404));

        assertThatThrownBy(() -> provider.listJobs(repositoryRef, "999", "test-token-not-real"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("CI_NOT_FOUND");
    }
}
