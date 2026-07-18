package ai.nova.platform.modelgateway.provider.http;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import org.junit.jupiter.api.Test;

import ai.nova.platform.modelgateway.config.ModelGatewayProperties;
import ai.nova.platform.web.error.ApiException;

class ProviderRestClientFactoryLocalhostGuardTest {

    @Test
    void rejectsLocalhostOverrideWhenFlagDisabled() {
        ModelGatewayProperties properties = new ModelGatewayProperties();
        properties.setAllowLocalhostOverrides(false);
        properties.getProviders().getOpenai().setBaseUrl("http://127.0.0.1:18080");
        ProviderRestClientFactory factory = new ProviderRestClientFactory(properties, new ProviderHostAllowlist());

        assertThatThrownBy(factory::resolveOpenAiBaseUrl)
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PROVIDER_HOST_REJECTED");
    }

    @Test
    void allowsLocalhostOverrideOnlyWhenFlagEnabled() {
        ModelGatewayProperties properties = new ModelGatewayProperties();
        properties.setAllowLocalhostOverrides(true);
        properties.getProviders().getOpenai().setBaseUrl("http://127.0.0.1:18080");
        ProviderRestClientFactory factory = new ProviderRestClientFactory(properties, new ProviderHostAllowlist());

        assertThatCode(() -> {
                    URI uri = factory.resolveOpenAiBaseUrl();
                    assertThatCode(() -> factory.create(uri, 5)).doesNotThrowAnyException();
                })
                .doesNotThrowAnyException();
    }
}
