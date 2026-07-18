package ai.nova.platform.modelcatalog.sync;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import ai.nova.platform.modelcatalog.dto.ModelCatalogDtos.SyncResultResponse;
import ai.nova.platform.modelcatalog.entity.AiModelCapability;
import ai.nova.platform.modelcatalog.entity.AiModelCapabilityEntity;
import ai.nova.platform.modelcatalog.entity.AiModelCapabilityEntity.AiModelCapabilityId;
import ai.nova.platform.modelcatalog.repository.AiModelCapabilityRepository;
import ai.nova.platform.modelcatalog.service.AiModelCatalogService;
import ai.nova.platform.modelgateway.entity.AiModel;
import ai.nova.platform.modelgateway.entity.AiModelSource;
import ai.nova.platform.modelgateway.entity.AiModelStatus;
import ai.nova.platform.modelgateway.entity.AiProvider;
import ai.nova.platform.modelgateway.entity.AiProviderStatus;
import ai.nova.platform.modelgateway.entity.AiProviderType;
import ai.nova.platform.modelgateway.entity.ConnectionTestStatus;
import ai.nova.platform.modelgateway.entity.EndpointProfile;
import ai.nova.platform.modelgateway.entity.ModelSyncStatus;
import ai.nova.platform.modelgateway.provider.ProviderCredentialResolver;
import ai.nova.platform.modelgateway.provider.ProviderException;
import ai.nova.platform.modelgateway.provider.error.UnifiedProviderErrorMapper;
import ai.nova.platform.modelgateway.repository.AiModelRepository;
import ai.nova.platform.modelgateway.repository.AiProviderRepository;
import ai.nova.platform.modelgateway.security.ModelGatewayAuthorizationService;
import ai.nova.platform.modelgateway.service.AiProviderService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class ModelCatalogSyncService {

    private static final Pattern SAFE_KEY_CHAR = Pattern.compile("[^a-z0-9._:-]");

    private final AiProviderService providerService;
    private final AiProviderRepository providerRepository;
    private final AiModelRepository modelRepository;
    private final AiModelCapabilityRepository capabilityRepository;
    private final AiModelCatalogService catalogService;
    private final ProviderCredentialResolver credentialResolver;
    private final List<ProviderModelCatalogClient> catalogClients;
    private final UnifiedProviderErrorMapper errorMapper;
    private final ModelGatewayAuthorizationService authorizationService;
    private final TransactionTemplate transactionTemplate;

    public ModelCatalogSyncService(
            AiProviderService providerService,
            AiProviderRepository providerRepository,
            AiModelRepository modelRepository,
            AiModelCapabilityRepository capabilityRepository,
            AiModelCatalogService catalogService,
            ProviderCredentialResolver credentialResolver,
            List<ProviderModelCatalogClient> catalogClients,
            UnifiedProviderErrorMapper errorMapper,
            ModelGatewayAuthorizationService authorizationService,
            TransactionTemplate transactionTemplate) {
        this.providerService = providerService;
        this.providerRepository = providerRepository;
        this.modelRepository = modelRepository;
        this.capabilityRepository = capabilityRepository;
        this.catalogService = catalogService;
        this.credentialResolver = credentialResolver;
        this.catalogClients = catalogClients;
        this.errorMapper = errorMapper;
        this.authorizationService = authorizationService;
        this.transactionTemplate = transactionTemplate;
    }

    public SyncResultResponse syncModels(UUID providerId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_CATALOG_SYNC);
        SyncContext context = transactionTemplate.execute(status -> loadContext(providerId, user.getOrganizationId()));
        Instant syncedAt = Instant.now();

        if (context.providerStatus() != AiProviderStatus.ACTIVE) {
            return persistSummary(context, user, ModelSyncStatus.FAILED, "PROVIDER_NOT_ACTIVE", syncedAt, 0, 0, 0, 0);
        }
        if (context.lastConnectionTestStatus() != ConnectionTestStatus.SUCCESS) {
            return persistSummary(
                    context, user, ModelSyncStatus.FAILED, "CONNECTION_TEST_REQUIRED", syncedAt, 0, 0, 0, 0);
        }

        ProviderModelCatalogClient client = catalogClients.stream()
                .filter(c -> c.supports(context.providerType()))
                .findFirst()
                .orElse(null);
        if (client == null) {
            return persistSummary(
                    context, user, ModelSyncStatus.UNSUPPORTED, "MODEL_SYNC_UNSUPPORTED", syncedAt, 0, 0, 0, 0);
        }

        String credential = credentialResolver
                .resolve(context.credentialReference(), context.organizationId())
                .orElse(null);
        if (credential == null || credential.isBlank()) {
            return persistSummary(
                    context, user, ModelSyncStatus.FAILED, "CREDENTIAL_UNRESOLVABLE", syncedAt, 0, 0, 0, 0);
        }

        List<DiscoveredModelDescriptor> discovered;
        try {
            // Intentionally outside any DB transaction.
            AiProvider probeProvider = toProbeProvider(context);
            discovered = client.discoverModels(probeProvider, credential);
        } catch (ApiException ex) {
            ModelSyncStatus status =
                    "MODEL_SYNC_UNSUPPORTED".equals(ex.getCode()) ? ModelSyncStatus.UNSUPPORTED : ModelSyncStatus.FAILED;
            return persistSummary(context, user, status, ex.getCode(), syncedAt, 0, 0, 0, 0);
        } catch (ProviderException ex) {
            return persistSummary(context, user, ModelSyncStatus.FAILED, ex.errorCode(), syncedAt, 0, 0, 0, 0);
        } catch (Exception ex) {
            ProviderException mapped = errorMapper.mapTransport(ex);
            return persistSummary(context, user, ModelSyncStatus.FAILED, mapped.errorCode(), syncedAt, 0, 0, 0, 0);
        }

        return transactionTemplate.execute(tx -> applyDiscoveries(context, user, discovered, syncedAt));
    }

    private SyncResultResponse applyDiscoveries(
            SyncContext context,
            AuthenticatedUser user,
            List<DiscoveredModelDescriptor> discovered,
            Instant syncedAt) {
        AiProvider provider = providerService.requireProvider(context.providerId(), user.getOrganizationId());
        if (!context.matchesCurrent(provider)) {
            provider.setLastModelSyncAt(syncedAt);
            provider.setLastModelSyncStatus(ModelSyncStatus.STALE);
            provider.setLastModelSyncErrorCode("MODEL_SYNC_STALE");
            provider.setUpdatedBy(user.getUserId());
            provider.setUpdatedAt(syncedAt);
            providerRepository.save(provider);
            return new SyncResultResponse(
                    context.providerId(), 0, 0, 0, 0, true, ModelSyncStatus.STALE, "MODEL_SYNC_STALE", syncedAt);
        }

        int created = 0;
        int updated = 0;
        int unchanged = 0;
        for (DiscoveredModelDescriptor descriptor : discovered) {
            Optional<AiModel> existing =
                    modelRepository.findByProviderIdAndProviderModelId(provider.getId(), descriptor.providerModelId());
            if (existing.isEmpty()) {
                createSyncedModel(provider, descriptor, user, syncedAt);
                created++;
            } else {
                ModelRowSnapshot snapshot = context.modelSnapshots().get(descriptor.providerModelId());
                UpdateOutcome outcome = updateSyncedModel(existing.get(), snapshot, descriptor, user, syncedAt);
                switch (outcome) {
                    case UPDATED -> updated++;
                    case UNCHANGED, SKIPPED_STALE, SKIPPED_MANUAL -> unchanged++;
                }
            }
        }

        provider.setLastModelSyncAt(syncedAt);
        provider.setLastModelSyncStatus(ModelSyncStatus.SUCCESS);
        provider.setLastModelSyncErrorCode(null);
        provider.setLastModelSyncDiscoveredCount(discovered.size());
        provider.setLastModelSyncCreatedCount(created);
        provider.setLastModelSyncUpdatedCount(updated);
        provider.setLastModelSyncUnchangedCount(unchanged);
        provider.setUpdatedBy(user.getUserId());
        provider.setUpdatedAt(syncedAt);
        providerRepository.save(provider);
        return new SyncResultResponse(
                context.providerId(),
                discovered.size(),
                created,
                updated,
                unchanged,
                false,
                ModelSyncStatus.SUCCESS,
                null,
                syncedAt);
    }

    private void createSyncedModel(
            AiProvider provider, DiscoveredModelDescriptor descriptor, AuthenticatedUser user, Instant syncedAt) {
        String modelKey = uniqueModelKey(provider.getOrganizationId(), descriptor.providerModelId());
        int context = descriptor.contextWindow() != null && descriptor.contextWindow() > 0
                ? descriptor.contextWindow()
                : 8192;
        int maxOut = descriptor.maxOutputTokens() != null && descriptor.maxOutputTokens() > 0
                ? Math.min(descriptor.maxOutputTokens(), context)
                : Math.min(4096, context);

        AiModel model = new AiModel();
        model.setId(UUID.randomUUID());
        model.setOrganizationId(provider.getOrganizationId());
        model.setProviderId(provider.getId());
        model.setModelKey(modelKey);
        model.setProviderModelId(descriptor.providerModelId());
        model.setDisplayName(
                descriptor.displayName() != null ? descriptor.displayName() : descriptor.providerModelId());
        model.setModelType(descriptor.modelType());
        model.setStatus(AiModelStatus.DRAFT);
        model.setSource(AiModelSource.PROVIDER_SYNC);
        model.setModelFamily(descriptor.modelFamily());
        model.setModelVersion(descriptor.modelVersion());
        model.setContextWindowTokens(context);
        model.setContextWindow(context);
        model.setMaxOutputTokens(maxOut);
        model.setSupportsTools(false);
        model.setSupportsKnowledgeContext(true);
        model.setSupportsJsonOutput(false);
        model.setSupportsStreaming(false);
        model.setSupportsSystemMessages(true);
        model.setDiscoveredAt(syncedAt);
        model.setLastSyncedAt(syncedAt);
        model.setLastSeenAt(syncedAt);
        model.setCreatedBy(user.getUserId());
        model.setUpdatedBy(user.getUserId());
        model.setCreatedAt(syncedAt);
        model.setUpdatedAt(syncedAt);
        if (!descriptor.capabilities().isEmpty()) {
            catalogService.syncBooleanCache(
                    model,
                    descriptor.capabilities().stream()
                            .map(cap -> {
                                AiModelCapabilityEntity entity = new AiModelCapabilityEntity();
                                entity.setId(new AiModelCapabilityId(model.getId(), cap));
                                entity.setEnabled(true);
                                entity.setCreatedAt(syncedAt);
                                return entity;
                            })
                            .toList());
        }
        modelRepository.save(model);
        if (!descriptor.capabilities().isEmpty()) {
            writeCapabilities(model, descriptor.capabilities(), syncedAt);
        }
    }

    /**
     * Applies discovery to an existing row only when the optimistic version still matches the pre-HTTP
     * snapshot. MANUAL / operator-managed rows never have display/family/context/capabilities overwritten;
     * only sync visibility timestamps may move when the version is unchanged.
     */
    private UpdateOutcome updateSyncedModel(
            AiModel model,
            ModelRowSnapshot snapshot,
            DiscoveredModelDescriptor descriptor,
            AuthenticatedUser user,
            Instant syncedAt) {
        AiModel current = modelRepository
                .findByIdAndOrganizationId(model.getId(), model.getOrganizationId())
                .orElse(model);

        if (snapshot == null || !Objects.equals(snapshot.version(), current.getVersion())) {
            // Operator (or another sync) changed the row after our snapshot — preserve newer config.
            return UpdateOutcome.SKIPPED_STALE;
        }

        if (current.getSource() == AiModelSource.MANUAL || snapshot.source() == AiModelSource.MANUAL) {
            boolean touched = false;
            if (!Objects.equals(current.getLastSeenAt(), syncedAt)) {
                current.setLastSeenAt(syncedAt);
                touched = true;
            }
            if (!Objects.equals(current.getLastSyncedAt(), syncedAt)) {
                current.setLastSyncedAt(syncedAt);
                touched = true;
            }
            if (!touched) {
                return UpdateOutcome.SKIPPED_MANUAL;
            }
            current.setUpdatedBy(user.getUserId());
            current.setUpdatedAt(syncedAt);
            modelRepository.save(current);
            return UpdateOutcome.UNCHANGED;
        }

        boolean changed = false;
        current.setLastSeenAt(syncedAt);
        current.setLastSyncedAt(syncedAt);
        if (current.getDiscoveredAt() == null) {
            current.setDiscoveredAt(syncedAt);
            changed = true;
        }
        if (descriptor.displayName() != null && !descriptor.displayName().equals(current.getDisplayName())) {
            current.setDisplayName(descriptor.displayName());
            changed = true;
        }
        if (descriptor.modelFamily() != null && !Objects.equals(descriptor.modelFamily(), current.getModelFamily())) {
            current.setModelFamily(descriptor.modelFamily());
            changed = true;
        }
        if (descriptor.contextWindow() != null
                && descriptor.contextWindow() > 0
                && !Objects.equals(descriptor.contextWindow(), current.getContextWindow())) {
            current.setContextWindow(descriptor.contextWindow());
            current.setContextWindowTokens(descriptor.contextWindow());
            changed = true;
        }
        List<AiModelCapabilityEntity> existingCaps = capabilityRepository.findByIdModelId(current.getId());
        if (existingCaps.isEmpty() && !descriptor.capabilities().isEmpty()) {
            writeCapabilities(current, descriptor.capabilities(), syncedAt);
            catalogService.syncBooleanCache(current, capabilityRepository.findByIdModelId(current.getId()));
            changed = true;
        }
        if (!changed) {
            // Timestamps alone do not count as an "updated" catalog mutation for sync metrics.
            current.setUpdatedBy(user.getUserId());
            current.setUpdatedAt(syncedAt);
            modelRepository.save(current);
            return UpdateOutcome.UNCHANGED;
        }
        current.setUpdatedBy(user.getUserId());
        current.setUpdatedAt(syncedAt);
        modelRepository.save(current);
        return UpdateOutcome.UPDATED;
    }

    enum UpdateOutcome {
        UPDATED,
        UNCHANGED,
        SKIPPED_STALE,
        SKIPPED_MANUAL
    }

    private void writeCapabilities(AiModel model, java.util.Set<AiModelCapability> capabilities, Instant now) {
        for (AiModelCapability capability : capabilities) {
            AiModelCapabilityEntity entity = new AiModelCapabilityEntity();
            entity.setId(new AiModelCapabilityId(model.getId(), capability));
            entity.setEnabled(true);
            entity.setCreatedAt(now);
            capabilityRepository.save(entity);
        }
    }

    private String uniqueModelKey(UUID organizationId, String providerModelId) {
        String base = SAFE_KEY_CHAR
                .matcher(providerModelId.toLowerCase(Locale.ROOT))
                .replaceAll("-");
        if (base.isBlank() || !Character.isLetterOrDigit(base.charAt(0))) {
            base = "m-" + base;
        }
        if (base.length() > 140) {
            base = base.substring(0, 140);
        }
        String candidate = base;
        int suffix = 1;
        while (modelRepository.existsByOrganizationIdAndModelKey(organizationId, candidate)) {
            String suffixPart = "-" + suffix++;
            candidate = base.substring(0, Math.min(base.length(), 150 - suffixPart.length())) + suffixPart;
        }
        AiModelCatalogService.validateModelKey(candidate);
        return candidate;
    }

    private SyncResultResponse persistSummary(
            SyncContext context,
            AuthenticatedUser user,
            ModelSyncStatus status,
            String errorCode,
            Instant syncedAt,
            int discovered,
            int created,
            int updated,
            int unchanged) {
        return transactionTemplate.execute(tx -> {
            AiProvider provider = providerService.requireProvider(context.providerId(), user.getOrganizationId());
            if (!context.matchesCurrent(provider)) {
                provider.setLastModelSyncAt(syncedAt);
                provider.setLastModelSyncStatus(ModelSyncStatus.STALE);
                provider.setLastModelSyncErrorCode("MODEL_SYNC_STALE");
                provider.setUpdatedBy(user.getUserId());
                provider.setUpdatedAt(syncedAt);
                providerRepository.save(provider);
                return new SyncResultResponse(
                        context.providerId(), 0, 0, 0, 0, true, ModelSyncStatus.STALE, "MODEL_SYNC_STALE", syncedAt);
            }
            provider.setLastModelSyncAt(syncedAt);
            provider.setLastModelSyncStatus(status);
            provider.setLastModelSyncErrorCode(errorCode);
            provider.setLastModelSyncDiscoveredCount(discovered);
            provider.setLastModelSyncCreatedCount(created);
            provider.setLastModelSyncUpdatedCount(updated);
            provider.setLastModelSyncUnchangedCount(unchanged);
            provider.setUpdatedBy(user.getUserId());
            provider.setUpdatedAt(syncedAt);
            providerRepository.save(provider);
            return new SyncResultResponse(
                    context.providerId(),
                    discovered,
                    created,
                    updated,
                    unchanged,
                    status == ModelSyncStatus.STALE,
                    status,
                    errorCode,
                    syncedAt);
        });
    }

    private SyncContext loadContext(UUID providerId, UUID organizationId) {
        AiProvider provider = providerService.requireProvider(providerId, organizationId);
        List<AiModel> existing = modelRepository.findByProviderIdAndOrganizationId(providerId, organizationId);
        Map<String, ModelRowSnapshot> snapshots = new HashMap<>();
        for (AiModel model : existing) {
            snapshots.put(
                    model.getProviderModelId(),
                    new ModelRowSnapshot(model.getId(), model.getVersion(), model.getSource()));
        }
        return SyncContext.from(provider, snapshots);
    }

    private static AiProvider toProbeProvider(SyncContext context) {
        AiProvider provider = new AiProvider();
        provider.setId(context.providerId());
        provider.setOrganizationId(context.organizationId());
        provider.setProviderType(context.providerType());
        provider.setCredentialReference(context.credentialReference());
        provider.setEndpointProfile(context.endpointProfile());
        provider.setAzureResourceName(context.azureResourceName());
        provider.setAzureApiVersion(context.azureApiVersion());
        provider.setRequestTimeoutSeconds(context.timeoutSeconds());
        provider.setVersion(context.providerVersion());
        return provider;
    }

    record ModelRowSnapshot(UUID modelId, Integer version, AiModelSource source) {
    }

    record SyncContext(
            UUID providerId,
            UUID organizationId,
            Integer providerVersion,
            AiProviderType providerType,
            AiProviderStatus providerStatus,
            String credentialReference,
            EndpointProfile endpointProfile,
            String azureResourceName,
            String azureApiVersion,
            ConnectionTestStatus lastConnectionTestStatus,
            int timeoutSeconds,
            Map<String, ModelRowSnapshot> modelSnapshots) {

        static SyncContext from(AiProvider provider, Map<String, ModelRowSnapshot> modelSnapshots) {
            return new SyncContext(
                    provider.getId(),
                    provider.getOrganizationId(),
                    provider.getVersion(),
                    provider.getProviderType(),
                    provider.getStatus(),
                    provider.getCredentialReference(),
                    provider.getEndpointProfile(),
                    provider.getAzureResourceName(),
                    provider.getAzureApiVersion(),
                    provider.getLastConnectionTestStatus(),
                    provider.getRequestTimeoutSeconds() != null ? provider.getRequestTimeoutSeconds() : 30,
                    Map.copyOf(modelSnapshots));
        }

        boolean matchesCurrent(AiProvider provider) {
            return Objects.equals(providerVersion, provider.getVersion())
                    && Objects.equals(credentialReference, provider.getCredentialReference())
                    && Objects.equals(endpointProfile, provider.getEndpointProfile())
                    && Objects.equals(azureResourceName, provider.getAzureResourceName())
                    && Objects.equals(azureApiVersion, provider.getAzureApiVersion())
                    && providerType == provider.getProviderType();
        }
    }
}
