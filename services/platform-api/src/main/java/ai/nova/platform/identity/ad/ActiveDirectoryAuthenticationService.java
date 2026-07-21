package ai.nova.platform.identity.ad;

import org.springframework.stereotype.Service;

import ai.nova.platform.identity.provider.AuthenticationCredentials;
import ai.nova.platform.identity.provider.AuthenticationResult;

@Service
public class ActiveDirectoryAuthenticationService {

    public AuthenticationResult authenticate(AuthenticationCredentials credentials, String domain, String ldapUrl) {
        if (ldapUrl == null || ldapUrl.isBlank()) {
            return AuthenticationResult.failure("Active Directory URL must be configured");
        }
        return AuthenticationResult.failure("Active Directory authentication is not configured for this environment");
    }
}
