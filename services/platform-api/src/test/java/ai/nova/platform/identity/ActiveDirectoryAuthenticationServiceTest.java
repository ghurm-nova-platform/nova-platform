package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.identity.ad.ActiveDirectoryAuthenticationService;
import ai.nova.platform.identity.provider.AuthenticationCredentials;
import ai.nova.platform.identity.provider.AuthenticationResult;

@SpringBootTest
class ActiveDirectoryAuthenticationServiceTest {

    @Autowired
    private ActiveDirectoryAuthenticationService activeDirectoryAuthenticationService;

    @Test
    void authenticateFailsWithoutUrl() {
        AuthenticationResult result = activeDirectoryAuthenticationService.authenticate(
                new AuthenticationCredentials("user@nova.local", "secret", null), "NOVA", " ");

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).containsIgnoringCase("URL");
    }

    @Test
    void authenticateFailsWhenUnconfigured() {
        AuthenticationResult result = activeDirectoryAuthenticationService.authenticate(
                new AuthenticationCredentials("user@nova.local", "secret", null),
                "NOVA",
                "ldap://ad.example.local");

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).containsIgnoringCase("not configured");
    }
}
