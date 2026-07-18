package ai.nova.platform.modelgateway.provider.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import ai.nova.platform.modelgateway.config.ModelGatewayProperties;

@Component
public class ProviderRestClientFactory {

    private final ModelGatewayProperties properties;
    private final ProviderHostAllowlist hostAllowlist;

    public ProviderRestClientFactory(ModelGatewayProperties properties, ProviderHostAllowlist hostAllowlist) {
        this.properties = properties;
        this.hostAllowlist = hostAllowlist;
    }

    public RestClient create(URI baseUri, int timeoutSeconds) {
        if (!isTestOverride(baseUri)) {
            hostAllowlist.validateUri(baseUri);
        }
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
            // Test-only override may point at MockWebServer (http localhost). Production path uses allowlist.
            if (isTestOverride(uri)) {
                return uri;
            }
            hostAllowlist.validateUri(uri);
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
            if (isTestOverride(uri)) {
                return uri;
            }
            hostAllowlist.validateUri(uri);
            return uri;
        }
        String host = hostAllowlist.azureHost(resourceName);
        URI production = URI.create("https://" + host);
        hostAllowlist.validateUri(production);
        return production;
    }

    private static boolean isTestOverride(URI uri) {
        String host = uri.getHost();
        if (host == null) {
            return false;
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        return ("http".equals(scheme) || "https".equals(scheme))
                && ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host));
    }
}
