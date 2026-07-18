package ai.nova.platform.modelgateway.provider.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import ai.nova.platform.modelgateway.provider.ProviderException;
import ai.nova.platform.modelgateway.provider.ProviderFailureKind;
import ai.nova.platform.modelgateway.provider.ProviderInvokeOutcome;
import ai.nova.platform.modelgateway.provider.ProviderInvokeRequest;
import ai.nova.platform.modelgateway.provider.ProviderInvokeResult;
import ai.nova.platform.modelgateway.provider.error.UnifiedProviderErrorMapper;
import ai.nova.platform.modelgateway.provider.http.ProviderHostAllowlist;
import ai.nova.platform.modelgateway.provider.http.ProviderRestClientFactory;
import ai.nova.platform.modelgateway.provider.http.TestLocalhostEndpointOverrideGate;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class OpenAiModelProviderTest {

    private MockWebServer server;
    private OpenAiModelProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        ModelGatewayProperties properties = new ModelGatewayProperties();
        properties.getProviders().getOpenai().setBaseUrl(server.url("/").toString().replaceAll("/$", ""));
        provider = new OpenAiModelProvider(
                new ProviderRestClientFactory(
                        properties, new ProviderHostAllowlist(), new TestLocalhostEndpointOverrideGate()),
                new UnifiedProviderErrorMapper(),
                new ObjectMapper(),
                properties);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void invokeSuccessMapsUsageAndContent() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "id":"chatcmpl-1",
                          "choices":[{"message":{"role":"assistant","content":"hello world"},"finish_reason":"stop"}],
                          "usage":{"prompt_tokens":11,"completion_tokens":2,"total_tokens":13}
                        }
                        """));

        ProviderInvokeResult result = provider.invoke(request("secret-key"));
        assertThat(result.outcome()).isEqualTo(ProviderInvokeOutcome.FINAL);
        assertThat(result.responseText()).isEqualTo("hello world");
        assertThat(result.inputTokens()).isEqualTo(11);
        assertThat(result.outputTokens()).isEqualTo(2);
        assertThat(result.providerRequestId()).isEqualTo("chatcmpl-1");

        RecordedRequest recorded = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(recorded.getPath()).isEqualTo("/v1/chat/completions");
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer secret-key");
        assertThat(recorded.getBody().readUtf8()).doesNotContain("system-secret");
    }

    @Test
    void invokeMapsAuthenticationError() {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"error\":{\"message\":\"bad key\"}}"));
        assertThatThrownBy(() -> provider.invoke(request("bad")))
                .isInstanceOf(ProviderException.class)
                .satisfies(ex -> {
                    ProviderException pe = (ProviderException) ex;
                    assertThat(pe.errorCode()).isEqualTo("PROVIDER_AUTHENTICATION_FAILED");
                    assertThat(pe.failureKind()).isEqualTo(ProviderFailureKind.PERMANENT);
                    assertThat(pe.getMessage()).doesNotContain("bad key");
                });
    }

    private ProviderInvokeRequest request(String credential) {
        return new ProviderInvokeRequest(
                "gpt-4o-mini",
                "system",
                List.of(new RuntimeMessage("USER", "hi")),
                List.of(),
                List.of(),
                null,
                64,
                10,
                credential,
                new ProviderEndpointConfig(EndpointProfile.OPENAI_PUBLIC, null, null));
    }
}
