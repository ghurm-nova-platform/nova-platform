package ai.nova.platform.modelgateway.provider.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

import ai.nova.platform.modelgateway.provider.ProviderException;
import ai.nova.platform.modelgateway.provider.ProviderFailureKind;

class UnifiedProviderErrorMapperTest {

    private final UnifiedProviderErrorMapper mapper = new UnifiedProviderErrorMapper();

    @Test
    void mapsHttpStatuses() {
        assertMapped(401, "PROVIDER_AUTHENTICATION_FAILED", ProviderFailureKind.PERMANENT);
        assertMapped(403, "PROVIDER_PERMISSION_DENIED", ProviderFailureKind.PERMANENT);
        assertMapped(429, "PROVIDER_RATE_LIMITED", ProviderFailureKind.TRANSIENT);
        assertMapped(408, "PROVIDER_TIMEOUT", ProviderFailureKind.TRANSIENT);
        assertMapped(504, "PROVIDER_TIMEOUT", ProviderFailureKind.TRANSIENT);
        assertMapped(503, "PROVIDER_UNAVAILABLE", ProviderFailureKind.TRANSIENT);
    }

    @Test
    void mapsContextLimitFromBodyHintWithoutExposingBody() {
        RestClientResponseException ex = new RestClientResponseException(
                "bad",
                400,
                "Bad Request",
                null,
                "{\"error\":{\"code\":\"context_length_exceeded\",\"message\":\"too long\"}}".getBytes(),
                null);
        ProviderException mapped = mapper.map(ex);
        assertThat(mapped.errorCode()).isEqualTo("CONTEXT_LIMIT_EXCEEDED");
        assertThat(mapped.getMessage()).doesNotContain("too long");
        assertThat(mapped.failureKind()).isEqualTo(ProviderFailureKind.PERMANENT);
    }

    private void assertMapped(int status, String code, ProviderFailureKind kind) {
        ProviderException mapped = mapper.map(HttpStatus.valueOf(status), null);
        assertThat(mapped.errorCode()).isEqualTo(code);
        assertThat(mapped.failureKind()).isEqualTo(kind);
    }
}
