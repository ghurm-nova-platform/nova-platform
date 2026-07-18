package ai.nova.platform.modelgateway.provider.azure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.runtime.RuntimeMessage;
import ai.nova.platform.modelgateway.config.ModelGatewayProperties;
import ai.nova.platform.modelgateway.entity.EndpointProfile;
import ai.nova.platform.modelgateway.provider.ProviderEndpointConfig;
import ai.nova.platform.modelgateway.provider.ProviderInvokeOutcome;
import ai.nova.platform.modelgateway.provider.ProviderInvokeRequest;
import ai.nova.platform.modelgateway.provider.ProviderInvokeResult;
import ai.nova.platform.modelgateway.provider.error.UnifiedProviderErrorMapper;
import ai.nova.platform.modelgateway.provider.http.ProviderHostAllowlist;
import ai.nova.platform.modelgateway.provider.http.ProviderRestClientFactory;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class AzureOpenAiModelProviderTest {

    private MockWebServer server;
    private AzureOpenAiModelProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        ModelGatewayProperties properties = new ModelGatewayProperties();
        properties.setAllowLocalhostOverrides(true);
        String base = server.url("/").toString().replaceAll("/$", "");
        properties.getProviders().getAzureOpenai().setBaseUrlTemplate(base);
        provider = new AzureOpenAiModelProvider(
                new ProviderRestClientFactory(properties, new ProviderHostAllowlist()),
                new UnifiedProviderErrorMapper(),
                new ObjectMapper(),
                properties);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void invokeSuccessUsesDeploymentPathAndApiKey() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "id":"chatcmpl-az",
                          "choices":[{"message":{"role":"assistant","content":"azure ok"},"finish_reason":"stop"}],
                          "usage":{"prompt_tokens":3,"completion_tokens":2,"total_tokens":5}
                        }
                        """));

        ProviderInvokeResult result = provider.invoke(new ProviderInvokeRequest(
                "my-deployment",
                "system",
                List.of(new RuntimeMessage("USER", "hi")),
                List.of(),
                List.of(),
                null,
                32,
                10,
                "azure-key",
                new ProviderEndpointConfig(
                        EndpointProfile.AZURE_OPENAI_RESOURCE, "myresource", "2024-02-15-preview")));

        assertThat(result.outcome()).isEqualTo(ProviderInvokeOutcome.FINAL);
        assertThat(result.responseText()).isEqualTo("azure ok");
        assertThat(result.inputTokens()).isEqualTo(3);

        RecordedRequest recorded = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(recorded.getPath())
                .isEqualTo("/openai/deployments/my-deployment/chat/completions?api-version=2024-02-15-preview");
        assertThat(recorded.getHeader("api-key")).isEqualTo("azure-key");
        assertThat(recorded.getHeader("Authorization")).isNull();
    }
}
