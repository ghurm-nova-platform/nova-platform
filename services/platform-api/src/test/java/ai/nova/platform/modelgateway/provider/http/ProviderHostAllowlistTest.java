package ai.nova.platform.modelgateway.provider.http;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import org.junit.jupiter.api.Test;

import ai.nova.platform.web.error.ApiException;

class ProviderHostAllowlistTest {

    private final ProviderHostAllowlist allowlist = new ProviderHostAllowlist();

    @Test
    void acceptsOpenAiAndAzureHosts() {
        assertThatCode(() -> allowlist.validateAllowlistedHost("api.openai.com")).doesNotThrowAnyException();
        assertThatCode(() -> allowlist.validateAllowlistedHost("myresource.openai.azure.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNonAllowlistedHostsAndArbitraryUrls() {
        assertThatThrownBy(() -> allowlist.validateAllowlistedHost("evil.example.com"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> allowlist.validateAllowlistedHost("127.0.0.1"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> allowlist.validateUri(URI.create("https://evil.example.com/v1")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> allowlist.validateUri(URI.create("http://api.openai.com")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> allowlist.validateUri(URI.create("https://169.254.169.254/")))
                .isInstanceOf(ApiException.class);
    }
}
