package ai.nova.platform.deploymentexecution.provider;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ai.nova.platform.deploymentexecution.entity.ExecutionProviderCode;

@Component
public class DeploymentExecutionProviderRegistry {

    private final Map<ExecutionProviderCode, DeploymentExecutionProvider> providers;

    public DeploymentExecutionProviderRegistry(List<DeploymentExecutionProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(DeploymentExecutionProvider::providerCode, Function.identity()));
    }

    public DeploymentExecutionProvider require(ExecutionProviderCode code) {
        DeploymentExecutionProvider provider = providers.get(code);
        if (provider == null) {
            throw new IllegalStateException("No deployment execution provider registered for " + code);
        }
        return provider;
    }

    public ExecutionProviderCode resolveConfigured(String configuredProvider, ExecutionProviderCode requested) {
        ExecutionProviderCode fallback = parseCode(configuredProvider);
        return requested != null ? requested : fallback;
    }

    public static ExecutionProviderCode parseCode(String value) {
        if (value == null || value.isBlank()) {
            return ExecutionProviderCode.LOCAL;
        }
        return ExecutionProviderCode.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
