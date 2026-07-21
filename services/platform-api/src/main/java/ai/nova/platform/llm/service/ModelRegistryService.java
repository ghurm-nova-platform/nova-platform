package ai.nova.platform.llm.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.llm.dto.LlmDtos.ModelView;
import ai.nova.platform.llm.dto.LlmDtos.RegisterModelRequest;
import ai.nova.platform.llm.dto.LlmDtos.UpdateModelRequest;
import ai.nova.platform.llm.entity.LlmModelEntity;
import ai.nova.platform.llm.entity.LlmModelStatus;
import ai.nova.platform.llm.entity.LlmModelVersionEntity;
import ai.nova.platform.llm.error.LlmErrorCodes;
import ai.nova.platform.llm.mapper.LlmMapper;
import ai.nova.platform.llm.repository.LlmModelRepository;
import ai.nova.platform.llm.repository.LlmModelVersionRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class ModelRegistryService {

    private final LlmModelRepository modelRepository;
    private final LlmModelVersionRepository versionRepository;
    private final LlmMapper mapper;
    private final LlmAuditService auditService;

    public ModelRegistryService(
            LlmModelRepository modelRepository,
            LlmModelVersionRepository versionRepository,
            LlmMapper mapper,
            LlmAuditService auditService) {
        this.modelRepository = modelRepository;
        this.versionRepository = versionRepository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ModelView> list(UUID organizationId) {
        return modelRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(mapper::toModelView)
                .toList();
    }

    @Transactional(readOnly = true)
    public ModelView get(UUID organizationId, UUID modelId) {
        return mapper.toModelView(require(organizationId, modelId));
    }

    @Transactional(readOnly = true)
    public LlmModelEntity requireEntity(UUID organizationId, UUID modelId) {
        return require(organizationId, modelId);
    }

    @Transactional(readOnly = true)
    public LlmModelEntity requireByCode(UUID organizationId, String code) {
        return modelRepository
                .findByOrganizationIdAndCode(organizationId, code)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, LlmErrorCodes.NOT_FOUND, "Model not found: " + code));
    }

    @Transactional
    public ModelView register(RegisterModelRequest request, AuthenticatedUser user) {
        if (modelRepository.existsByOrganizationIdAndCode(user.getOrganizationId(), request.code())) {
            throw new ApiException(HttpStatus.CONFLICT, LlmErrorCodes.CONFLICT, "Model code already exists");
        }
        Instant now = Instant.now();
        LlmModelEntity entity = new LlmModelEntity(
                UUID.randomUUID(),
                user.getOrganizationId(),
                request.code().trim(),
                request.displayName().trim(),
                request.family(),
                request.providerType(),
                LlmModelStatus.REGISTERED,
                now);
        if (request.contextLength() != null) {
            entity.setContextLength(request.contextLength());
        }
        entity.setEndpointUrl(request.endpointUrl());
        entity.setOwner(request.owner());
        if (request.capabilitiesJson() != null) {
            entity.setCapabilitiesJson(request.capabilitiesJson());
        }
        if (request.tagsJson() != null) {
            entity.setTagsJson(request.tagsJson());
        }
        modelRepository.save(entity);
        versionRepository.save(new LlmModelVersionEntity(
                UUID.randomUUID(),
                entity.getId(),
                "1.0.0",
                "local://" + entity.getCode(),
                true,
                now));
        auditService.record(
                user, entity.getId(), entity.getCode(), AuditAction.CREATE, AuditResult.SUCCESS, Map.of());
        return mapper.toModelView(entity);
    }

    @Transactional
    public ModelView update(UUID modelId, UpdateModelRequest request, AuthenticatedUser user) {
        LlmModelEntity entity = require(user.getOrganizationId(), modelId);
        if (request.displayName() != null) {
            entity.setDisplayName(request.displayName());
        }
        if (request.family() != null) {
            entity.setFamily(request.family());
        }
        if (request.contextLength() != null) {
            entity.setContextLength(request.contextLength());
        }
        if (request.endpointUrl() != null) {
            entity.setEndpointUrl(request.endpointUrl());
        }
        if (request.owner() != null) {
            entity.setOwner(request.owner());
        }
        if (request.capabilitiesJson() != null) {
            entity.setCapabilitiesJson(request.capabilitiesJson());
        }
        if (request.tagsJson() != null) {
            entity.setTagsJson(request.tagsJson());
        }
        entity.touch(Instant.now());
        auditService.record(
                user, entity.getId(), entity.getCode(), AuditAction.UPDATE, AuditResult.SUCCESS, Map.of());
        return mapper.toModelView(entity);
    }

    @Transactional
    public ModelView enable(UUID modelId, AuthenticatedUser user) {
        LlmModelEntity entity = require(user.getOrganizationId(), modelId);
        entity.setEnabled(true);
        if (entity.getStatus() == LlmModelStatus.DISABLED) {
            entity.setStatus(LlmModelStatus.REGISTERED);
        }
        entity.touch(Instant.now());
        auditService.record(
                user, entity.getId(), entity.getCode(), AuditAction.ENABLE, AuditResult.SUCCESS, Map.of());
        return mapper.toModelView(entity);
    }

    @Transactional
    public ModelView disable(UUID modelId, AuthenticatedUser user) {
        LlmModelEntity entity = require(user.getOrganizationId(), modelId);
        entity.setEnabled(false);
        entity.setStatus(LlmModelStatus.DISABLED);
        entity.touch(Instant.now());
        auditService.record(
                user, entity.getId(), entity.getCode(), AuditAction.DISABLE, AuditResult.SUCCESS, Map.of());
        return mapper.toModelView(entity);
    }

    @Transactional
    public void delete(UUID modelId, AuthenticatedUser user) {
        LlmModelEntity entity = require(user.getOrganizationId(), modelId);
        modelRepository.delete(entity);
        auditService.record(
                user, entity.getId(), entity.getCode(), AuditAction.DELETE, AuditResult.SUCCESS, Map.of());
    }

    private LlmModelEntity require(UUID organizationId, UUID modelId) {
        return modelRepository
                .findByIdAndOrganizationId(modelId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, LlmErrorCodes.NOT_FOUND, "Model not found"));
    }
}
