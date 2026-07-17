package ai.nova.platform.agent.validation;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import ai.nova.platform.web.error.ApiException;

@ConfigurationProperties(prefix = "nova.agents")
@Component
public class ModelProviderAllowlist {

    private List<String> allowedProviders = List.of(
            "OPENAI",
            "ANTHROPIC",
            "GOOGLE",
            "AZURE_OPENAI",
            "LOCAL");

    public List<String> getAllowedProviders() {
        return allowedProviders;
    }

    public void setAllowedProviders(List<String> allowedProviders) {
        this.allowedProviders = allowedProviders;
    }

    public String requireAllowed(String provider) {
        if (!StringUtils.hasText(provider)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MODEL_PROVIDER", "Model provider is required");
        }
        String normalized = provider.trim().toUpperCase(Locale.ROOT);
        Set<String> allowed = allowedProviders.stream()
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        if (!allowed.contains(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MODEL_PROVIDER", "Unknown model provider");
        }
        return normalized;
    }
}
