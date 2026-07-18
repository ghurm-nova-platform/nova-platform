package ai.nova.platform.modelgateway.secrets.vault;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.modelgateway.entity.AiProvider;
import ai.nova.platform.modelgateway.entity.AiProviderType;
import ai.nova.platform.modelgateway.entity.ConnectionTestStatus;
import ai.nova.platform.modelgateway.repository.AiProviderRepository;
import ai.nova.platform.modelgateway.secrets.vault.ProviderSecretDtos.CreateProviderSecretRequest;
import ai.nova.platform.modelgateway.secrets.vault.ProviderSecretDtos.ProviderSecretResponse;
import ai.nova.platform.modelgateway.secrets.vault.ProviderSecretDtos.RotateProviderSecretRequest;
import ai.nova.platform.modelgateway.security.ModelGatewayAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class ProviderSecretService {

    public static final String CREDENTIAL_REFERENCE_PREFIX = "vault:provider-secret:";
    private static final Pattern SECRET_KEY_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{0,99}$");
    private static final int SECRET_KEY_MAX_LENGTH = 100;

    private final ProviderSecretRepository secretRepository;
    private final AiProviderRepository providerRepository;
    private final SecretEncryptionService encryptionService;
    private final ModelGatewayAuthorizationService authorizationService;
    private final ProviderSecretAuditService auditService;

    public ProviderSecretService(
            ProviderSecretRepository secretRepository,
            AiProviderRepository providerRepository,
            SecretEncryptionService encryptionService,
            ModelGatewayAuthorizationService authorizationService,
            ProviderSecretAuditService auditService) {
        this.secretRepository = secretRepository;
        this.providerRepository = providerRepository;
        this.encryptionService = encryptionService;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<ProviderSecretResponse> list(
            ProviderSecretStatus status,
            AiProviderType providerType,
            String search,
            Pageable pageable,
            AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.PROVIDER_SECRET_READ);
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return secretRepository
                .search(user.getOrganizationId(), status, providerType, normalizedSearch, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProviderSecretResponse get(UUID secretId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.PROVIDER_SECRET_READ);
        return toResponse(requireSecret(secretId, user.getOrganizationId()));
    }

    @Transactional
    public ProviderSecretResponse create(CreateProviderSecretRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.PROVIDER_SECRET_CREATE);
        if (request.providerType() == AiProviderType.DETERMINISTIC_LOCAL) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PROVIDER_TYPE_INVALID",
                    "Deterministic local providers cannot store secrets");
        }
        String key = request.secretKey().trim().toUpperCase();
        if (key.length() > SECRET_KEY_MAX_LENGTH || !SECRET_KEY_PATTERN.matcher(key).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SECRET_KEY_INVALID", "Invalid secret key");
        }
        if (secretRepository.existsByOrganizationIdAndSecretKey(user.getOrganizationId(), key)) {
            throw new ApiException(HttpStatus.CONFLICT, "SECRET_KEY_EXISTS", "Secret key already exists");
        }

        String plaintext = request.secret();
        EncryptedSecretPayload encrypted = encryptionService.encrypt(plaintext);
        Instant now = Instant.now();

        ProviderSecret secret = newProviderSecret(
                user.getOrganizationId(),
                key,
                request.name().trim(),
                trimToNull(request.description()),
                request.providerType(),
                encrypted,
                plaintext,
                user.getUserId(),
                now);
        secretRepository.save(secret);
        auditService.secretCreated(user.getOrganizationId(), secret.getId(), user.getUserId());
        return toResponse(secret);
    }

    /**
     * Creates a new ACTIVE secret and marks the previous row ROTATED.
     * Providers referencing the old vault UUID are remapped to the new credential reference.
     */
    @Transactional
    public ProviderSecretResponse rotate(
            UUID secretId, RotateProviderSecretRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.PROVIDER_SECRET_ROTATE);
        ProviderSecret previous = requireSecret(secretId, user.getOrganizationId());
        if (previous.getStatus() != ProviderSecretStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "SECRET_NOT_ACTIVE", "Only active secrets can be rotated");
        }

        String originalKey = previous.getSecretKey();
        Instant now = Instant.now();
        String retiredKey = retiredSecretKey(originalKey, previous.getId());
        previous.setSecretKey(retiredKey);
        previous.setStatus(ProviderSecretStatus.ROTATED);
        previous.setRotatedAt(now);
        previous.setUpdatedBy(user.getUserId());
        previous.setUpdatedAt(now);
        secretRepository.saveAndFlush(previous);

        String plaintext = request.secret();
        EncryptedSecretPayload encrypted = encryptionService.encrypt(plaintext);
        ProviderSecret replacement = newProviderSecret(
                previous.getOrganizationId(),
                originalKey,
                previous.getName(),
                previous.getDescription(),
                previous.getProviderType(),
                encrypted,
                plaintext,
                user.getUserId(),
                now);
        secretRepository.save(replacement);

        String oldRef = toCredentialReference(previous.getId());
        String newRef = toCredentialReference(replacement.getId());
        List<AiProvider> linked = providerRepository.findByOrganizationIdAndCredentialReference(
                previous.getOrganizationId(), oldRef);
        for (AiProvider provider : linked) {
            provider.setCredentialReference(newRef);
            provider.setLastConnectionTestStatus(ConnectionTestStatus.NEVER);
            provider.setLastConnectionTestAt(null);
            provider.setLastConnectionTestErrorCode(null);
            provider.setUpdatedBy(user.getUserId());
            provider.setUpdatedAt(now);
        }
        if (!linked.isEmpty()) {
            providerRepository.saveAll(linked);
        }

        auditService.secretRotated(user.getOrganizationId(), previous.getId(), user.getUserId());
        auditService.secretCreated(user.getOrganizationId(), replacement.getId(), user.getUserId());
        return toResponse(replacement);
    }

    @Transactional
    public ProviderSecretResponse revoke(UUID secretId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.PROVIDER_SECRET_REVOKE);
        ProviderSecret secret = requireSecret(secretId, user.getOrganizationId());
        if (secret.getStatus() == ProviderSecretStatus.REVOKED) {
            return toResponse(secret);
        }
        Instant now = Instant.now();
        secret.setStatus(ProviderSecretStatus.REVOKED);
        secret.setRevokedAt(now);
        secret.setUpdatedBy(user.getUserId());
        secret.setUpdatedAt(now);
        secretRepository.save(secret);
        auditService.secretRevoked(user.getOrganizationId(), secret.getId(), user.getUserId());
        return toResponse(secret);
    }

    @Transactional(readOnly = true)
    public Optional<String> resolveActiveSecret(UUID secretId, UUID organizationId) {
        return secretRepository
                .findByIdAndOrganizationId(secretId, organizationId)
                .filter(secret -> secret.getStatus() == ProviderSecretStatus.ACTIVE)
                .map(secret -> encryptionService.decrypt(
                        secret.getCiphertext(), secret.getNonce(), secret.getKeyVersion()));
    }

    public static String toCredentialReference(UUID secretId) {
        return CREDENTIAL_REFERENCE_PREFIX + secretId;
    }

    public static Optional<UUID> parseVaultSecretId(String credentialReference) {
        if (credentialReference == null || credentialReference.isBlank()) {
            return Optional.empty();
        }
        String trimmed = credentialReference.trim();
        if (!trimmed.startsWith(CREDENTIAL_REFERENCE_PREFIX)) {
            return Optional.empty();
        }
        String idPart = trimmed.substring(CREDENTIAL_REFERENCE_PREFIX.length());
        try {
            return Optional.of(UUID.fromString(idPart));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private ProviderSecret newProviderSecret(
            UUID organizationId,
            String key,
            String name,
            String description,
            AiProviderType providerType,
            EncryptedSecretPayload encrypted,
            String plaintext,
            UUID actorId,
            Instant now) {
        ProviderSecret secret = new ProviderSecret();
        secret.setId(UUID.randomUUID());
        secret.setOrganizationId(organizationId);
        secret.setSecretKey(key);
        secret.setName(name);
        secret.setDescription(description);
        secret.setProviderType(providerType);
        secret.setStatus(ProviderSecretStatus.ACTIVE);
        secret.setCiphertext(encrypted.ciphertext());
        secret.setNonce(encrypted.nonce());
        secret.setKeyVersion(encrypted.keyVersion());
        secret.setAlgorithm(encrypted.algorithm());
        // Internal only — never returned via API (use HMAC with master material for stored fingerprint).
        secret.setFingerprintSha256(encryptionService.internalFingerprint(plaintext));
        secret.setLast4(encryptionService.last4(plaintext));
        secret.setCreatedBy(actorId);
        secret.setUpdatedBy(actorId);
        secret.setCreatedAt(now);
        secret.setUpdatedAt(now);
        return secret;
    }

    /**
     * Builds a retired key that always fits VARCHAR(100): prefix truncated if needed before
     * {@code __ROTATED_<8hex>}.
     */
    static String retiredSecretKey(String originalKey, UUID previousId) {
        String idHex = previousId.toString().replace("-", "").substring(0, 8).toUpperCase();
        String suffix = "__ROTATED_" + idHex;
        if (originalKey.length() + suffix.length() <= SECRET_KEY_MAX_LENGTH) {
            return originalKey + suffix;
        }
        int keep = SECRET_KEY_MAX_LENGTH - suffix.length();
        if (keep < 1) {
            return suffix.substring(0, SECRET_KEY_MAX_LENGTH);
        }
        return originalKey.substring(0, keep) + suffix;
    }

    private ProviderSecret requireSecret(UUID secretId, UUID organizationId) {
        return secretRepository
                .findByIdAndOrganizationId(secretId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SECRET_NOT_FOUND", "Secret not found"));
    }

    private ProviderSecretResponse toResponse(ProviderSecret secret) {
        return new ProviderSecretResponse(
                secret.getId(),
                secret.getSecretKey(),
                secret.getName(),
                secret.getDescription(),
                secret.getProviderType(),
                secret.getStatus(),
                toCredentialReference(secret.getId()),
                secret.getAlgorithm(),
                secret.getKeyVersion(),
                secret.getLast4(),
                secret.getVersion(),
                secret.getCreatedAt(),
                secret.getUpdatedAt(),
                secret.getRotatedAt(),
                secret.getRevokedAt());
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
