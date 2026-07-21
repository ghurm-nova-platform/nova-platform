package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.identity.configuration.IdentityProperties;
import ai.nova.platform.identity.ldap.LdapAuthenticationService;
import ai.nova.platform.identity.ldap.LdapConfigurationService;
import ai.nova.platform.identity.provider.AuthenticationCredentials;
import ai.nova.platform.identity.provider.AuthenticationResult;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest
class LdapAuthenticationServiceTest {

    @Autowired
    private LdapAuthenticationService ldapAuthenticationService;

    @Autowired
    private IdentityProperties identityProperties;

    @Test
    void authenticateFailsWhenLdapDisabled() {
        assertThat(identityProperties.getLdap().isEnabled()).isFalse();

        assertThatThrownBy(() -> ldapAuthenticationService.authenticate(
                        new AuthenticationCredentials("user@nova.local", "secret", null), "ldap://localhost"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("LDAP_DISABLED");
    }

    @Test
    void authenticateFailsWhenEnabledWithoutUrl() {
        IdentityProperties props = new IdentityProperties();
        props.getLdap().setEnabled(true);
        LdapAuthenticationService service = new LdapAuthenticationService(new LdapConfigurationService(props));

        assertThatThrownBy(() -> service.authenticate(
                        new AuthenticationCredentials("user@nova.local", "secret", null), " "))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("LDAP_URL_REQUIRED");
    }

    @Test
    void authenticateReturnsFailureWhenEnabledWithUrlButUnconfigured() {
        IdentityProperties props = new IdentityProperties();
        props.getLdap().setEnabled(true);
        LdapAuthenticationService service = new LdapAuthenticationService(new LdapConfigurationService(props));

        AuthenticationResult result = service.authenticate(
                new AuthenticationCredentials("user@nova.local", "secret", null), "ldap://localhost");

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).containsIgnoringCase("not configured");
    }
}
