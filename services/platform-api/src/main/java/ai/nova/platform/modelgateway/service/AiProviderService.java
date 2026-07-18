package ai.nova.platform.modelgateway.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.modelgateway.audit.ModelGatewayAuditService;
import ai.nova.platform.modelgateway.config.ModelGatewayProperties;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.CreateProviderRequest;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.ProviderAdapterResponse;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.ProviderAdaptersResponse;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.ProviderResponse;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.UpdateProviderRequest;
import ai.nova.platform.modelgateway.entity.AiProvider;
import ai.nova.platform.modelgateway.entity.AiProviderStatus;
import ai.nova.platform.modelgateway.mapper.ModelGatewayMapper;
import ai.nova.platform.modelgateway.provider.AiModelProvider;
import ai.nova.platform.modelgateway.provider.AiModelProviderRegistry;
import ai.nova.platform.modelgateway.repository.AiProviderRepository;
import ai.nova.platform.modelgateway.security.ModelGatewayAuthorizationService;
import ai.nova.platform.modelgateway.validation.CredentialReferenceValidator;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class AiProviderService {

    private static final Pattern PROVIDER_KEY_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    private final AiProviderRepository providerRepository;
    private final AiModelProviderRegistry providerRegistry;
    private final ModelGatewayProperties properties;
    private final ModelGatewayMapper mapper;
    private final ModelGatewayAuthorizationService authorizationService;
    private final CredentialReferenceValidator credentialReferenceValidator;
    private final ModelGatewayAuditService auditService;

    public AiProviderService(
            AiProviderRepository providerRepository,
            AiModelProviderRegistry providerRegistry,
            ModelGatewayProperties properties,
            ModelGatewayMapper mapper,
            ModelGatewayAuthorizationService authorizationService,
            CredentialReferenceValidator credentialReferenceValidator,
            ModelGatewayAuditService auditService) {
        this.providerRepository = providerRepository;
        this.providerRegistry = providerRegistry;
        this.properties = properties;
        this.mapper = mapper;
        this.authorizationService = authorizationService;
        this.credentialReferenceValidator = credentialReferenceValidator;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<ProviderResponse> list(AiProviderStatus status, String search, Pageable pageable, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_PROVIDER_READ);
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return providerRepository
                .search(user.getOrganizationId(), status, normalizedSearch, pageable)
                .map(mapper::toProviderResponse);
    }

    @Transactional(readOnly = true)
    public ProviderResponse get(UUID providerId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_PROVIDER_READ);
        return mapper.toProviderResponse(requireProvider(providerId, user.getOrganizationId()));
    }

    @Transactional(readOnly = true)
    public ProviderAdaptersResponse listAdapters(AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_PROVIDER_READ);
        List<ProviderAdapterResponse> adapters = providerRegistry.list().stream()
                .map(this::toAdapterResponse)
                .toList();
        return new ProviderAdaptersResponse(adapters);
    }

    @Transactional
    public ProviderResponse create(CreateProviderRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_PROVIDER_CREATE);
        String key = request.providerKey().trim().toUpperCase();
        if (!PROVIDER_KEY_PATTERN.matcher(key).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PROVIDER_KEY_INVALID", "Invalid provider key");
        }
        if (providerRepository.existsByOrganizationIdAndProviderKey(user.getOrganizationId(), key)) {
            throw new ApiException(HttpStatus.CONFLICT, "PROVIDER_KEY_EXISTS", "Provider key already exists");
        }
        if (!providerRegistry.isRegistered(request.adapterKey())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ADAPTER_NOT_REGISTERED", "Adapter not registered");
        }
        credentialReferenceValidator.validate(request.credentialReference(), request.providerType());

        Instant now = Instant.now();
        AiProvider provider = new AiProvider();
        provider.setId(UUID.randomUUID());
        provider.setOrganizationId(user.getOrganizationId());
        provider.setProviderKey(key);
        provider.setName(request.name().trim());
        provider.setDescription(trimToNull(request.description()));
        provider.setProviderType(request.providerType());
        provider.setAdapterKey(request.adapterKey().trim());
        provider.setCredentialReference(trimToNull(request.credentialReference()));
        provider.setRegion(trimToNull(request.region()));
        provider.setStatus(AiProviderStatus.DRAFT);
        provider.setRequestTimeoutSeconds(
                request.requestTimeoutSeconds() != null
                        ? request.requestTimeoutSeconds()
                        : properties.getDefaultTimeoutSeconds());
        provider.setMaxConcurrentRequests(
                request.maxConcurrentRequests() != null
                        ? request.maxConcurrentRequests()
                        : properties.getMaxConcurrentRequestsPerProvider());
        provider.setMaxRetries(request.maxRetries() != null ? request.maxRetries() : 1);
        provider.setRetryBackoffMs(request.retryBackoffMs() != null ? request.retryBackoffMs() : 250);
        provider.setCreatedBy(user.getUserId());
        provider.setUpdatedBy(user.getUserId());
        provider.setCreatedAt(now);
        provider.setUpdatedAt(now);
        provider.setVersion(0);
        providerRepository.save(provider);
        auditService.providerCreated(user.getOrganizationId(), provider.getId(), user.getUserId());
        return mapper.toProviderResponse(provider);
    }

    @Transactional
    public ProviderResponse update(UUID providerId, UpdateProviderRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_PROVIDER_UPDATE);
        AiProvider provider = requireProvider(providerId, user.getOrganizationId());
        if (!provider.getVersion().equals(request.version())) {
            throw optimisticLockFailure();
        }
        credentialReferenceValidator.validate(request.credentialReference(), provider.getProviderType());
        provider.setName(request.name().trim());
        provider.setDescription(trimToNull(request.description()));
        provider.setCredentialReference(trimToNull(request.credentialReference()));
        provider.setRegion(trimToNull(request.region()));
        if (request.requestTimeoutSeconds() != null) {
            provider.setRequestTimeoutSeconds(request.requestTimeoutSeconds());
        }
        if (request.maxConcurrentRequests() != null) {
            provider.setMaxConcurrentRequests(request.maxConcurrentRequests());
        }
        if (request.maxRetries() != null) {
            provider.setMaxRetries(request.maxRetries());
        }
        if (request.retryBackoffMs() != null) {
            provider.setRetryBackoffMs(request.retryBackoffMs());
        }
        provider.setUpdatedBy(user.getUserId());
        provider.setUpdatedAt(Instant.now());
        return mapper.toProviderResponse(providerRepository.save(provider));
    }

    @Transactional
    public ProviderResponse activate(UUID providerId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_PROVIDER_ACTIVATE);
        AiProvider provider = requireProvider(providerId, user.getOrganizationId());
        if (!providerRegistry.isRegistered(provider.getAdapterKey())) {
            throw new ApiException(HttpStatus.CONFLICT, "ADAPTER_NOT_REGISTERED", "Adapter not registered");
        }
        provider.setStatus(AiProviderStatus.ACTIVE);
        provider.setUpdatedBy(user.getUserId());
        provider.setUpdatedAt(Instant.now());
        providerRepository.save(provider);
        auditService.providerStatusChanged(
                user.getOrganizationId(), providerId, AiProviderStatus.ACTIVE.name(), user.getUserId());
        return mapper.toProviderResponse(provider);
    }

    @Transactional
    public ProviderResponse disable(UUID providerId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_PROVIDER_DISABLE);
        AiProvider provider = requireProvider(providerId, user.getOrganizationId());
        provider.setStatus(AiProviderStatus.DISABLED);
        provider.setUpdatedBy(user.getUserId());
        provider.setUpdatedAt(Instant.now());
        providerRepository.save(provider);
        auditService.providerStatusChanged(
                user.getOrganizationId(), providerId, AiProviderStatus.DISABLED.name(), user.getUserId());
        return mapper.toProviderResponse(provider);
    }

    @Transactional
    public ProviderResponse archive(UUID providerId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_PROVIDER_ARCHIVE);
        AiProvider provider = requireProvider(providerId, user.getOrganizationId());
        provider.setStatus(AiProviderStatus.ARCHIVED);
        provider.setUpdatedBy(user.getUserId());
        provider.setUpdatedAt(Instant.now());
        providerRepository.save(provider);
        auditService.providerStatusChanged(
                user.getOrganizationId(), providerId, AiProviderStatus.ARCHIVED.name(), user.getUserId());
        return mapper.toProviderResponse(provider);
    }

    AiProvider requireProvider(UUID providerId, UUID organizationId) {
        return providerRepository
                .findByIdAndOrganizationId(providerId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROVIDER_NOT_FOUND", "Provider not found"));
    }

    private ProviderAdapterResponse toAdapterResponse(AiModelProvider provider) {
        return new ProviderAdapterResponse(
                provider.adapterKey(),
                provider.capabilities().tools(),
                provider.capabilities().knowledgeContext(),
                provider.capabilities().jsonOutput(),
                provider.capabilities().systemMessages(),
                provider.capabilities().streaming());
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static ApiException optimisticLockFailure() {
        return new ApiException(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK", "Resource was updated by another request");
    }
}
