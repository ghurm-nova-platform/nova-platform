package ai.nova.platform.identity.ldap;

import org.springframework.stereotype.Service;

import ai.nova.platform.identity.provider.AuthenticationCredentials;
import ai.nova.platform.identity.provider.AuthenticationResult;

@Service
public class LdapAuthenticationService {

    private final LdapConfigurationService ldapConfigurationService;

    public LdapAuthenticationService(LdapConfigurationService ldapConfigurationService) {
        this.ldapConfigurationService = ldapConfigurationService;
    }

    public AuthenticationResult authenticate(AuthenticationCredentials credentials, String ldapUrl) {
        ldapConfigurationService.requireEnabled();
        ldapConfigurationService.requireUrl(ldapUrl);
        return AuthenticationResult.failure("LDAP authentication is not configured for this environment");
    }
}
