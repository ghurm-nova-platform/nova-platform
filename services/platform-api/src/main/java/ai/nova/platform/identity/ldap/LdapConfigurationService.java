package ai.nova.platform.identity.ldap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.identity.configuration.IdentityProperties;
import ai.nova.platform.web.error.ApiException;

@Service
public class LdapConfigurationService {

    private final IdentityProperties properties;

    public LdapConfigurationService(IdentityProperties properties) {
        this.properties = properties;
    }

    public void requireEnabled() {
        if (!properties.getLdap().isEnabled()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LDAP_DISABLED", "LDAP integration is disabled");
        }
    }

    public void requireUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LDAP_URL_REQUIRED", "LDAP server URL must be configured");
        }
    }
}
