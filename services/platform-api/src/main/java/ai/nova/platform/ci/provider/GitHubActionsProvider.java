package ai.nova.platform.ci.provider;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;

import ai.nova.platform.ci.config.CiObservationProperties;
import ai.nova.platform.pullrequest.provider.RepositoryRef;
import ai.nova.platform.web.error.ApiException;

/**
 * Read-only GitHub Actions workflow observer. Never POSTs to rerun or cancel endpoints.
 */
@Component
@ConditionalOnProperty(name = "nova.ci-observation.provider", havingValue = "GITHUB", matchIfMissing = true)
public class GitHubActionsProvider implements CiProvider {

    private static final String PROVIDER_ID = "GITHUB";

    private final RestClient restClient;

    public GitHubActionsProvider(CiObservationProperties properties) {
        this.restClient = buildClient(properties);
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderWorkflowRun> listWorkflowRuns(RepositoryRef ref, WorkflowRunQuery query, String token) {
        requireToken(token);
        try {
            JsonNode response = restClient
                    .get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/repos/{owner}/{repo}/actions/runs")
                                .queryParam("event", "pull_request")
                                .queryParam("per_page", Math.max(1, query.maxRuns()));
                        if (query.branch() != null && !query.branch().isBlank()) {
                            builder = builder.queryParam("branch", query.branch());
                        }
                        if (query.commitHash() != null && !query.commitHash().isBlank()) {
                            builder = builder.queryParam("head_sha", query.commitHash());
                        }
                        return builder.build(ref.owner(), ref.name());
                    })
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.has("workflow_runs")) {
                throw invalidResponse("Workflow runs response missing workflow_runs");
            }
            JsonNode runs = response.get("workflow_runs");
            if (!runs.isArray()) {
                throw invalidResponse("Workflow runs response is not an array");
            }
            List<ProviderWorkflowRun> result = new ArrayList<>();
            for (JsonNode run : runs) {
                result.add(parseWorkflowRun(run));
            }
            return List.copyOf(result);
        } catch (RestClientResponseException ex) {
            throw mapHttpError(ex, "CI_FETCH_FAILED", "GitHub workflow run listing failed");
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "CI_PROVIDER_RESPONSE_INVALID",
                    "GitHub workflow run listing failed: " + safeMessage(ex));
        }
    }

    @Override
    public List<ProviderJob> listJobs(RepositoryRef ref, String externalRunId, String token) {
        requireToken(token);
        try {
            JsonNode response = restClient
                    .get()
                    .uri("/repos/{owner}/{repo}/actions/runs/{run_id}/jobs", ref.owner(), ref.name(), externalRunId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.has("jobs")) {
                throw invalidResponse("Jobs response missing jobs");
            }
            JsonNode jobs = response.get("jobs");
            if (!jobs.isArray()) {
                throw invalidResponse("Jobs response is not an array");
            }
            List<ProviderJob> result = new ArrayList<>();
            for (JsonNode job : jobs) {
                result.add(parseJob(job));
            }
            return List.copyOf(result);
        } catch (RestClientResponseException ex) {
            throw mapHttpError(ex, "CI_FETCH_FAILED", "GitHub job listing failed");
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "CI_PROVIDER_RESPONSE_INVALID",
                    "GitHub job listing failed: " + safeMessage(ex));
        }
    }

    private ProviderWorkflowRun parseWorkflowRun(JsonNode node) {
        if (!node.has("id") || !node.has("name")) {
            throw invalidResponse("Workflow run missing required fields");
        }
        String externalRunId = node.get("id").asText();
        String workflowName = node.get("name").asText();
        String externalWorkflowId = node.has("workflow_id") ? node.get("workflow_id").asText() : null;
        String runUrl = node.has("html_url") ? node.get("html_url").asText() : null;
        String status = node.has("status") ? node.get("status").asText() : "unknown";
        String conclusion = node.has("conclusion") && !node.get("conclusion").isNull()
                ? node.get("conclusion").asText()
                : null;
        Long durationMs = computeDurationMs(node);
        String triggerEvent = node.has("event") ? node.get("event").asText() : null;
        String commitHash = node.has("head_sha") ? node.get("head_sha").asText() : null;
        String branch = node.has("head_branch") ? node.get("head_branch").asText() : null;
        Long pullRequestNumber = extractPullRequestNumber(node);
        String failureReason = conclusion != null && "failure".equalsIgnoreCase(conclusion)
                ? "Workflow run failed"
                : null;
        Instant startedAt = parseInstant(node, "run_started_at", "created_at");
        Instant completedAt = parseInstant(node, "updated_at");
        return new ProviderWorkflowRun(
                externalWorkflowId,
                workflowName,
                externalRunId,
                runUrl,
                status,
                conclusion,
                durationMs,
                triggerEvent,
                commitHash,
                branch,
                pullRequestNumber,
                failureReason,
                startedAt,
                completedAt);
    }

    private ProviderJob parseJob(JsonNode node) {
        if (!node.has("id") || !node.has("name")) {
            throw invalidResponse("Job missing required fields");
        }
        String externalJobId = node.get("id").asText();
        String jobName = node.get("name").asText();
        String status = node.has("status") ? node.get("status").asText() : "unknown";
        String conclusion = node.has("conclusion") && !node.get("conclusion").isNull()
                ? node.get("conclusion").asText()
                : null;
        Long durationMs = computeDurationMs(node);
        String failureReason = conclusion != null && "failure".equalsIgnoreCase(conclusion)
                ? "Job failed"
                : null;
        Instant startedAt = parseInstant(node, "started_at");
        Instant completedAt = parseInstant(node, "completed_at");
        List<ProviderStep> steps = parseSteps(node);
        return new ProviderJob(
                externalJobId, jobName, status, conclusion, durationMs, failureReason, steps, startedAt, completedAt);
    }

    private List<ProviderStep> parseSteps(JsonNode jobNode) {
        if (!jobNode.has("steps") || !jobNode.get("steps").isArray()) {
            return List.of();
        }
        List<ProviderStep> steps = new ArrayList<>();
        int stepNumber = 1;
        for (JsonNode step : jobNode.get("steps")) {
            String stepName = step.has("name") ? step.get("name").asText() : "step-" + stepNumber;
            String status = step.has("status") ? step.get("status").asText() : "unknown";
            String conclusion = step.has("conclusion") && !step.get("conclusion").isNull()
                    ? step.get("conclusion").asText()
                    : null;
            Long durationMs = step.has("number") ? null : null;
            if (step.has("started_at") && step.has("completed_at")) {
                Instant start = parseInstant(step, "started_at");
                Instant end = parseInstant(step, "completed_at");
                if (start != null && end != null) {
                    durationMs = Duration.between(start, end).toMillis();
                }
            }
            String failureReason = conclusion != null && "failure".equalsIgnoreCase(conclusion)
                    ? "Step failed: " + stepName
                    : null;
            steps.add(new ProviderStep(
                    stepNumber,
                    stepName,
                    status,
                    conclusion,
                    durationMs,
                    failureReason,
                    parseInstant(step, "started_at"),
                    parseInstant(step, "completed_at")));
            stepNumber++;
        }
        return List.copyOf(steps);
    }

    private static Long extractPullRequestNumber(JsonNode node) {
        if (node.has("pull_requests") && node.get("pull_requests").isArray()) {
            JsonNode prs = node.get("pull_requests");
            if (!prs.isEmpty() && prs.get(0).has("number")) {
                return prs.get(0).get("number").asLong();
            }
        }
        return null;
    }

    private static Long computeDurationMs(JsonNode node) {
        Instant startedAt = parseInstant(node, "run_started_at", "started_at", "created_at");
        Instant completedAt = parseInstant(node, "updated_at", "completed_at");
        if (startedAt != null && completedAt != null) {
            return Duration.between(startedAt, completedAt).toMillis();
        }
        return null;
    }

    private static Instant parseInstant(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.has(fieldName) && !node.get(fieldName).isNull()) {
                String value = node.get(fieldName).asText();
                if (value == null || value.isBlank()) {
                    continue;
                }
                try {
                    return Instant.parse(value);
                } catch (DateTimeParseException ignored) {
                    // try next field
                }
            }
        }
        return null;
    }

    private static RestClient buildClient(CiObservationProperties properties) {
        int timeoutSeconds = Math.max(1, properties.getRequestTimeoutSeconds());
        Duration timeout = Duration.ofSeconds(timeoutSeconds);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        String baseUrl = properties.getGithubApiBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    private static void requireToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CI_CREDENTIALS_MISSING", "GitHub token is not configured");
        }
    }

    private static String bearer(String token) {
        return "Bearer " + token.trim();
    }

    private static ApiException invalidResponse(String message) {
        return new ApiException(HttpStatus.BAD_GATEWAY, "CI_PROVIDER_RESPONSE_INVALID", message);
    }

    private static ApiException mapHttpError(RestClientResponseException ex, String defaultCode, String prefix) {
        String code = defaultCode;
        if (ex.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()
                || ex.getStatusCode().value() == HttpStatus.FORBIDDEN.value()) {
            code = "CI_CREDENTIALS_MISSING";
        } else if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
            code = "CI_NOT_FOUND";
        }
        return new ApiException(
                HttpStatus.valueOf(ex.getStatusCode().value()),
                code,
                prefix + " (" + ex.getStatusCode().value() + ")");
    }

    private static String safeMessage(Throwable ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message.toLowerCase(Locale.ROOT).contains("token") ? "provider request failed" : message;
    }
}
