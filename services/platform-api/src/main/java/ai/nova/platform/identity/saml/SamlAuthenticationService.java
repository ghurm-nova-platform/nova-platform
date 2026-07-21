package ai.nova.platform.identity.saml;

import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class SamlAuthenticationService {

    public Map<String, String> parseMetadata(String metadataXml) {
        if (metadataXml == null || metadataXml.isBlank()) {
            return Map.of();
        }
        return Map.of("entityId", "stub-entity", "ssoUrl", "https://idp.example/saml/sso");
    }

    public Map<String, String> mapAssertionAttributes(Map<String, String> attributes) {
        return attributes == null ? Map.of() : attributes;
    }

    public boolean validateAssertion(String assertionXml, boolean validateSignature) {
        if (!validateSignature) {
            return assertionXml != null && !assertionXml.isBlank();
        }
        return false;
    }

    public String assertionConsumerUrl(java.util.UUID providerId) {
        return "/api/identity/saml/acs?providerId=" + providerId;
    }
}
