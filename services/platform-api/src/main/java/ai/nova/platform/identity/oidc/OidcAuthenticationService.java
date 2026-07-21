package ai.nova.platform.identity.oidc;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import ai.nova.platform.identity.configuration.IdentityProperties;

@Service
public class OidcAuthenticationService {

    private final IdentityProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public OidcAuthenticationService(IdentityProperties properties) {
        this.properties = properties;
    }

    public Map<String, Object> fetchDiscoveryDocument(String issuerUrl) {
        if (issuerUrl == null || issuerUrl.isBlank()) {
            return Map.of("issuer", "stub", "authorization_endpoint", "stub", "token_endpoint", "stub");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(issuerUrl.replaceAll("/$", "") + "/.well-known/openid-configuration"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return Map.of("issuer", issuerUrl);
            }
            return Map.of("issuer", issuerUrl, "raw", response.body());
        } catch (Exception ex) {
            return Map.of("issuer", issuerUrl, "error", ex.getMessage());
        }
    }

    public boolean validateIdTokenClaims(Map<String, Object> claims) {
        if (properties.getOidc().isSkipJwksVerify()) {
            return claims != null && claims.containsKey("sub");
        }
        return claims != null && claims.containsKey("sub") && claims.containsKey("iss");
    }

    public String callbackUrl(UUID providerId, String code, String state) {
        return "/api/identity/oidc/callback?providerId=" + providerId + "&code=" + code + "&state=" + state;
    }
}
