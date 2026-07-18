package ai.nova.platform.modelgateway.validation;

import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.modelgateway.entity.AiProviderType;
import ai.nova.platform.web.error.ApiException;

@Component
public class CredentialReferenceValidator {

    private static final Pattern ENV_REFERENCE =
            Pattern.compile("^env:NOVA_PROVIDER_[A-Z0-9_]+$");
    private static final Pattern BEARER = Pattern.compile("(?i)bearer\\s+");
    private static final Pattern PRIVATE_KEY = Pattern.compile("(?i)-----BEGIN (?:RSA )?PRIVATE KEY-----");
    private static final Pattern JSON_SECRET = Pattern.compile("^\\s*\\{.*\"(?:api[_-]?key|secret|token)\".*\\}\\s*$", Pattern.DOTALL);
    private static final Pattern PLAINTEXT_KEY = Pattern.compile("(?i)(?:sk-[a-z0-9]{20,}|api[_-]?key\\s*[:=])");

    public void validate(String credentialReference, AiProviderType providerType) {
        if (providerType == AiProviderType.DETERMINISTIC_LOCAL) {
            if (credentialReference != null && !credentialReference.isBlank()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_CREDENTIAL_REFERENCE",
                        "Deterministic local providers must not have credentials");
            }
            return;
        }
        if (credentialReference == null || credentialReference.isBlank()) {
            return;
        }
        String trimmed = credentialReference.trim();
        rejectSuspicious(trimmed);
        if (!ENV_REFERENCE.matcher(trimmed).matches()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CREDENTIAL_REFERENCE",
                    "Credential reference must match env:NOVA_PROVIDER_<NAME>");
        }
    }

    public void rejectSuspicious(String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (BEARER.matcher(value).find()
                || PRIVATE_KEY.matcher(value).find()
                || JSON_SECRET.matcher(value).find()
                || PLAINTEXT_KEY.matcher(value).find()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CREDENTIAL_REFERENCE",
                    "Plaintext secrets are not allowed");
        }
    }
}
