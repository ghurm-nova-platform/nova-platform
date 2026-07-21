package ai.nova.platform.llm.provider;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.llm.entity.LlmProviderType;
import ai.nova.platform.llm.error.LlmErrorCodes;
import ai.nova.platform.llm.provider.LlmCompletionRequest.LlmChatMessage;

/**
 * Shared OpenAI-compatible chat completions client for local runtimes.
 * Uses RestClient.create() so localhost endpoints are allowed.
 */
public final class OpenAiCompatibleClient {

    public OpenAiCompatibleClient() {
    }

    public OpenAiCompatibleClient(ObjectMapper ignored) {
        this();
    }

    public LlmCompletionResult chatCompletions(
            String baseUrl, LlmCompletionRequest request, LlmProviderType providerType) {
        long start = System.nanoTime();
        String root = trimTrailingSlash(baseUrl);
        RestClient client = RestClient.create();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.modelCode());
        body.put("messages", toMessages(request.messages()));
        if (request.maxTokens() != null) {
            body.put("max_tokens", request.maxTokens());
        }
        if (request.temperature() != null) {
            body.put("temperature", request.temperature());
        }
        body.put("stream", false);
        try {
            JsonNode response = client.post()
                    .uri(URI.create(root + "/v1/chat/completions"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String content = extractContent(response);
            int inputTokens = readUsage(response, "prompt_tokens", estimateInput(request));
            int outputTokens = readUsage(response, "completion_tokens", Math.max(1, content.split("\\s+").length));
            long latencyMs = (System.nanoTime() - start) / 1_000_000L;
            return new LlmCompletionResult(content, inputTokens, outputTokens, latencyMs, providerType, "stop");
        } catch (RestClientException ex) {
            throw new LlmProviderException(
                    LlmErrorCodes.PROVIDER_ERROR, providerType + " request failed: " + ex.getMessage(), true);
        } catch (Exception ex) {
            throw new LlmProviderException(
                    LlmErrorCodes.PROVIDER_ERROR, providerType + " request failed: " + ex.getMessage(), true);
        }
    }

    public LlmCompletionResult ollamaChat(String baseUrl, LlmCompletionRequest request) {
        long start = System.nanoTime();
        String root = trimTrailingSlash(baseUrl);
        RestClient client = RestClient.create();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.modelCode());
        body.put("messages", toMessages(request.messages()));
        body.put("stream", false);
        if (request.temperature() != null) {
            body.put("options", Map.of("temperature", request.temperature()));
        }
        try {
            JsonNode response = client.post()
                    .uri(URI.create(root + "/api/chat"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String content = "";
            if (response != null && response.path("message").path("content").isTextual()) {
                content = response.path("message").path("content").asText();
            }
            int inputTokens = estimateInput(request);
            int outputTokens = Math.max(1, content.isBlank() ? 0 : content.split("\\s+").length);
            long latencyMs = (System.nanoTime() - start) / 1_000_000L;
            return new LlmCompletionResult(
                    content, inputTokens, outputTokens, latencyMs, LlmProviderType.OLLAMA, "stop");
        } catch (RestClientException ex) {
            // Fall back to OpenAI-compatible path
            return chatCompletions(baseUrl, request, LlmProviderType.OLLAMA);
        }
    }

    private static List<Map<String, String>> toMessages(List<LlmChatMessage> messages) {
        List<Map<String, String>> out = new ArrayList<>();
        if (messages == null) {
            return out;
        }
        for (LlmChatMessage message : messages) {
            if (message == null) {
                continue;
            }
            out.add(Map.of(
                    "role", message.role() == null ? "user" : message.role().toLowerCase(),
                    "content", message.content() == null ? "" : message.content()));
        }
        return out;
    }

    private String extractContent(JsonNode response) {
        if (response == null) {
            return "";
        }
        JsonNode choices = response.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode message = choices.get(0).path("message");
            if (message.path("content").isTextual()) {
                return message.path("content").asText();
            }
            if (choices.get(0).path("text").isTextual()) {
                return choices.get(0).path("text").asText();
            }
        }
        return "";
    }

    private static int readUsage(JsonNode response, String field, int fallback) {
        if (response != null && response.path("usage").path(field).isIntegralNumber()) {
            return response.path("usage").path(field).asInt();
        }
        return fallback;
    }

    private static int estimateInput(LlmCompletionRequest request) {
        int total = 0;
        if (request.messages() == null) {
            return 0;
        }
        for (LlmChatMessage message : request.messages()) {
            if (message != null && message.content() != null && !message.content().isBlank()) {
                total += Math.max(1, message.content().trim().split("\\s+").length);
            }
        }
        return total;
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
