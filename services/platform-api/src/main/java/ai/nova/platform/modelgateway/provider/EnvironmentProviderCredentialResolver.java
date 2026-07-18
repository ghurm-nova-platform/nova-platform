package ai.nova.platform.modelgateway.provider;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import ai.nova.platform.modelgateway.validation.CredentialReferenceValidator;

@Component
public class EnvironmentProviderCredentialResolver implements ProviderCredentialResolver {

    private static final Pattern ENV_REFERENCE =
            Pattern.compile("^env:(NOVA_PROVIDER_[A-Z0-9_]+)$");

    private final CredentialReferenceValidator credentialReferenceValidator;

    public EnvironmentProviderCredentialResolver(CredentialReferenceValidator credentialReferenceValidator) {
        this.credentialReferenceValidator = credentialReferenceValidator;
    }

    @Override
    public Optional<String> resolve(String credentialReference, UUID organizationId) {
        if (credentialReference == null || credentialReference.isBlank()) {
            return Optional.empty();
        }
        credentialReferenceValidator.rejectSuspicious(credentialReference);
        Matcher matcher = ENV_REFERENCE.matcher(credentialReference.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String envName = matcher.group(1);
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        credentialReferenceValidator.rejectSuspicious(value);
        return Optional.of(value);
    }
}
