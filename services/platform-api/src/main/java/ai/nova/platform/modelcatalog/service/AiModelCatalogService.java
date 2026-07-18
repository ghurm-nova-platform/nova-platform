package ai.nova.platform.modelcatalog.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.modelcatalog.dto.ModelCatalogDtos.CapabilityInput;
import ai.nova.platform.modelcatalog.dto.ModelCatalogDtos.CatalogModelResponse;
import ai.nova.platform.modelcatalog.dto.ModelCatalogDtos.CreateCatalogModelRequest;
import ai.nova.platform.modelcatalog.dto.ModelCatalogDtos.ReplaceCapabilitiesRequest;
import ai.nova.platform.modelcatalog.dto.ModelCatalogDtos.UpdateCatalogModelRequest;
import ai.nova.platform.modelcatalog.entity.AiModelCapability;
import ai.nova.platform.modelcatalog.entity.AiModelCapabilityEntity;
import ai.nova.platform.modelcatalog.entity.AiModelCapabilityEntity.AiModelCapabilityId;
import ai.nova.platform.modelcatalog.mapper.ModelCatalogMapper;
import ai.nova.platform.modelcatalog.repository.AiModelCapabilityRepository;
import ai.nova.platform.modelgateway.entity.AiModel;
import ai.nova.platform.modelgateway.entity.AiModelSource;
import ai.nova.platform.modelgateway.entity.AiModelStatus;
import ai.nova.platform.modelgateway.entity.AiModelType;
import ai.nova.platform.modelgateway.entity.AiProvider;
import ai.nova.platform.modelgateway.entity.AiProviderStatus;
import ai.nova.platform.modelgateway.entity.AiProviderType;
import ai.nova.platform.modelgateway.provider.ProviderCredentialResolver;
import ai.nova.platform.modelgateway.repository.AiModelRepository;
import ai.nova.platform.modelgateway.repository.AiProviderRepository;
import ai.nova.platform.modelgateway.security.ModelGatewayAuthorizationService;
import ai.nova.platform.modelgateway.service.AiProviderService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class AiModelCatalogService {

    public static final Pattern MODEL_KEY_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9._:-]{1,149}$");

    private final AiModelRepository modelRepository;
    private final AiProviderRepository providerRepository;
    private final AiProviderService providerService;
    private final AiModelCapabilityRepository capabilityRepository;
    private final ProviderCredentialResolver credentialResolver;
    private final ModelCatalogMapper mapper;
    private final ModelGatewayAuthorizationService authorizationService;

    public AiModelCatalogService(
            AiModelRepository modelRepository,
            AiProviderRepository providerRepository,
            AiProviderService providerService,
            AiModelCapabilityRepository capabilityRepository,
            ProviderCredentialResolver credentialResolver,
            ModelCatalogMapper mapper,
            ModelGatewayAuthorizationService authorizationService) {
        this.modelRepository = modelRepository;
        this.providerRepository = providerRepository;
        this.providerService = providerService;
        this.capabilityRepository = capabilityRepository;
        this.credentialResolver = credentialResolver;
        this.mapper = mapper;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public Page<CatalogModelResponse> list(
            AiModelStatus status,
            UUID providerId,
            AiModelSource source,
            AiModelCapability capability,
            String search,
            Pageable pageable,
            AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_CATALOG_READ);
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        Page<AiModel> page = modelRepository.searchCatalog(
                user.getOrganizationId(), status, providerId, source, capability, normalizedSearch, pageable);
        Map<UUID, AiProvider> providers = loadProviders(page.getContent(), user.getOrganizationId());
        Map<UUID, List<AiModelCapabilityEntity>> caps = loadCapabilities(page.getContent());
        return page.map(model -> mapper.toResponse(
                model, providers.get(model.getProviderId()), caps.getOrDefault(model.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public CatalogModelResponse get(UUID modelId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_CATALOG_READ);
        AiModel model = requireModel(modelId, user.getOrganizationId());
        AiProvider provider = providerRepository
                .findByIdAndOrganizationId(model.getProviderId(), user.getOrganizationId())
                .orElse(null);
        return mapper.toResponse(model, provider, capabilityRepository.findByIdModelId(modelId));
    }

    @Transactional
    public CatalogModelResponse create(CreateCatalogModelRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_CATALOG_CREATE);
        AiProvider provider = providerService.requireProvider(request.providerId(), user.getOrganizationId());
        if (provider.getStatus() == AiProviderStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "PROVIDER_ARCHIVED", "Provider is archived");
        }
        String key = normalizeModelKey(request.modelKey());
        validateModelKey(key);
        if (modelRepository.existsByOrganizationIdAndModelKey(user.getOrganizationId(), key)) {
            throw new ApiException(HttpStatus.CONFLICT, "MODEL_KEY_EXISTS", "Model key already exists");
        }
        String providerModelId = request.providerModelId().trim();
        if (providerModelId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PROVIDER_MODEL_ID_REQUIRED", "Provider model id required");
        }
        if (modelRepository.findByProviderIdAndProviderModelId(provider.getId(), providerModelId).isPresent()) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "PROVIDER_MODEL_ID_EXISTS", "Provider model id already registered");
        }
        validateTokenBounds(request.contextWindowTokens(), request.maxOutputTokens(), request.defaultMaxOutputTokens());
        validateDefaults(request.defaultTemperature(), request.defaultTopP());

        Instant now = Instant.now();
        AiModel model = new AiModel();
        model.setId(UUID.randomUUID());
        model.setOrganizationId(user.getOrganizationId());
        model.setProviderId(provider.getId());
        model.setModelKey(key);
        model.setProviderModelId(providerModelId);
        model.setDisplayName(request.displayName().trim());
        model.setDescription(trim(request.description()));
        model.setModelType(request.modelType());
        model.setStatus(AiModelStatus.DRAFT);
        model.setSource(AiModelSource.MANUAL);
        model.setModelFamily(trim(request.modelFamily()));
        model.setModelVersion(trim(request.modelVersion()));
        model.setContextWindowTokens(request.contextWindowTokens());
        model.setContextWindow(request.contextWindowTokens());
        model.setMaxInputTokens(request.maxInputTokens());
        model.setMaxOutputTokens(request.maxOutputTokens());
        model.setDefaultTemperature(request.defaultTemperature());
        model.setDefaultTopP(request.defaultTopP());
        model.setDefaultMaxOutputTokens(request.defaultMaxOutputTokens());
        model.setSupportsKnowledgeContext(
                request.supportsKnowledgeContext() == null || request.supportsKnowledgeContext());
        model.setSupportsSystemMessages(
                request.supportsSystemMessages() == null || request.supportsSystemMessages());
        model.setSupportsTools(false);
        model.setSupportsJsonOutput(false);
        model.setSupportsStreaming(false);
        model.setInputCostPerMillion(request.inputCostPerMillion());
        model.setOutputCostPerMillion(request.outputCostPerMillion());
        String currency = trim(request.currency());
        model.setCurrency(currency);
        model.setCurrencyCode(currency != null && currency.length() <= 3 ? currency : null);
        model.setCreatedBy(user.getUserId());
        model.setUpdatedBy(user.getUserId());
        model.setCreatedAt(now);
        model.setUpdatedAt(now);

        List<AiModelCapability> initialCaps = request.capabilities() == null || request.capabilities().isEmpty()
                ? defaultCapabilitiesForType(request.modelType())
                : request.capabilities();
        List<CapabilityInput> capabilityInputs = toCapabilityInputs(initialCaps);
        syncBooleanCacheFromInputs(model, capabilityInputs);
        modelRepository.save(model);
        replaceCapabilityRows(model, capabilityInputs, now);
        return mapper.toResponse(model, provider, capabilityRepository.findByIdModelId(model.getId()));
    }

    @Transactional
    public CatalogModelResponse update(UUID modelId, UpdateCatalogModelRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_CATALOG_UPDATE);
        AiModel model = requireModel(modelId, user.getOrganizationId());
        if (!model.getVersion().equals(request.version())) {
            throw conflict();
        }
        if (model.getStatus() == AiModelStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "MODEL_ARCHIVED", "Archived models cannot be updated");
        }
        validateTokenBounds(request.contextWindowTokens(), request.maxOutputTokens(), request.defaultMaxOutputTokens());
        validateDefaults(request.defaultTemperature(), request.defaultTopP());

        model.setDisplayName(request.displayName().trim());
        model.setDescription(trim(request.description()));
        model.setModelFamily(trim(request.modelFamily()));
        model.setModelVersion(trim(request.modelVersion()));
        model.setContextWindowTokens(request.contextWindowTokens());
        model.setContextWindow(request.contextWindowTokens());
        model.setMaxInputTokens(request.maxInputTokens());
        model.setMaxOutputTokens(request.maxOutputTokens());
        model.setDefaultTemperature(request.defaultTemperature());
        model.setDefaultTopP(request.defaultTopP());
        model.setDefaultMaxOutputTokens(request.defaultMaxOutputTokens());
        if (request.supportsKnowledgeContext() != null) {
            model.setSupportsKnowledgeContext(request.supportsKnowledgeContext());
        }
        if (request.supportsSystemMessages() != null) {
            model.setSupportsSystemMessages(request.supportsSystemMessages());
        }
        model.setInputCostPerMillion(request.inputCostPerMillion());
        model.setOutputCostPerMillion(request.outputCostPerMillion());
        String currency = trim(request.currency());
        model.setCurrency(currency);
        if (currency != null && currency.length() <= 3) {
            model.setCurrencyCode(currency);
        }
        model.setUpdatedBy(user.getUserId());
        model.setUpdatedAt(Instant.now());
        modelRepository.save(model);
        AiProvider provider = providerRepository
                .findByIdAndOrganizationId(model.getProviderId(), user.getOrganizationId())
                .orElse(null);
        return mapper.toResponse(model, provider, capabilityRepository.findByIdModelId(modelId));
    }

    @Transactional
    public CatalogModelResponse activate(UUID modelId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_CATALOG_UPDATE);
        AiModel model = requireModel(modelId, user.getOrganizationId());
        AiProvider provider = providerService.requireProvider(model.getProviderId(), user.getOrganizationId());
        if (provider.getStatus() != AiProviderStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "PROVIDER_NOT_ACTIVE", "Provider must be active");
        }
        if (provider.getStatus() == AiProviderStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "PROVIDER_ARCHIVED", "Provider is archived");
        }
        if (model.getProviderModelId() == null || model.getProviderModelId().isBlank()) {
            throw new ApiException(HttpStatus.CONFLICT, "PROVIDER_MODEL_ID_REQUIRED", "Provider model id required");
        }
        validateModelKey(model.getModelKey());
        if (!capabilityRepository.existsByIdModelIdAndEnabledTrue(modelId)) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "MODEL_CAPABILITY_REQUIRED", "At least one enabled capability is required");
        }
        requireCredentialIfNeeded(provider);
        model.setStatus(AiModelStatus.ACTIVE);
        model.setUpdatedBy(user.getUserId());
        model.setUpdatedAt(Instant.now());
        modelRepository.save(model);
        return mapper.toResponse(model, provider, capabilityRepository.findByIdModelId(modelId));
    }

    @Transactional
    public CatalogModelResponse disable(UUID modelId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_CATALOG_UPDATE);
        AiModel model = requireModel(modelId, user.getOrganizationId());
        model.setStatus(AiModelStatus.DISABLED);
        model.setUpdatedBy(user.getUserId());
        model.setUpdatedAt(Instant.now());
        modelRepository.save(model);
        return toResponse(model, user.getOrganizationId());
    }

    @Transactional
    public CatalogModelResponse deprecate(UUID modelId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_CATALOG_UPDATE);
        AiModel model = requireModel(modelId, user.getOrganizationId());
        if (model.getStatus() == AiModelStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "MODEL_ARCHIVED", "Archived models cannot be deprecated");
        }
        model.setStatus(AiModelStatus.DEPRECATED);
        model.setUpdatedBy(user.getUserId());
        model.setUpdatedAt(Instant.now());
        modelRepository.save(model);
        return toResponse(model, user.getOrganizationId());
    }

    @Transactional
    public CatalogModelResponse archive(UUID modelId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_CATALOG_DELETE);
        AiModel model = requireModel(modelId, user.getOrganizationId());
        model.setStatus(AiModelStatus.ARCHIVED);
        model.setUpdatedBy(user.getUserId());
        model.setUpdatedAt(Instant.now());
        modelRepository.save(model);
        return toResponse(model, user.getOrganizationId());
    }

    @Transactional
    public CatalogModelResponse replaceCapabilities(
            UUID modelId, ReplaceCapabilitiesRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_CAPABILITY_MANAGE);
        AiModel model = requireModel(modelId, user.getOrganizationId());
        if (model.getStatus() == AiModelStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "MODEL_ARCHIVED", "Archived models cannot change capabilities");
        }
        Instant now = Instant.now();
        replaceCapabilityRows(model, request.capabilities(), now);
        syncBooleanCache(model, capabilityRepository.findByIdModelId(modelId));
        model.setUpdatedBy(user.getUserId());
        model.setUpdatedAt(now);
        modelRepository.save(model);
        return toResponse(model, user.getOrganizationId());
    }

    public void syncBooleanCache(AiModel model, List<AiModelCapabilityEntity> capabilities) {
        Set<AiModelCapability> enabled = capabilities.stream()
                .filter(AiModelCapabilityEntity::isEnabled)
                .map(AiModelCapabilityEntity::getCapability)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(AiModelCapability.class)));
        applyEnabledCapabilities(model, enabled);
    }

    private void syncBooleanCacheFromInputs(AiModel model, List<CapabilityInput> inputs) {
        Set<AiModelCapability> enabled = EnumSet.noneOf(AiModelCapability.class);
        if (inputs != null) {
            for (CapabilityInput input : inputs) {
                if (input != null
                        && input.capability() != null
                        && (input.enabled() == null || input.enabled())) {
                    enabled.add(input.capability());
                }
            }
        }
        applyEnabledCapabilities(model, enabled);
    }

    private static void applyEnabledCapabilities(AiModel model, Set<AiModelCapability> enabled) {
        model.setSupportsTools(
                enabled.contains(AiModelCapability.TOOL_CALLING)
                        || enabled.contains(AiModelCapability.FUNCTION_CALLING));
        model.setSupportsJsonOutput(
                enabled.contains(AiModelCapability.JSON_MODE)
                        || enabled.contains(AiModelCapability.STRUCTURED_OUTPUT));
        model.setSupportsStreaming(enabled.contains(AiModelCapability.STREAMING));
    }

    public static String normalizeModelKey(String modelKey) {
        return modelKey.trim().toLowerCase(Locale.ROOT);
    }

    public static void validateModelKey(String key) {
        if (key == null || !MODEL_KEY_PATTERN.matcher(key).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MODEL_KEY_INVALID", "Invalid model key");
        }
    }

    private void replaceCapabilityRows(AiModel model, List<CapabilityInput> inputs, Instant now) {
        capabilityRepository.deleteByIdModelId(model.getId());
        capabilityRepository.flush();
        if (inputs == null || inputs.isEmpty()) {
            return;
        }
        Set<AiModelCapability> seen = new HashSet<>();
        List<AiModelCapabilityEntity> rows = new ArrayList<>();
        for (CapabilityInput input : inputs) {
            if (input == null || input.capability() == null) {
                continue;
            }
            if (!seen.add(input.capability())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "CAPABILITY_DUPLICATE", "Duplicate capability");
            }
            AiModelCapabilityEntity entity = new AiModelCapabilityEntity();
            entity.setId(new AiModelCapabilityId(model.getId(), input.capability()));
            entity.setEnabled(input.enabled() == null || input.enabled());
            entity.setMetadataJson(trim(input.metadataJson()));
            entity.setCreatedAt(now);
            rows.add(entity);
        }
        capabilityRepository.saveAll(rows);
    }

    private void requireCredentialIfNeeded(AiProvider provider) {
        if (provider.getProviderType() == AiProviderType.DETERMINISTIC_LOCAL) {
            return;
        }
        String credential = credentialResolver
                .resolve(provider.getCredentialReference(), provider.getOrganizationId())
                .orElse(null);
        if (credential == null || credential.isBlank()) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "CREDENTIAL_UNRESOLVABLE", "Provider credential is not resolvable");
        }
    }

    private CatalogModelResponse toResponse(AiModel model, UUID organizationId) {
        AiProvider provider =
                providerRepository.findByIdAndOrganizationId(model.getProviderId(), organizationId).orElse(null);
        return mapper.toResponse(model, provider, capabilityRepository.findByIdModelId(model.getId()));
    }

    private AiModel requireModel(UUID modelId, UUID organizationId) {
        return modelRepository
                .findByIdAndOrganizationId(modelId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MODEL_NOT_FOUND", "Model not found"));
    }

    private Map<UUID, AiProvider> loadProviders(List<AiModel> models, UUID organizationId) {
        Map<UUID, AiProvider> map = new HashMap<>();
        for (AiModel model : models) {
            map.computeIfAbsent(
                    model.getProviderId(),
                    id -> providerRepository.findByIdAndOrganizationId(id, organizationId).orElse(null));
        }
        return map;
    }

    private Map<UUID, List<AiModelCapabilityEntity>> loadCapabilities(List<AiModel> models) {
        if (models.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = models.stream().map(AiModel::getId).toList();
        return capabilityRepository.findByIdModelIdIn(ids).stream()
                .collect(Collectors.groupingBy(AiModelCapabilityEntity::getModelId));
    }

    private static List<AiModelCapability> defaultCapabilitiesForType(AiModelType type) {
        return switch (type) {
            case EMBEDDING -> List.of(AiModelCapability.EMBEDDINGS);
            case CHAT, TEXT_GENERATION, REASONING, MULTIMODAL -> List.of(AiModelCapability.CHAT);
        };
    }

    private static List<CapabilityInput> toCapabilityInputs(List<AiModelCapability> capabilities) {
        return capabilities.stream().map(c -> new CapabilityInput(c, true, null)).toList();
    }

    private static void validateTokenBounds(
            Integer contextWindow, Integer maxOutput, Integer defaultMaxOutput) {
        if (contextWindow == null || contextWindow <= 0 || maxOutput == null || maxOutput <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "Token limits must be positive");
        }
        if (maxOutput > contextWindow) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "maxOutputTokens exceeds context window");
        }
        if (defaultMaxOutput != null && (defaultMaxOutput <= 0 || defaultMaxOutput > maxOutput)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "INVALID_INPUT", "defaultMaxOutputTokens must be within maxOutputTokens");
        }
    }

    private static void validateDefaults(BigDecimal temperature, BigDecimal topP) {
        if (temperature != null
                && (temperature.compareTo(BigDecimal.ZERO) < 0
                        || temperature.compareTo(BigDecimal.valueOf(2)) > 0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "defaultTemperature out of range");
        }
        if (topP != null
                && (topP.compareTo(BigDecimal.ZERO) <= 0 || topP.compareTo(BigDecimal.ONE) > 0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "defaultTopP out of range");
        }
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ApiException conflict() {
        return new ApiException(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK", "Resource was updated by another request");
    }
}
