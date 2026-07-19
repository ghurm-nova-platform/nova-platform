package ai.nova.platform.pullrequest.provider;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.web.error.ApiException;

@Component
public class PullRequestProviderRegistry {

    private final Map<String, PullRequestProvider> providersById;

    public PullRequestProviderRegistry(List<PullRequestProvider> providers) {
        Map<String, PullRequestProvider> map = new HashMap<>();
        for (PullRequestProvider provider : providers) {
            map.put(normalize(provider.providerId()), provider);
        }
        this.providersById = Map.copyOf(map);
    }

    public PullRequestProvider requireProvider(String providerId) {
        PullRequestProvider provider = providersById.get(normalize(providerId));
        if (provider == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "PR_PROVIDER_UNSUPPORTED", "Unsupported pull request provider: " + providerId);
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
