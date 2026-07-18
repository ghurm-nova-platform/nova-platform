package ai.nova.platform.modelgateway.provider.http;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import ai.nova.platform.modelgateway.config.ModelGatewayProperties;
import ai.nova.platform.web.error.ApiException;

class ProviderRestClientFactoryLocalhostGuardTest {

    @Test
    void nonTestGateAlwaysRejectsLocalhostRegardlessOfBaseUrlSetting() {
        ModelGatewayProperties properties = new ModelGatewayProperties();
        properties.getProviders().getOpenai().setBaseUrl("http://127.0.0.1:18080");
        ProviderRestClientFactory factory = new ProviderRestClientFactory(
                properties, new ProviderHostAllowlist(), new DenyLocalhostEndpointOverrideGate());

        assertThatThrownBy(factory::resolveOpenAiBaseUrl)
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PROVIDER_HOST_REJECTED");
    }

    @Test
    void testGateAllowsLocalhostForMockWebServer() {
        ModelGatewayProperties properties = new ModelGatewayProperties();
        properties.getProviders().getOpenai().setBaseUrl("http://127.0.0.1:18080");
        ProviderRestClientFactory factory = new ProviderRestClientFactory(
                properties, new ProviderHostAllowlist(), new TestLocalhostEndpointOverrideGate());

        assertThatCode(() -> factory.create(factory.resolveOpenAiBaseUrl(), 5)).doesNotThrowAnyException();
    }
}
