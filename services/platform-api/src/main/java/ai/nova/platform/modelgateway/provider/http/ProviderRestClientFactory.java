package ai.nova.platform.modelgateway.provider.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import ai.nova.platform.modelgateway.config.ModelGatewayProperties;
import ai.nova.platform.web.error.ApiException;

@Component
public class ProviderRestClientFactory {

    private final ModelGatewayProperties properties;
    private final ProviderHostAllowlist hostAllowlist;
    private final LocalhostEndpointOverrideGate localhostOverrideGate;

    public ProviderRestClientFactory(
            ModelGatewayProperties properties,
            ProviderHostAllowlist hostAllowlist,
            LocalhostEndpointOverrideGate localhostOverrideGate) {
        this.properties = properties;
        this.hostAllowlist = hostAllowlist;
        this.localhostOverrideGate = localhostOverrideGate;
    }

    public RestClient create(URI baseUri, int timeoutSeconds) {
        validateOrAllowTestOverride(baseUri);
        int boundedTimeout = Math.max(1, Math.min(timeoutSeconds, properties.getMaximumTimeoutSeconds()));
        Duration timeout = Duration.ofSeconds(boundedTimeout);

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);

        return RestClient.builder()
                .baseUrl(normalizeBase(baseUri))
                .requestFactory(requestFactory)
                .build();
    }

    private static String normalizeBase(URI baseUri) {
        String raw = baseUri.toString();
        if (raw.endsWith("/")) {
            return raw.substring(0, raw.length() - 1);
        }
        return raw;
    }

    public URI resolveOpenAiBaseUrl() {
        String override = properties.getProviders().getOpenai().getBaseUrl();
        if (override != null && !override.isBlank()) {
            URI uri = URI.create(override.trim());
            validateOrAllowTestOverride(uri);
            return uri;
        }
        URI production = URI.create("https://" + ProviderHostAllowlist.OPENAI_HOST);
        hostAllowlist.validateUri(production);
        return production;
    }

    public URI resolveAzureBaseUrl(String resourceName) {
        String template = properties.getProviders().getAzureOpenai().getBaseUrlTemplate();
        if (template != null && !template.isBlank()) {
            String filled = template.replace("{resource}", resourceName.trim().toLowerCase(Locale.ROOT));
            URI uri = URI.create(filled);
            validateOrAllowTestOverride(uri);
            return uri;
        }
        String host = hostAllowlist.azureHost(resourceName);
        URI production = URI.create("https://" + host);
        hostAllowlist.validateUri(production);
        return production;
    }

    /**
     * Localhost is allowed only when the active Spring profile supplies a permissive gate
     * ({@code @Profile("test")}). Production has no configuration switch to enable this.
     */
    private void validateOrAllowTestOverride(URI uri) {
        if (isLocalhostOverride(uri)) {
            if (!localhostOverrideGate.allowsLocalhostOverrides()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "PROVIDER_HOST_REJECTED",
                        "Localhost provider endpoints are not allowed");
            }
            return;
        }
        hostAllowlist.validateUri(uri);
    }

    static boolean isLocalhostOverride(URI uri) {
        String host = uri.getHost();
        if (host == null) {
            return false;
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        return ("http".equals(scheme) || "https".equals(scheme))
                && ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host));
    }
}
