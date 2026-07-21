package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.identity.oauth2.OAuth2AuthenticationService;

@SpringBootTest
class OAuth2AuthenticationServiceTest {

    @Autowired
    private OAuth2AuthenticationService oAuth2AuthenticationService;

    @Test
    void authorizationFlowSmoke() {
        UUID providerId = UUID.randomUUID();
        String redirect = oAuth2AuthenticationService.beginAuthorizationCodeFlow(providerId);
        assertThat(redirect).contains(providerId.toString()).contains("state=");

        String state = redirect.replaceAll(".*state=([^&]+).*", "$1");
        assertThat(oAuth2AuthenticationService.handleCallback(state, "stub-code")).isTrue();
        assertThat(oAuth2AuthenticationService.handleCallback("unknown", "stub-code")).isFalse();

        UUID serviceAccountId = UUID.randomUUID();
        assertThat(oAuth2AuthenticationService.clientCredentialsToken(serviceAccountId))
                .contains(serviceAccountId.toString());
    }
}
