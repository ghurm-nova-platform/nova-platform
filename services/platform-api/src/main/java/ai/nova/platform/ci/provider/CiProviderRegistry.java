package ai.nova.platform.ci.provider;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.web.error.ApiException;

@Component
public class CiProviderRegistry {

    private final Map<String, CiProvider> providersById;

    public CiProviderRegistry(List<CiProvider> providers) {
        Map<String, CiProvider> map = new HashMap<>();
        for (CiProvider provider : providers) {
            map.put(normalize(provider.providerId()), provider);
        }
        this.providersById = Map.copyOf(map);
    }

    public CiProvider requireProvider(String providerId) {
        CiProvider provider = providersById.get(normalize(providerId));
        if (provider == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "CI_PROVIDER_UNSUPPORTED", "Unsupported CI provider: " + providerId);
        }
        return provider;
    }

    public boolean supports(String providerId) {
        return providersById.containsKey(normalize(providerId));
    }

    private static String normalize(String providerId) {
        return providerId == null ? "" : providerId.trim().toUpperCase(Locale.ROOT);
    }
}
