package ai.nova.platform.pullrequest.provider;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

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

import ai.nova.platform.pullrequest.config.PullRequestProperties;
import ai.nova.platform.web.error.ApiException;

@Component
@ConditionalOnProperty(name = "nova.pull-request.provider", havingValue = "GITHUB", matchIfMissing = true)
public class GitHubPullRequestProvider implements PullRequestProvider {

    private static final String PROVIDER_ID = "GITHUB";

    private final PullRequestProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GitHubPullRequestProvider(PullRequestProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = buildClient(properties);
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public void validateRepository(RepositoryRef ref, String token) {
        requireToken(token);
        try {
            JsonNode response = restClient
                    .get()
                    .uri("/repos/{owner}/{repo}", ref.owner(), ref.name())
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.has("full_name")) {
                throw invalidResponse("Repository validation returned empty response");
            }
            String fullName = response.get("full_name").asText();
            String expected = ref.owner() + "/" + ref.name();
            if (!expected.equalsIgnoreCase(fullName)) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "PR_REPOSITORY_MISMATCH",
                        "Configured repository does not match remote: " + fullName);
            }
        } catch (RestClientResponseException ex) {
            throw mapHttpError(ex, "PR_REMOTE_NOT_ALLOWED", "GitHub repository validation failed");
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "PR_PROVIDER_RESPONSE_INVALID",
                    "GitHub repository validation failed: " + safeMessage(ex));
        }
    }

    @Override
    public Optional<RemoteBranchInfo> findRemoteBranch(RepositoryRef ref, String branch, String token) {
        requireToken(token);
        try {
            JsonNode response = restClient
                    .get()
                    .uri("/repos/{owner}/{repo}/git/ref/heads/{branch}", ref.owner(), ref.name(), branch)
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.has("object")) {
                return Optional.empty();
            }
            JsonNode object = response.get("object");
            if (!object.has("sha")) {
                return Optional.empty();
            }
            return Optional.of(new RemoteBranchInfo(branch, object.get("sha").asText()));
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return Optional.empty();
            }
            throw mapHttpError(ex, "PR_PROVIDER_RESPONSE_INVALID", "Failed to resolve remote branch");
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "PR_PROVIDER_RESPONSE_INVALID",
                    "Failed to resolve remote branch: " + safeMessage(ex));
        }
    }

    @Override
    public Optional<ProviderPullRequest> findExistingPullRequest(
            RepositoryRef ref, String sourceBranch, String targetBranch, String token) {
        requireToken(token);
        String head = ref.owner() + ":" + sourceBranch;
        try {
            JsonNode response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/pulls")
                            .queryParam("head", head)
                            .queryParam("base", targetBranch)
                            .queryParam("state", "open")
                            .build(ref.owner(), ref.name()))
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.isArray() || response.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(parsePullRequest(response.get(0)));
        } catch (RestClientResponseException ex) {
            throw mapHttpError(ex, "PR_PROVIDER_RESPONSE_INVALID", "Failed to search for existing pull request");
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "PR_PROVIDER_RESPONSE_INVALID",
                    "Failed to search for existing pull request: " + safeMessage(ex));
        }
    }

    @Override
    public ProviderPullRequest createPullRequest(CreatePullRequestRequest request, String token) {
        requireToken(token);
        RepositoryRef ref = request.repository();
        try {
            String payload = objectMapper
                    .createObjectNode()
                    .put("title", request.title())
                    .put("body", request.body())
                    .put("head", request.sourceBranch())
                    .put("base", request.targetBranch())
                    .put("draft", request.draft())
                    .toString();
            JsonNode response = restClient
                    .post()
                    .uri("/repos/{owner}/{repo}/pulls", ref.owner(), ref.name())
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                throw invalidResponse("Pull request creation returned empty response");
            }
            return parsePullRequest(response);
        } catch (RestClientResponseException ex) {
            throw mapHttpError(ex, "PR_CREATE_FAILED", "GitHub pull request creation failed");
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY, "PR_CREATE_FAILED", "GitHub pull request creation failed: " + safeMessage(ex));
        }
    }

    @Override
    public ProviderPullRequest getPullRequest(RepositoryRef ref, long number, String token) {
        requireToken(token);
        try {
            JsonNode response = restClient
                    .get()
                    .uri("/repos/{owner}/{repo}/pulls/{number}", ref.owner(), ref.name(), number)
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                throw invalidResponse("Pull request lookup returned empty response");
            }
            return parsePullRequest(response);
        } catch (RestClientResponseException ex) {
            throw mapHttpError(ex, "PR_VERIFY_FAILED", "GitHub pull request verification failed");
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "PR_VERIFY_FAILED",
                    "GitHub pull request verification failed: " + safeMessage(ex));
        }
    }

    private ProviderPullRequest parsePullRequest(JsonNode node) {
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
        String externalId = node.has("id") ? node.get("id").asText() : String.valueOf(node.get("number").asLong());
        return new ProviderPullRequest(
                externalId,
                node.get("number").asLong(),
                node.get("html_url").asText(),
                node.get("title").asText(),
                head.get("ref").asText(),
                base.get("ref").asText(),
                node.get("state").asText(),
                head.get("sha").asText());
    }

    private static RestClient buildClient(PullRequestProperties properties) {
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
            throw new ApiException(HttpStatus.BAD_REQUEST, "PR_CREDENTIALS_MISSING", "GitHub token is not configured");
        }
    }

    private static String bearer(String token) {
        return "Bearer " + token.trim();
    }

    private static ApiException invalidResponse(String message) {
        return new ApiException(HttpStatus.BAD_GATEWAY, "PR_PROVIDER_RESPONSE_INVALID", message);
    }

    private static ApiException mapHttpError(RestClientResponseException ex, String defaultCode, String prefix) {
        String code = defaultCode;
        if (ex.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()
                || ex.getStatusCode().value() == HttpStatus.FORBIDDEN.value()) {
            code = "PR_CREDENTIALS_MISSING";
        } else if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
            code = "PR_REMOTE_NOT_ALLOWED";
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
