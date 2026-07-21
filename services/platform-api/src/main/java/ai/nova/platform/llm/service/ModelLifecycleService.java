package ai.nova.platform.llm.service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.llm.dto.LlmDtos.ModelView;
import ai.nova.platform.llm.entity.LlmModelEntity;
import ai.nova.platform.llm.entity.LlmModelStatus;
import ai.nova.platform.llm.entity.LlmProviderType;
import ai.nova.platform.llm.error.LlmErrorCodes;
import ai.nova.platform.llm.mapper.LlmMapper;
import ai.nova.platform.llm.provider.LlmProviderRegistry;
import ai.nova.platform.llm.provider.LlmRuntimeProvider;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class ModelLifecycleService {

    private final ModelRegistryService modelRegistryService;
    private final LlmMapper mapper;
    private final LlmAuditService auditService;
    private final LlmProviderRegistry providerRegistry;

    public ModelLifecycleService(
            ModelRegistryService modelRegistryService,
            LlmMapper mapper,
            LlmAuditService auditService,
            LlmProviderRegistry providerRegistry) {
        this.modelRegistryService = modelRegistryService;
        this.mapper = mapper;
        this.auditService = auditService;
        this.providerRegistry = providerRegistry;
    }

    @Transactional
    public ModelView install(UUID modelId, AuthenticatedUser user) {
        return transition(modelId, user, LlmModelStatus.INSTALLED, EnumSet.of(
                LlmModelStatus.REGISTERED, LlmModelStatus.DOWNLOADING, LlmModelStatus.ERROR));
    }

    @Transactional
    public ModelView download(UUID modelId, AuthenticatedUser user) {
        return transition(modelId, user, LlmModelStatus.DOWNLOADING, EnumSet.of(
                LlmModelStatus.REGISTERED, LlmModelStatus.ERROR, LlmModelStatus.STOPPED));
    }

    @Transactional
    public ModelView load(UUID modelId, AuthenticatedUser user) {
        return transition(modelId, user, LlmModelStatus.LOADING, EnumSet.of(
                LlmModelStatus.INSTALLED, LlmModelStatus.STOPPED, LlmModelStatus.READY));
    }

    @Transactional
    public ModelView unload(UUID modelId, AuthenticatedUser user) {
        return transition(modelId, user, LlmModelStatus.UNLOADING, EnumSet.of(
                LlmModelStatus.READY, LlmModelStatus.LOADING));
    }

    @Transactional
    public ModelView start(UUID modelId, AuthenticatedUser user) {
        LlmModelEntity entity = modelRegistryService.requireEntity(user.getOrganizationId(), modelId);
        if (entity.getProviderType() == LlmProviderType.DETERMINISTIC) {
            entity.setStatus(LlmModelStatus.READY);
            entity.setEnabled(true);
            entity.touch(Instant.now());
            auditService.record(
                    user, entity.getId(), entity.getCode(), AuditAction.START, AuditResult.SUCCESS, Map.of());
            return mapper.toModelView(entity);
        }
        return transition(modelId, user, LlmModelStatus.READY, EnumSet.of(
                LlmModelStatus.LOADING, LlmModelStatus.INSTALLED, LlmModelStatus.STOPPED, LlmModelStatus.REGISTERED));
    }

    @Transactional
    public ModelView stop(UUID modelId, AuthenticatedUser user) {
        return transition(modelId, user, LlmModelStatus.STOPPED, EnumSet.of(
                LlmModelStatus.READY, LlmModelStatus.LOADING, LlmModelStatus.UNLOADING));
    }

    @Transactional
    public ModelView restart(UUID modelId, AuthenticatedUser user) {
        stop(modelId, user);
        return start(modelId, user);
    }

    @Transactional
    public ModelView warmup(UUID modelId, AuthenticatedUser user) {
        LlmModelEntity entity = modelRegistryService.requireEntity(user.getOrganizationId(), modelId);
        if (entity.getProviderType() == LlmProviderType.DETERMINISTIC) {
            entity.setStatus(LlmModelStatus.READY);
            entity.touch(Instant.now());
            return mapper.toModelView(entity);
        }
        return start(modelId, user);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> health(UUID modelId, AuthenticatedUser user) {
        LlmModelEntity entity = modelRegistryService.requireEntity(user.getOrganizationId(), modelId);
        LlmRuntimeProvider provider = providerRegistry.require(entity.getProviderType());
        LlmRuntimeProvider.ProviderHealth health = provider.health();
        return Map.of(
                "modelId", entity.getId().toString(),
                "modelStatus", entity.getStatus().name(),
                "providerType", entity.getProviderType().name(),
                "providerStatus", health.status().name(),
                "detail", health.detail() == null ? "" : health.detail());
    }

    private ModelView transition(
            UUID modelId, AuthenticatedUser user, LlmModelStatus target, Set<LlmModelStatus> allowedFrom) {
        LlmModelEntity entity = modelRegistryService.requireEntity(user.getOrganizationId(), modelId);
        if (entity.getProviderType() == LlmProviderType.DETERMINISTIC
                && (target == LlmModelStatus.READY
                        || target == LlmModelStatus.INSTALLED
                        || target == LlmModelStatus.LOADING)) {
            entity.setStatus(LlmModelStatus.READY);
            entity.setEnabled(true);
            entity.touch(Instant.now());
            auditService.record(
                    user, entity.getId(), entity.getCode(), AuditAction.UPDATE, AuditResult.SUCCESS, Map.of("status", target));
            return mapper.toModelView(entity);
        }
        if (target == LlmModelStatus.UNLOADING) {
            entity.setStatus(LlmModelStatus.STOPPED);
            entity.touch(Instant.now());
            return mapper.toModelView(entity);
        }
        if (target == LlmModelStatus.LOADING) {
            entity.setStatus(LlmModelStatus.READY);
            entity.touch(Instant.now());
            return mapper.toModelView(entity);
        }
        if (target == LlmModelStatus.DOWNLOADING) {
            entity.setStatus(LlmModelStatus.INSTALLED);
            entity.touch(Instant.now());
            return mapper.toModelView(entity);
        }
        if (!allowedFrom.contains(entity.getStatus()) && entity.getStatus() != target) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    LlmErrorCodes.INVALID_STATE,
                    "Cannot transition from " + entity.getStatus() + " to " + target);
        }
        entity.setStatus(target);
        entity.touch(Instant.now());
        auditService.record(
                user, entity.getId(), entity.getCode(), AuditAction.UPDATE, AuditResult.SUCCESS, Map.of("status", target));
        return mapper.toModelView(entity);
    }
}
