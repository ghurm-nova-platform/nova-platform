package ai.nova.platform.modelgateway.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
import ai.nova.platform.modelgateway.entity.AiProviderType;
import ai.nova.platform.modelgateway.entity.ConnectionTestStatus;
import ai.nova.platform.modelgateway.entity.EndpointProfile;
import ai.nova.platform.modelgateway.mapper.ModelGatewayMapper;
import ai.nova.platform.modelgateway.provider.AiModelProvider;
import ai.nova.platform.modelgateway.provider.AiModelProviderRegistry;
import ai.nova.platform.modelgateway.provider.ProviderCredentialResolver;
import ai.nova.platform.modelgateway.repository.AiProviderRepository;
import ai.nova.platform.modelgateway.security.ModelGatewayAuthorizationService;
import ai.nova.platform.modelgateway.validation.CredentialReferenceValidator;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class AiProviderService {

    private static final Pattern PROVIDER_KEY_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]*$");
    private static final Pattern AZURE_RESOURCE_PATTERN =
            Pattern.compile("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$");

    private final AiProviderRepository providerRepository;
    private final AiModelProviderRegistry providerRegistry;
    private final ModelGatewayProperties properties;
    private final ModelGatewayMapper mapper;
    private final ModelGatewayAuthorizationService authorizationService;
    private final CredentialReferenceValidator credentialReferenceValidator;
    private final ProviderCredentialResolver credentialResolver;
    private final ModelGatewayAuditService auditService;

    public AiProviderService(
            AiProviderRepository providerRepository,
            AiModelProviderRegistry providerRegistry,
            ModelGatewayProperties properties,
            ModelGatewayMapper mapper,
            ModelGatewayAuthorizationService authorizationService,
            CredentialReferenceValidator credentialReferenceValidator,
            ProviderCredentialResolver credentialResolver,
            ModelGatewayAuditService auditService) {
        this.providerRepository = providerRepository;
        this.providerRegistry = providerRegistry;
        this.properties = properties;
        this.mapper = mapper;
        this.authorizationService = authorizationService;
        this.credentialReferenceValidator = credentialReferenceValidator;
        this.credentialResolver = credentialResolver;
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
        applyEndpointFields(
                request.providerType(),
                request.endpointProfile(),
                request.azureResourceName(),
                request.azureApiVersion(),
                true);

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
        setEndpointMetadata(
                provider,
                request.providerType(),
                request.endpointProfile(),
                request.azureResourceName(),
                request.azureApiVersion());
        provider.setStatus(AiProviderStatus.DRAFT);
        provider.setLastConnectionTestStatus(ConnectionTestStatus.NEVER);
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
        applyEndpointFields(
                provider.getProviderType(),
                request.endpointProfile(),
                request.azureResourceName(),
                request.azureApiVersion(),
                true);
        boolean connectionRelevantChanged = connectionRelevantFieldsChanged(
                provider,
                trimToNull(request.credentialReference()),
                request.endpointProfile(),
                request.azureResourceName(),
                request.azureApiVersion());
        provider.setName(request.name().trim());
        provider.setDescription(trimToNull(request.description()));
        provider.setCredentialReference(trimToNull(request.credentialReference()));
        provider.setRegion(trimToNull(request.region()));
        setEndpointMetadata(
                provider,
                provider.getProviderType(),
                request.endpointProfile(),
                request.azureResourceName(),
                request.azureApiVersion());
        if (connectionRelevantChanged) {
            clearConnectionTest(provider);
        }
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
        validateActivationRequirements(provider);
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

    public AiProvider requireProvider(UUID providerId, UUID organizationId) {
        return providerRepository
                .findByIdAndOrganizationId(providerId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROVIDER_NOT_FOUND", "Provider not found"));
    }

    private void validateActivationRequirements(AiProvider provider) {
        AiProviderType type = provider.getProviderType();
        if (type == AiProviderType.DETERMINISTIC_LOCAL) {
            if (provider.getCredentialReference() != null && !provider.getCredentialReference().isBlank()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_CREDENTIAL_REFERENCE",
                        "Deterministic local providers must not have credentials");
            }
            return;
        }
        if (type == AiProviderType.OPENAI || type == AiProviderType.AZURE_OPENAI) {
            applyEndpointFields(
                    type,
                    provider.getEndpointProfile(),
                    provider.getAzureResourceName(),
                    provider.getAzureApiVersion(),
                    false);
            if (provider.getCredentialReference() == null || provider.getCredentialReference().isBlank()) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "CREDENTIAL_REQUIRED",
                        "Active OpenAI/Azure providers require a credential reference");
            }
            if (credentialResolver
                    .resolve(provider.getCredentialReference(), provider.getOrganizationId())
                    .isEmpty()) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "CREDENTIAL_UNRESOLVABLE",
                        "Credential reference could not be resolved to an active secret");
            }
            if (provider.getLastConnectionTestStatus() != ConnectionTestStatus.SUCCESS) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "CONNECTION_TEST_REQUIRED",
                        "A successful connection test is required before activation");
            }
        }
    }

    private void applyEndpointFields(
            AiProviderType providerType,
            EndpointProfile endpointProfile,
            String azureResourceName,
            String azureApiVersion,
            boolean allowNullProfile) {
        if (providerType == AiProviderType.OPENAI) {
            if (endpointProfile == null) {
                if (!allowNullProfile) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "ENDPOINT_PROFILE_REQUIRED",
                            "OPENAI providers require endpointProfile OPENAI_PUBLIC");
                }
                return;
            }
            if (endpointProfile != EndpointProfile.OPENAI_PUBLIC) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "ENDPOINT_PROFILE_INVALID",
                        "OPENAI providers must use OPENAI_PUBLIC");
            }
            if (azureResourceName != null && !azureResourceName.isBlank()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "AZURE_FIELDS_INVALID",
                        "OPENAI providers must not set azureResourceName");
            }
            return;
        }
        if (providerType == AiProviderType.AZURE_OPENAI) {
            if (endpointProfile == null) {
                if (!allowNullProfile) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "ENDPOINT_PROFILE_REQUIRED",
                            "AZURE_OPENAI providers require endpointProfile AZURE_OPENAI_RESOURCE");
                }
                return;
            }
            if (endpointProfile != EndpointProfile.AZURE_OPENAI_RESOURCE) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "ENDPOINT_PROFILE_INVALID",
                        "AZURE_OPENAI providers must use AZURE_OPENAI_RESOURCE");
            }
            String resource = trimToNull(azureResourceName);
            String apiVersion = trimToNull(azureApiVersion);
            if (resource == null || apiVersion == null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "AZURE_ENDPOINT_INCOMPLETE",
                        "AZURE_OPENAI requires azureResourceName and azureApiVersion");
            }
            if (!AZURE_RESOURCE_PATTERN.matcher(resource).matches()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "AZURE_RESOURCE_INVALID",
                        "Invalid azureResourceName");
            }
        }
    }

    private void setEndpointMetadata(
            AiProvider provider,
            AiProviderType providerType,
            EndpointProfile endpointProfile,
            String azureResourceName,
            String azureApiVersion) {
        if (providerType == AiProviderType.OPENAI) {
            provider.setEndpointProfile(endpointProfile != null ? endpointProfile : null);
            provider.setAzureResourceName(null);
            provider.setAzureApiVersion(null);
            return;
        }
        if (providerType == AiProviderType.AZURE_OPENAI) {
            provider.setEndpointProfile(endpointProfile);
            provider.setAzureResourceName(trimToNull(azureResourceName));
            provider.setAzureApiVersion(trimToNull(azureApiVersion));
            return;
        }
        provider.setEndpointProfile(endpointProfile);
        provider.setAzureResourceName(trimToNull(azureResourceName));
        provider.setAzureApiVersion(trimToNull(azureApiVersion));
    }

    private static boolean connectionRelevantFieldsChanged(
            AiProvider provider,
            String newCredentialReference,
            EndpointProfile newEndpointProfile,
            String newAzureResourceName,
            String newAzureApiVersion) {
        return !java.util.Objects.equals(provider.getCredentialReference(), newCredentialReference)
                || !java.util.Objects.equals(provider.getEndpointProfile(), newEndpointProfile)
                || !java.util.Objects.equals(provider.getAzureResourceName(), trimToNull(newAzureResourceName))
                || !java.util.Objects.equals(provider.getAzureApiVersion(), trimToNull(newAzureApiVersion));
    }

    private static void clearConnectionTest(AiProvider provider) {
        provider.setLastConnectionTestStatus(ConnectionTestStatus.NEVER);
        provider.setLastConnectionTestAt(null);
        provider.setLastConnectionTestErrorCode(null);
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
