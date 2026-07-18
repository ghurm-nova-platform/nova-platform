package ai.nova.platform.modelgateway.provider.azure;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.nova.platform.modelgateway.config.ModelGatewayProperties;
import ai.nova.platform.modelgateway.entity.EndpointProfile;
import ai.nova.platform.modelgateway.provider.AiModelProvider;
import ai.nova.platform.modelgateway.provider.ProviderCapabilities;
import ai.nova.platform.modelgateway.provider.ProviderEndpointConfig;
import ai.nova.platform.modelgateway.provider.ProviderException;
import ai.nova.platform.modelgateway.provider.ProviderFailureKind;
import ai.nova.platform.modelgateway.provider.ProviderInvokeOutcome;
import ai.nova.platform.modelgateway.provider.ProviderInvokeRequest;
import ai.nova.platform.modelgateway.provider.ProviderInvokeResult;
import ai.nova.platform.modelgateway.provider.error.UnifiedProviderErrorMapper;
import ai.nova.platform.modelgateway.provider.http.ProviderRestClientFactory;
import ai.nova.platform.modelgateway.provider.openai.OpenAiChatCompletionsMapper;

@Component
public class AzureOpenAiModelProvider implements AiModelProvider {

    public static final String ADAPTER_KEY = "AZURE_OPENAI";

    private final ProviderRestClientFactory restClientFactory;
    private final UnifiedProviderErrorMapper errorMapper;
    private final OpenAiChatCompletionsMapper mapper;

    public AzureOpenAiModelProvider(
            ProviderRestClientFactory restClientFactory,
            UnifiedProviderErrorMapper errorMapper,
            ObjectMapper objectMapper,
            ModelGatewayProperties properties) {
        this.restClientFactory = restClientFactory;
        this.errorMapper = errorMapper;
        this.mapper = new OpenAiChatCompletionsMapper(objectMapper, properties);
    }

    @Override
    public String adapterKey() {
        return ADAPTER_KEY;
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(true, true, true, true, false);
    }

    @Override
    public ProviderInvokeResult invoke(ProviderInvokeRequest request) throws ProviderException {
        ProviderEndpointConfig endpoint = request.endpoint();
        if (endpoint == null
                || endpoint.endpointProfile() != EndpointProfile.AZURE_OPENAI_RESOURCE
                || endpoint.azureResourceName() == null
                || endpoint.azureResourceName().isBlank()
                || endpoint.azureApiVersion() == null
                || endpoint.azureApiVersion().isBlank()) {
            throw new ProviderException(
                    "ENDPOINT_PROFILE_INVALID",
                    ProviderFailureKind.PERMANENT,
                    "AZURE_OPENAI requires resource name and api version");
        }
        if (request.credentialSecret() == null || request.credentialSecret().isBlank()) {
            throw new ProviderException(
                    "PROVIDER_AUTHENTICATION_FAILED",
                    ProviderFailureKind.PERMANENT,
                    "Missing provider credential");
        }
        if (request.providerModelId() == null || request.providerModelId().isBlank()) {
            throw new ProviderException(
                    "MODEL_NOT_SUPPORTED", ProviderFailureKind.PERMANENT, "Missing Azure deployment name");
        }

        int timeout = request.timeoutSeconds() != null ? request.timeoutSeconds() : 60;
        URI baseUri = restClientFactory.resolveAzureBaseUrl(endpoint.azureResourceName());
        RestClient client = restClientFactory.create(baseUri, timeout);
        ObjectNode body = mapper.toRequestBody(request, request.providerModelId());
        // Azure uses deployment in path; model field is optional but harmless.
        String path = "/openai/deployments/"
                + encodePath(request.providerModelId())
                + "/chat/completions?api-version="
                + URLEncoder.encode(endpoint.azureApiVersion(), StandardCharsets.UTF_8);

        long start = System.nanoTime();
        try {
            JsonNode response = client.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("api-key", request.credentialSecret())
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return toResult(response, start);
        } catch (RestClientResponseException ex) {
            throw errorMapper.map(ex);
        } catch (ProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw errorMapper.mapTransport(ex);
        }
    }

    private ProviderInvokeResult toResult(JsonNode response, long startNanos) throws ProviderException {
        if (response == null) {
            throw new ProviderException("PROVIDER_ERROR", ProviderFailureKind.PERMANENT, "Empty provider response");
        }
        try {
            OpenAiChatCompletionsMapper.MappedCompletion mapped = mapper.mapResponse(response);
            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000L;
            if (mapped.toolCalls() != null && !mapped.toolCalls().isEmpty()) {
                return new ProviderInvokeResult(
                        ProviderInvokeOutcome.TOOL_CALLS,
                        null,
                        mapped.toolCalls(),
                        mapped.inputTokens(),
                        mapped.outputTokens(),
                        latencyMs,
                        mapped.finishReason() != null ? mapped.finishReason() : "tool_calls",
                        mapped.providerRequestId(),
                        null,
                        null);
            }
            String text = mapped.content() != null ? mapped.content() : "";
            return new ProviderInvokeResult(
                    ProviderInvokeOutcome.FINAL,
                    text,
                    java.util.List.of(),
                    mapped.inputTokens(),
                    mapped.outputTokens(),
                    latencyMs,
                    mapped.finishReason() != null ? mapped.finishReason() : "stop",
                    mapped.providerRequestId(),
                    null,
                    null);
        } catch (ProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProviderException("PROVIDER_ERROR", ProviderFailureKind.PERMANENT, "Invalid provider response");
        }
    }

    private static String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
