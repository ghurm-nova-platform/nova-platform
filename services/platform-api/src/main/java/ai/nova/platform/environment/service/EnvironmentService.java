package ai.nova.platform.environment.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.deployment.entity.DeploymentEnvironmentEntity;
import ai.nova.platform.deployment.entity.EnvironmentType;
import ai.nova.platform.deployment.repository.DeploymentEnvironmentRepository;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSource;
import ai.nova.platform.audit.service.AuditRecordingSupport;
import ai.nova.platform.environment.config.EnvironmentProperties;
import ai.nova.platform.environment.dto.EnvironmentDtos.CreateEnvironmentRequest;
import ai.nova.platform.environment.dto.EnvironmentDtos.Environment;
import ai.nova.platform.environment.dto.EnvironmentDtos.UpdateEnvironmentRequest;
import ai.nova.platform.environment.entity.EnvironmentEventType;
import ai.nova.platform.environment.entity.EnvironmentStatus;
import ai.nova.platform.environment.security.EnvironmentAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class EnvironmentService {

    private final EnvironmentProperties properties;
    private final EnvironmentAuthorizationService authorizationService;
    private final EnvironmentStorageService storageService;
    private final EnvironmentValidationService validationService;
    private final DeploymentEnvironmentRepository environmentRepository;
    private final AuditRecordingSupport auditRecordingSupport;

    public EnvironmentService(
            EnvironmentProperties properties,
            EnvironmentAuthorizationService authorizationService,
            EnvironmentStorageService storageService,
            EnvironmentValidationService validationService,
            DeploymentEnvironmentRepository environmentRepository,
            AuditRecordingSupport auditRecordingSupport) {
        this.properties = properties;
        this.authorizationService = authorizationService;
        this.storageService = storageService;
        this.validationService = validationService;
        this.environmentRepository = environmentRepository;
        this.auditRecordingSupport = auditRecordingSupport;
    }

    @Transactional
    public Environment create(CreateEnvironmentRequest request, AuthenticatedUser user) {
        authorizationService.require(user, EnvironmentAuthorizationService.ENVIRONMENT_RUN);
        requireEnabled();

        validationService.validateCreateName(request.name());
        EnvironmentType environmentType = validationService.normalizeType(request.environmentType());
        Map<String, String> tags = validationService.normalizeTags(request.tags());
        validationService.validateMetadata(request.labels(), request.variables());

        var existing = environmentRepository.findByOrganizationIdAndProjectIdAndNameIgnoreCase(
                user.getOrganizationId(), request.projectId(), request.name().trim());
        if (existing.isPresent()) {
            storageService.appendEvent(
                    existing.get().getId(),
                    EnvironmentEventType.IDEMPOTENT_RETURN,
                    "Environment with same name already exists",
                    Instant.now());
            return storageService.toDto(existing.get(), true, false);
        }

        boolean productionExists = environmentRepository.existsByOrganizationIdAndProjectIdAndEnvironmentType(
                user.getOrganizationId(), request.projectId(), EnvironmentType.PRODUCTION);
        validationService.validateProductionUniqueness(
                user.getOrganizationId(), request.projectId(), environmentType, null, productionExists);

        try {
            CreateEnvironmentRequest normalized = new CreateEnvironmentRequest(
                    request.projectId(),
                    request.name(),
                    request.description(),
                    environmentType,
                    request.region(),
                    request.provider(),
                    request.clusterName(),
                    request.namespaceName(),
                    request.cloudProvider(),
                    request.platform(),
                    request.ownerName(),
                    request.businessUnit(),
                    request.costCenter(),
                    tags,
                    request.labels(),
                    request.variables());
            DeploymentEnvironmentEntity created = storageService.createEnvironment(
                    user.getOrganizationId(), normalized, environmentType, user.getUserId(), Instant.now());
            publishAudit(user, created.getProjectId(), created.getId(), created.getName(), AuditAction.CREATE);
            return storageService.toDto(created, true, false);
        } catch (DataIntegrityViolationException ex) {
            if (environmentType == EnvironmentType.PRODUCTION) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "ENVIRONMENT_DUPLICATE_TYPE",
                        "A PRODUCTION environment already exists for this project");
            }
            throw new ApiException(
                    HttpStatus.CONFLICT, "ENVIRONMENT_ALREADY_EXISTS", "An environment with the same name already exists");
        }
    }

    @Transactional
    public Environment update(UUID id, UpdateEnvironmentRequest request, AuthenticatedUser user) {
        authorizationService.require(user, EnvironmentAuthorizationService.ENVIRONMENT_RUN);
        requireEnabled();

        DeploymentEnvironmentEntity entity = storageService.requireManagedForOrg(id, user.getOrganizationId());
        validationService.validateManaged(entity);
        if (entity.getStatus() == EnvironmentStatus.ARCHIVED) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "ENVIRONMENT_INVALID_STATUS", "Archived environments cannot be updated");
        }

        if (request.name() != null && !request.name().equalsIgnoreCase(entity.getName())) {
            environmentRepository
                    .findByOrganizationIdAndProjectIdAndNameIgnoreCase(
                            user.getOrganizationId(), entity.getProjectId(), request.name().trim())
                    .filter(other -> !other.getId().equals(entity.getId()))
                    .ifPresent(other -> {
                        throw new ApiException(
                                HttpStatus.CONFLICT, "ENVIRONMENT_DUPLICATE_NAME", "Environment name already in use");
                    });
        }

        Map<String, String> tags = request.tags() != null ? validationService.normalizeTags(request.tags()) : null;
        validationService.validateMetadata(request.labels(), request.variables());

        UpdateEnvironmentRequest normalized = new UpdateEnvironmentRequest(
                request.name(),
                request.description(),
                request.region(),
                request.provider(),
                request.clusterName(),
                request.namespaceName(),
                request.cloudProvider(),
                request.platform(),
                request.ownerName(),
                request.businessUnit(),
                request.costCenter(),
                tags,
                request.labels(),
                request.variables());
        DeploymentEnvironmentEntity updated =
                storageService.updateEnvironment(entity, normalized, user.getUserId(), Instant.now());
        publishAudit(user, updated.getProjectId(), updated.getId(), updated.getName(), AuditAction.UPDATE);
        return storageService.toDto(updated, true, false);
    }

    @Transactional
    public Environment enable(UUID id, AuthenticatedUser user) {
        return transitionStatus(id, user, EnvironmentStatus.ACTIVE, EnvironmentEventType.ENABLED);
    }

    @Transactional
    public Environment disable(UUID id, AuthenticatedUser user) {
        return transitionStatus(id, user, EnvironmentStatus.DISABLED, EnvironmentEventType.DISABLED);
    }

    @Transactional
    public Environment archive(UUID id, AuthenticatedUser user) {
        return transitionStatus(id, user, EnvironmentStatus.ARCHIVED, EnvironmentEventType.ARCHIVED);
    }

    @Transactional(readOnly = true)
    public List<Environment> list(UUID projectId, AuthenticatedUser user) {
        authorizationService.require(user, EnvironmentAuthorizationService.ENVIRONMENT_READ);
        requireEnabled();
        if (projectId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ENVIRONMENT_INVALID_CONFIGURATION", "projectId is required");
        }
        return environmentRepository
                .findByOrganizationIdAndProjectIdOrderBySortOrderAscCreatedAtDesc(user.getOrganizationId(), projectId)
                .stream()
                .map(storageService::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Environment get(UUID id, AuthenticatedUser user) {
        authorizationService.require(user, EnvironmentAuthorizationService.ENVIRONMENT_READ);
        requireEnabled();
        DeploymentEnvironmentEntity entity = storageService.requireManagedForOrg(id, user.getOrganizationId());
        return storageService.toDto(entity, true, false);
    }

    @Transactional(readOnly = true)
    public Environment history(UUID id, AuthenticatedUser user) {
        authorizationService.require(user, EnvironmentAuthorizationService.ENVIRONMENT_READ);
        requireEnabled();
        DeploymentEnvironmentEntity entity = storageService.requireManagedForOrg(id, user.getOrganizationId());
        return storageService.toDto(entity, true, true);
    }

    private Environment transitionStatus(
            UUID id, AuthenticatedUser user, EnvironmentStatus target, EnvironmentEventType eventType) {
        authorizationService.require(user, EnvironmentAuthorizationService.ENVIRONMENT_RUN);
        requireEnabled();

        DeploymentEnvironmentEntity entity = storageService.requireManagedForOrg(id, user.getOrganizationId());
        validationService.validateManaged(entity);
        validationService.validateStatusTransition(entity.getStatus(), target);
        DeploymentEnvironmentEntity updated =
                storageService.applyStatus(entity, target, eventType, user.getUserId(), Instant.now());
        AuditAction action = switch (target) {
            case ACTIVE -> AuditAction.ENABLE;
            case DISABLED -> AuditAction.DISABLE;
            case ARCHIVED -> AuditAction.ARCHIVE;
            default -> AuditAction.UPDATE;
        };
        publishAudit(user, updated.getProjectId(), updated.getId(), updated.getName(), action);
        return storageService.toDto(updated, true, false);
    }

    private void publishAudit(
            AuthenticatedUser user, UUID projectId, UUID entityId, String name, AuditAction action) {
        auditRecordingSupport.recordDomainEvent(
                user,
                projectId,
                AuditEntityType.ENVIRONMENT,
                entityId,
                name,
                action,
                AuditResult.SUCCESS,
                AuditSource.ENVIRONMENT_MANAGEMENT,
                java.util.Map.of("environmentId", entityId.toString()));
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE, "ENVIRONMENT_DISABLED", "Environment management is disabled");
        }
    }
}
