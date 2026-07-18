package ai.nova.platform.modelgateway.secrets.vault;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import ai.nova.platform.modelgateway.entity.AiProviderType;

public final class ProviderSecretDtos {

    private ProviderSecretDtos() {
    }

    public record CreateProviderSecretRequest(
            @NotBlank String secretKey,
            @NotBlank String name,
            String description,
            @NotNull AiProviderType providerType,
            @NotBlank String secret) {
    }

    public record RotateProviderSecretRequest(@NotBlank String secret) {
    }

    public record ProviderSecretResponse(
            UUID id,
            String secretKey,
            String name,
            String description,
            AiProviderType providerType,
            ProviderSecretStatus status,
            String credentialReference,
            String algorithm,
            Integer keyVersion,
            String fingerprintSha256,
            String last4,
            Integer version,
            Instant createdAt,
            Instant updatedAt,
            Instant rotatedAt,
            Instant revokedAt) {
    }
}
