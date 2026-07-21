package ai.nova.platform.identity.oauth2;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class OAuth2AuthenticationService {

    private final Map<String, String> pkceStateStore = new ConcurrentHashMap<>();

    public String storeAuthorizationState(String state, String codeVerifier) {
        pkceStateStore.put(state, codeVerifier);
        return state;
    }

    public String beginAuthorizationCodeFlow(UUID providerId) {
        String state = UUID.randomUUID().toString();
        storeAuthorizationState(state, UUID.randomUUID().toString());
        return "/api/identity/oauth2/callback?providerId=" + providerId + "&state=" + state + "&code=stub";
    }

    public boolean handleCallback(String state, String code) {
        return pkceStateStore.containsKey(state) && code != null;
    }

    public String clientCredentialsToken(UUID serviceAccountId) {
        return "stub-access-token-" + serviceAccountId;
    }
}
