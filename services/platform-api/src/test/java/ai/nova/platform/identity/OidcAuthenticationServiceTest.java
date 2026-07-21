package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.identity.oidc.OidcAuthenticationService;

@SpringBootTest
class OidcAuthenticationServiceTest {

    @Autowired
    private OidcAuthenticationService oidcAuthenticationService;

    @Test
    void stubDiscoveryAndClaimValidation() {
        Map<String, Object> discovery = oidcAuthenticationService.fetchDiscoveryDocument(null);
        assertThat(discovery).containsKeys("issuer", "authorization_endpoint", "token_endpoint");

        assertThat(oidcAuthenticationService.validateIdTokenClaims(Map.of("sub", "user-1"))).isTrue();
        assertThat(oidcAuthenticationService.validateIdTokenClaims(Map.of())).isFalse();

        UUID providerId = UUID.randomUUID();
        assertThat(oidcAuthenticationService.callbackUrl(providerId, "code", "state"))
                .contains(providerId.toString())
                .contains("code=code");
    }
}
