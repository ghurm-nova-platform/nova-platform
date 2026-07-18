package ai.nova.platform.modelgateway.provider.error;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import ai.nova.platform.modelgateway.provider.ProviderException;
import ai.nova.platform.modelgateway.provider.ProviderFailureKind;

/**
 * Maps provider HTTP failures to stable platform error codes. Never includes raw provider bodies.
 */
@Component
public class UnifiedProviderErrorMapper {

    public ProviderException map(RestClientResponseException ex) {
        return map(ex.getStatusCode(), safeBodyHint(ex));
    }

    public ProviderException map(HttpStatusCode status, String bodyHint) {
        int code = status != null ? status.value() : 500;
        if (code == 401) {
            return permanent("PROVIDER_AUTHENTICATION_FAILED", "Provider authentication failed");
        }
        if (code == 403) {
            return permanent("PROVIDER_PERMISSION_DENIED", "Provider permission denied");
        }
        if (code == 429) {
            return transientFailure("PROVIDER_RATE_LIMITED", "Provider rate limited");
        }
        if (code == 408 || code == 504) {
            return transientFailure("PROVIDER_TIMEOUT", "Provider timed out");
        }
        if (code == 400 && looksLikeContextLimit(bodyHint)) {
            return permanent("CONTEXT_LIMIT_EXCEEDED", "Context limit exceeded");
        }
        if (code >= 500) {
            return transientFailure("PROVIDER_UNAVAILABLE", "Provider temporarily unavailable");
        }
        if (code >= 400) {
            return permanent("PROVIDER_ERROR", "Provider rejected the request");
        }
        return transientFailure("PROVIDER_UNAVAILABLE", "Provider temporarily unavailable");
    }

    public ProviderException mapTransport(Exception ex) {
        if (ex instanceof java.net.http.HttpTimeoutException
                || ex instanceof java.util.concurrent.TimeoutException
                || containsTimeout(ex)) {
            return transientFailure("PROVIDER_TIMEOUT", "Provider timed out");
        }
        return transientFailure("PROVIDER_UNAVAILABLE", "Provider temporarily unavailable");
    }

    private static boolean looksLikeContextLimit(String bodyHint) {
        if (bodyHint == null || bodyHint.isBlank()) {
            return false;
        }
        String lower = bodyHint.toLowerCase();
        return lower.contains("context_length")
                || lower.contains("context length")
                || lower.contains("maximum context")
                || lower.contains("token limit")
                || lower.contains("too many tokens");
    }

    private static String safeBodyHint(RestClientResponseException ex) {
        try {
            String body = ex.getResponseBodyAsString();
            if (body == null || body.isBlank()) {
                return null;
            }
            // Keep only a short lowercase scan window; never log or rethrow raw body.
            return body.length() > 500 ? body.substring(0, 500) : body;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean containsTimeout(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String name = current.getClass().getSimpleName().toLowerCase();
            String message = current.getMessage() == null ? "" : current.getMessage().toLowerCase();
            if (name.contains("timeout") || message.contains("timed out") || message.contains("timeout")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static ProviderException permanent(String code, String message) {
        return new ProviderException(code, ProviderFailureKind.PERMANENT, message);
    }

    private static ProviderException transientFailure(String code, String message) {
        return new ProviderException(code, ProviderFailureKind.TRANSIENT, message);
    }
}
