package ai.nova.platform.environment.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.deployment.entity.DeploymentEnvironmentEntity;
import ai.nova.platform.deployment.entity.EnvironmentType;
import ai.nova.platform.deployment.repository.DeploymentEnvironmentRepository;
import ai.nova.platform.environment.config.EnvironmentProperties;
import ai.nova.platform.environment.dto.EnvironmentDtos.CreateEnvironmentRequest;
import ai.nova.platform.environment.dto.EnvironmentDtos.Environment;
import ai.nova.platform.environment.dto.EnvironmentDtos.HistoryEntry;
import ai.nova.platform.environment.dto.EnvironmentDtos.LabelItem;
import ai.nova.platform.environment.dto.EnvironmentDtos.LabelView;
import ai.nova.platform.environment.dto.EnvironmentDtos.TimelineEvent;
import ai.nova.platform.environment.dto.EnvironmentDtos.UpdateEnvironmentRequest;
import ai.nova.platform.environment.dto.EnvironmentDtos.VariableMetadataItem;
import ai.nova.platform.environment.dto.EnvironmentDtos.VariableMetadataView;
import ai.nova.platform.environment.entity.EnvironmentEventEntity;
import ai.nova.platform.environment.entity.EnvironmentEventType;
import ai.nova.platform.environment.entity.EnvironmentHistoryEntity;
import ai.nova.platform.environment.entity.EnvironmentLabelEntity;
import ai.nova.platform.environment.entity.EnvironmentStatus;
import ai.nova.platform.environment.entity.EnvironmentVariableMetadataEntity;
import ai.nova.platform.environment.repository.EnvironmentEventRepository;
import ai.nova.platform.environment.repository.EnvironmentHistoryRepository;
import ai.nova.platform.environment.repository.EnvironmentLabelRepository;
import ai.nova.platform.environment.repository.EnvironmentVariableMetadataRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class EnvironmentStorageService {

    private static final TypeReference<Map<String, String>> TAGS_TYPE = new TypeReference<>() {};

    private final EnvironmentProperties properties;
    private final DeploymentEnvironmentRepository environmentRepository;
    private final EnvironmentLabelRepository labelRepository;
    private final EnvironmentVariableMetadataRepository variableRepository;
    private final EnvironmentEventRepository eventRepository;
    private final EnvironmentHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    public EnvironmentStorageService(
            EnvironmentProperties properties,
            DeploymentEnvironmentRepository environmentRepository,
            EnvironmentLabelRepository labelRepository,
            EnvironmentVariableMetadataRepository variableRepository,
            EnvironmentEventRepository eventRepository,
            EnvironmentHistoryRepository historyRepository,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.environmentRepository = environmentRepository;
        this.labelRepository = labelRepository;
        this.variableRepository = variableRepository;
        this.eventRepository = eventRepository;
        this.historyRepository = historyRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DeploymentEnvironmentEntity createEnvironment(
            UUID organizationId,
            CreateEnvironmentRequest request,
            EnvironmentType environmentType,
            UUID createdBy,
            Instant now) {
        UUID id = UUID.randomUUID();
        String code = generateCode(request.projectId(), request.name());
        DeploymentEnvironmentEntity entity = new DeploymentEnvironmentEntity(
                id, code, request.name().trim(), environmentType, nextSortOrder(organizationId, request.projectId()), true, now);
        entity.setOrganizationId(organizationId);
        entity.setProjectId(request.projectId());
        entity.setDescription(truncate(request.description(), 2000));
        entity.setStatus(EnvironmentStatus.ACTIVE);
        entity.setRegion(truncate(request.region(), 80));
        entity.setProvider(truncate(request.provider(), 80));
        entity.setClusterName(truncate(request.clusterName(), 120));
        entity.setNamespaceName(truncate(request.namespaceName(), 120));
        entity.setCloudProvider(truncate(request.cloudProvider(), 80));
        entity.setPlatform(truncate(request.platform(), 80));
        entity.setOwnerName(truncate(request.ownerName(), 120));
        entity.setBusinessUnit(truncate(request.businessUnit(), 120));
        entity.setCostCenter(truncate(request.costCenter(), 80));
        entity.setTagsJson(toTagsJson(request.tags()));
        entity.setCreatedBy(createdBy);
        entity.setUpdatedAt(now);
        environmentRepository.save(entity);
        replaceLabels(id, request.labels(), now);
        replaceVariables(id, request.variables(), now);
        appendEvent(id, EnvironmentEventType.CREATED, "Environment created", now);
        appendEvent(id, EnvironmentEventType.ENABLED, "Environment active on create", now);
        recordHistory(id, "CREATED", entity, createdBy, now);
        return entity;
    }

    @Transactional
    public DeploymentEnvironmentEntity updateEnvironment(
            DeploymentEnvironmentEntity entity, UpdateEnvironmentRequest request, UUID updatedBy, Instant now) {
        if (request.name() != null && !request.name().isBlank()) {
            entity.setName(request.name().trim());
        }
        if (request.description() != null) {
            entity.setDescription(truncate(request.description(), 2000));
        }
        if (request.region() != null) {
            entity.setRegion(truncate(request.region(), 80));
        }
        if (request.provider() != null) {
            entity.setProvider(truncate(request.provider(), 80));
        }
        if (request.clusterName() != null) {
            entity.setClusterName(truncate(request.clusterName(), 120));
        }
        if (request.namespaceName() != null) {
            entity.setNamespaceName(truncate(request.namespaceName(), 120));
        }
        if (request.cloudProvider() != null) {
            entity.setCloudProvider(truncate(request.cloudProvider(), 80));
        }
        if (request.platform() != null) {
            entity.setPlatform(truncate(request.platform(), 80));
        }
        if (request.ownerName() != null) {
            entity.setOwnerName(truncate(request.ownerName(), 120));
        }
        if (request.businessUnit() != null) {
            entity.setBusinessUnit(truncate(request.businessUnit(), 120));
        }
        if (request.costCenter() != null) {
            entity.setCostCenter(truncate(request.costCenter(), 80));
        }
        if (request.tags() != null) {
            entity.setTagsJson(toTagsJson(request.tags()));
        }
        entity.setUpdatedAt(now);
        environmentRepository.save(entity);
        if (request.labels() != null) {
            replaceLabels(entity.getId(), request.labels(), now);
        }
        if (request.variables() != null) {
            replaceVariables(entity.getId(), request.variables(), now);
        }
        appendEvent(entity.getId(), EnvironmentEventType.UPDATED, "Environment metadata updated", now);
        recordHistory(entity.getId(), "UPDATED", entity, updatedBy, now);
        return entity;
    }

    @Transactional
    public DeploymentEnvironmentEntity applyStatus(
            DeploymentEnvironmentEntity entity, EnvironmentStatus status, EnvironmentEventType eventType, UUID actor, Instant now) {
        entity.setStatus(status);
        entity.setActive(status.isActiveFlag());
        entity.setUpdatedAt(now);
        environmentRepository.save(entity);
        appendEvent(entity.getId(), eventType, "Status changed to " + status, now);
        recordHistory(entity.getId(), "STATUS_CHANGED", entity, actor, now);
        return entity;
    }

    @Transactional
    public void appendEvent(UUID environmentId, EnvironmentEventType type, String detail, Instant at) {
        eventRepository.save(new EnvironmentEventEntity(
                UUID.randomUUID(), environmentId, type, truncate(detail, 2000), at));
    }

    public DeploymentEnvironmentEntity requireManagedForOrg(UUID id, UUID organizationId) {
        DeploymentEnvironmentEntity entity = environmentRepository
                .findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ENVIRONMENT_NOT_FOUND", "Environment not found"));
        if (!entity.isProjectScoped()) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND, "ENVIRONMENT_NOT_FOUND", "Environment not found for organization");
        }
        return entity;
    }

    public Environment toDto(DeploymentEnvironmentEntity entity) {
        return toDto(entity, false, false);
    }

    public Environment toDto(DeploymentEnvironmentEntity entity, boolean includeTimeline, boolean includeHistory) {
        List<LabelView> labels = labelRepository.findByEnvironmentIdOrderByLabelKeyAsc(entity.getId()).stream()
                .map(l -> new LabelView(l.getLabelKey(), l.getLabelValue(), l.getCreatedAt()))
                .toList();
        List<VariableMetadataView> variables =
                variableRepository.findByEnvironmentIdOrderByVariableNameAsc(entity.getId()).stream()
                        .map(v -> new VariableMetadataView(
                                v.getId(),
                                v.getVariableName(),
                                v.getDescription(),
                                v.isRequired(),
                                v.isMasked(),
                                v.getScope(),
                                v.getCreatedAt(),
                                v.getUpdatedAt()))
                        .toList();
        List<TimelineEvent> timeline = includeTimeline
                ? eventRepository.findByEnvironmentIdOrderByCreatedAtAsc(entity.getId()).stream()
                        .map(e -> new TimelineEvent(e.getEventType().name(), e.getCreatedAt(), e.getDetail()))
                        .toList()
                : List.of();
        List<HistoryEntry> history = includeHistory && properties.isRetainHistory()
                ? historyRepository.findByEnvironmentIdOrderByCreatedAtDesc(entity.getId()).stream()
                        .map(h -> new HistoryEntry(
                                h.getId(), h.getChangeType(), h.getSnapshotJson(), h.getCreatedBy(), h.getCreatedAt()))
                        .toList()
                : List.of();

        return new Environment(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getProjectId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getEnvironmentType(),
                entity.getStatus(),
                entity.isActive(),
                entity.getRegion(),
                entity.getProvider(),
                entity.getClusterName(),
                entity.getNamespaceName(),
                entity.getCloudProvider(),
                entity.getPlatform(),
                entity.getOwnerName(),
                entity.getBusinessUnit(),
                entity.getCostCenter(),
                parseTags(entity.getTagsJson()),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                labels,
                variables,
                timeline,
                history);
    }

    private void recordHistory(UUID environmentId, String changeType, DeploymentEnvironmentEntity entity, UUID actor, Instant at) {
        if (!properties.isRetainHistory()) {
            return;
        }
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("id", entity.getId());
            snapshot.put("name", entity.getName());
            snapshot.put("status", entity.getStatus().name());
            snapshot.put("environmentType", entity.getEnvironmentType().name());
            snapshot.put("description", entity.getDescription());
            snapshot.put("region", entity.getRegion());
            snapshot.put("provider", entity.getProvider());
            snapshot.put("clusterName", entity.getClusterName());
            snapshot.put("namespaceName", entity.getNamespaceName());
            snapshot.put("cloudProvider", entity.getCloudProvider());
            snapshot.put("platform", entity.getPlatform());
            snapshot.put("ownerName", entity.getOwnerName());
            snapshot.put("businessUnit", entity.getBusinessUnit());
            snapshot.put("costCenter", entity.getCostCenter());
            snapshot.put("tags", parseTags(entity.getTagsJson()));
            String json = objectMapper.writeValueAsString(snapshot);
            historyRepository.save(new EnvironmentHistoryEntity(
                    UUID.randomUUID(), environmentId, changeType, json, actor, at));
        } catch (JsonProcessingException ex) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "ENVIRONMENT_METADATA_INVALID", "Failed to record history");
        }
    }

    private void replaceLabels(UUID environmentId, List<LabelItem> labels, Instant now) {
        if (labels == null) {
            return;
        }
        labelRepository.deleteByEnvironmentId(environmentId);
        for (LabelItem label : labels) {
            labelRepository.save(new EnvironmentLabelEntity(
                    UUID.randomUUID(),
                    environmentId,
                    label.key().trim(),
                    label.value().trim(),
                    now));
        }
    }

    private void replaceVariables(UUID environmentId, List<VariableMetadataItem> variables, Instant now) {
        if (variables == null) {
            return;
        }
        variableRepository.deleteByEnvironmentId(environmentId);
        for (VariableMetadataItem variable : variables) {
            variableRepository.save(new EnvironmentVariableMetadataEntity(
                    UUID.randomUUID(),
                    environmentId,
                    variable.name().trim(),
                    truncate(variable.description(), 2000),
                    variable.required(),
                    variable.masked(),
                    variable.scope().trim(),
                    now,
                    now));
        }
    }

    private int nextSortOrder(UUID organizationId, UUID projectId) {
        return environmentRepository
                        .findByOrganizationIdAndProjectIdOrderBySortOrderAscCreatedAtDesc(organizationId, projectId)
                        .size()
                * 10
                + 100;
    }

    static String generateCode(UUID projectId, String name) {
        String base = name.toUpperCase().replaceAll("[^A-Z0-9]", "_").replaceAll("_+", "_");
        if (base.startsWith("_")) {
            base = base.substring(1);
        }
        if (base.endsWith("_")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isEmpty()) {
            base = "ENV";
        }
        if (base.length() > 24) {
            base = base.substring(0, 24);
        }
        return base + "_" + projectId.toString().substring(0, 8).toUpperCase();
    }

    private String toTagsJson(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ENVIRONMENT_METADATA_INVALID", "Invalid tags");
        }
    }

    private Map<String, String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(tagsJson, TAGS_TYPE);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
