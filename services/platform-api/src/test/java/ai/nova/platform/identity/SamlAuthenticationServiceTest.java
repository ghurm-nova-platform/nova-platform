package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.identity.saml.SamlAuthenticationService;

@SpringBootTest
class SamlAuthenticationServiceTest {

    @Autowired
    private SamlAuthenticationService samlAuthenticationService;

    @Test
    void stubMetadataAssertionAndAcs() {
        assertThat(samlAuthenticationService.parseMetadata(null)).isEmpty();
        assertThat(samlAuthenticationService.parseMetadata("<EntityDescriptor/>"))
                .containsKeys("entityId", "ssoUrl");

        Map<String, String> attrs = Map.of("email", "user@nova.local");
        assertThat(samlAuthenticationService.mapAssertionAttributes(attrs)).isEqualTo(attrs);

        assertThat(samlAuthenticationService.validateAssertion("<Assertion/>", false)).isTrue();
        assertThat(samlAuthenticationService.validateAssertion("<Assertion/>", true)).isFalse();

        UUID providerId = UUID.randomUUID();
        assertThat(samlAuthenticationService.assertionConsumerUrl(providerId)).contains(providerId.toString());
    }
}
