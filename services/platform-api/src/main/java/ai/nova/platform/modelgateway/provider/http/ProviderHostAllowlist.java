package ai.nova.platform.modelgateway.provider.http;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.web.error.ApiException;

/**
 * SSRF guard: only allowlisted hosts may be contacted. URLs are never taken from clients.
 */
@Component
public class ProviderHostAllowlist {

    public static final String OPENAI_HOST = "api.openai.com";
    private static final Pattern AZURE_OPENAI_HOST =
            Pattern.compile("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\\.openai\\.azure\\.com$");
    private static final Set<String> BLOCKED_LITERALS = Set.of(
            "localhost",
            "127.0.0.1",
            "0.0.0.0",
            "::1",
            "[::1]");

    public void validateAllowlistedHost(String host) {
        if (host == null || host.isBlank()) {
            throw ssrfRejected();
        }
        String normalized = host.trim().toLowerCase(Locale.ROOT);
        if (BLOCKED_LITERALS.contains(normalized) || isPrivateOrLoopbackLiteral(normalized)) {
            throw ssrfRejected();
        }
        if (OPENAI_HOST.equals(normalized) || AZURE_OPENAI_HOST.matcher(normalized).matches()) {
            return;
        }
        throw ssrfRejected();
    }

    public void validateUri(URI uri) {
        if (uri == null || uri.getHost() == null) {
            throw ssrfRejected();
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw ssrfRejected();
        }
        validateAllowlistedHost(uri.getHost());
        try {
            InetAddress[] addresses = InetAddress.getAllByName(uri.getHost());
            for (InetAddress address : addresses) {
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) {
                    throw ssrfRejected();
                }
            }
        } catch (UnknownHostException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PROVIDER_HOST_UNRESOLVABLE", "Provider host could not be resolved");
        }
    }

    public String azureHost(String resourceName) {
        if (resourceName == null || resourceName.isBlank()) {
            throw ssrfRejected();
        }
        String host = resourceName.trim().toLowerCase(Locale.ROOT) + ".openai.azure.com";
        validateAllowlistedHost(host);
        return host;
    }

    private static boolean isPrivateOrLoopbackLiteral(String host) {
        return host.startsWith("10.")
                || host.startsWith("192.168.")
                || host.startsWith("169.254.")
                || host.matches("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")
                || host.endsWith(".local")
                || host.endsWith(".internal");
    }

    private static ApiException ssrfRejected() {
        return new ApiException(HttpStatus.BAD_REQUEST, "PROVIDER_HOST_NOT_ALLOWED", "Provider host is not allowlisted");
    }
}
