package ai.nova.platform.merge.provider;

import java.net.http.HttpClient;
import java.time.Instant;
import java.time.Duration;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.merge.config.MergeProperties;
import ai.nova.platform.merge.entity.MergeMethod;
import ai.nova.platform.pullrequest.config.PullRequestProperties;
import ai.nova.platform.pullrequest.provider.RepositoryRef;
import ai.nova.platform.web.error.ApiException;

@Component
@ConditionalOnProperty(name = "nova.merge.provider", havingValue = "GITHUB", matchIfMissing = true)
public class GitHubMergeProvider implements MergeProvider {

    private static final String PROVIDER_ID = "GITHUB";

    private final MergeProperties mergeProperties;
    private final PullRequestProperties pullRequestProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GitHubMergeProvider(
            MergeProperties mergeProperties,
            PullRequestProperties pullRequestProperties,
            ObjectMapper objectMapper) {
        this.mergeProperties = mergeProperties;
        this.pullRequestProperties = pullRequestProperties;
        this.objectMapper = objectMapper;
        this.restClient = buildClient(mergeProperties);
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public MergeOutcome merge(MergeRequest request, String token) {
        String effectiveToken = resolveToken(token);
        requireToken(effectiveToken);
        RepositoryRef ref = request.repository();
        RemotePullRequestState current = getPullRequest(ref, request.pullRequestNumber(), effectiveToken);
        if (current.isMerged()) {
            // Never store head SHA as merge commit SHA.
            return MergeOutcome.alreadyMerged(
                    current.headSha(),
                    blankToNull(current.mergeCommitSha()),
                    current.mergedAt(),
                    current.mergedBy(),
                    current.url(),
                    "Pull request already merged");
        }
        if (request.headSha() == null || request.headSha().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MERGE_INVALID_REQUEST", "Head SHA is required to merge");
        }
        try {
            String mergeMethod = toGithubMethod(request.mergeMethod());
            String payload = objectMapper
                    .createObjectNode()
                    .put("merge_method", mergeMethod)
                    .put("sha", request.headSha())
                    .toString();
            JsonNode response = restClient
                    .put()
                    .uri("/repos/{owner}/{repo}/pulls/{number}/merge", ref.owner(), ref.name(), request.pullRequestNumber())
                    .header(HttpHeaders.AUTHORIZATION, bearer(effectiveToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                return MergeOutcome.ambiguous(request.headSha(), request.pullRequestUrl(), "Merge returned empty response");
            }
            boolean mergedFlag = !response.has("merged") || response.get("merged").asBoolean(true);
            String mergeCommitSha = textOrNull(response, "sha");
            String message = response.has("message") ? response.get("message").asText() : "Pull request merged";
            String url = request.pullRequestUrl() != null ? request.pullRequestUrl() : current.url();
            if (!mergedFlag) {
                return MergeOutcome.ambiguous(request.headSha(), url, message);
            }
            // Do not fall back to headSha for merge commit.
            return MergeOutcome.success(
                    request.headSha(), mergeCommitSha, Instant.now(), null, url, message);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == HttpStatus.METHOD_NOT_ALLOWED.value()
                    || status == HttpStatus.CONFLICT.value()
                    || status >= 500
                    || status == HttpStatus.GATEWAY_TIMEOUT.value()
                    || status == HttpStatus.REQUEST_TIMEOUT.value()) {
                return MergeOutcome.ambiguous(
                        request.headSha(),
                        request.pullRequestUrl(),
                        "Merge provider response ambiguous (" + status + ")");
            }
            throw mapHttpError(ex, "MERGE_PROVIDER_FAILED", "GitHub merge failed");
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            return MergeOutcome.ambiguous(
                    request.headSha(),
                    request.pullRequestUrl(),
                    "Merge provider request failed: " + safeMessage(ex));
        }
    }

    @Override
    public RemotePullRequestState getPullRequest(RepositoryRef ref, long pullRequestNumber, String token) {
        String effectiveToken = resolveToken(token);
        requireToken(effectiveToken);
        try {
            JsonNode response = restClient
                    .get()
                    .uri("/repos/{owner}/{repo}/pulls/{number}", ref.owner(), ref.name(), pullRequestNumber)
                    .header(HttpHeaders.AUTHORIZATION, bearer(effectiveToken))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                throw invalidResponse("Pull request lookup returned empty response");
            }
            return parsePullRequest(response, ref);
        } catch (RestClientResponseException ex) {
            throw mapHttpError(ex, "MERGE_PROVIDER_FAILED", "GitHub pull request lookup failed");
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "MERGE_PROVIDER_FAILED",
                    "GitHub pull request lookup failed: " + safeMessage(ex));
        }
    }

    @Override
    public BranchProtectionStatus checkBranchProtection(RepositoryRef ref, String branch, String token) {
        String effectiveToken = resolveToken(token);
        requireToken(effectiveToken);
        try {
            restClient
                    .get()
                    .uri("/repos/{owner}/{repo}/branches/{branch}/protection", ref.owner(), ref.name(), branch)
                    .header(HttpHeaders.AUTHORIZATION, bearer(effectiveToken))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            return BranchProtectionStatus.PROTECTED;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return BranchProtectionStatus.NOT_PROTECTED;
            }
            throw mapHttpError(ex, "MERGE_PROVIDER_FAILED", "GitHub branch protection lookup failed");
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "MERGE_PROVIDER_FAILED",
                    "GitHub branch protection lookup failed: " + safeMessage(ex));
        }
    }

    private String resolveToken(String token) {
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        String mergeToken = mergeProperties.getGithubToken();
        if (mergeToken != null && !mergeToken.isBlank()) {
            return mergeToken.trim();
        }
        String prToken = pullRequestProperties.getGithubToken();
        if (prToken != null && !prToken.isBlank()) {
            return prToken.trim();
        }
        return "";
    }

    private static String toGithubMethod(MergeMethod method) {
        if (method == null) {
            return "squash";
        }
        return switch (method) {
            case MERGE -> "merge";
            case REBASE -> "rebase";
            case SQUASH -> "squash";
        };
    }

    private static RemotePullRequestState parsePullRequest(JsonNode node, RepositoryRef ref) {
        if (!node.has("number") || !node.has("html_url") || !node.has("title") || !node.has("state")) {
            throw invalidResponse("Pull request response missing required fields");
        }
        JsonNode head = node.get("head");
        JsonNode base = node.get("base");
        if (head == null
                || base == null
                || !head.has("ref")
                || !base.has("ref")
                || !head.has("sha")) {
            throw invalidResponse("Pull request response missing head/base metadata");
        }
        boolean merged = node.has("merged") && node.get("merged").asBoolean(false);
        String mergeCommitSha = textOrNull(node, "merge_commit_sha");
        Instant mergedAt = parseInstant(textOrNull(node, "merged_at"));
        String mergedBy = null;
        if (node.has("merged_by") && node.get("merged_by") != null && !node.get("merged_by").isNull()) {
            JsonNode by = node.get("merged_by");
            if (by.has("login")) {
                mergedBy = by.get("login").asText();
            }
        }
        String owner = ref != null ? ref.owner() : null;
        String name = ref != null ? ref.name() : null;
        if (node.has("base") && node.get("base").has("repo") && node.get("base").get("repo").has("full_name")) {
            String fullName = node.get("base").get("repo").get("full_name").asText();
            int slash = fullName.indexOf('/');
            if (slash > 0) {
                owner = fullName.substring(0, slash);
                name = fullName.substring(slash + 1);
            }
        }
        return new RemotePullRequestState(
                node.get("number").asLong(),
                node.get("html_url").asText(),
                node.get("title").asText(),
                head.get("ref").asText(),
                base.get("ref").asText(),
                node.get("state").asText(),
                merged,
                head.get("sha").asText(),
                blankToNull(mergeCommitSha),
                mergedAt,
                mergedBy,
                owner,
                name);
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        String text = node.get(field).asText();
        return blankToNull(text);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        return value.trim();
    }

    private static RestClient buildClient(MergeProperties properties) {
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
            throw new ApiException(HttpStatus.BAD_REQUEST, "MERGE_PROVIDER_FAILED", "GitHub token is not configured");
        }
    }

    private static String bearer(String token) {
        return "Bearer " + token.trim();
    }

    private static ApiException invalidResponse(String message) {
        return new ApiException(HttpStatus.BAD_GATEWAY, "MERGE_PROVIDER_FAILED", message);
    }

    private static ApiException mapHttpError(RestClientResponseException ex, String defaultCode, String prefix) {
        return new ApiException(
                HttpStatus.valueOf(ex.getStatusCode().value()),
                defaultCode,
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
