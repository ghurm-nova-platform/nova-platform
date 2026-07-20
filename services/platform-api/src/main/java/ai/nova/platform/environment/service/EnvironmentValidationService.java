package ai.nova.platform.environment.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.deployment.entity.DeploymentEnvironmentEntity;
import ai.nova.platform.deployment.entity.EnvironmentType;
import ai.nova.platform.environment.config.EnvironmentProperties;
import ai.nova.platform.environment.entity.EnvironmentStatus;
import ai.nova.platform.environment.dto.EnvironmentDtos.LabelItem;
import ai.nova.platform.environment.dto.EnvironmentDtos.VariableMetadataItem;
import ai.nova.platform.web.error.ApiException;

@Service
public class EnvironmentValidationService {

    private final EnvironmentProperties properties;

    public EnvironmentValidationService(EnvironmentProperties properties) {
        this.properties = properties;
    }

    public EnvironmentType normalizeType(EnvironmentType type) {
        return type;
    }

    public EnvironmentType normalizeType(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "ENVIRONMENT_INVALID_CONFIGURATION", "Environment type is required");
        }
        if ("TEST".equalsIgnoreCase(typeName.trim())) {
            return EnvironmentType.TESTING;
        }
        try {
            return EnvironmentType.valueOf(typeName.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "ENVIRONMENT_INVALID_CONFIGURATION",
                    "Unknown environment type: " + typeName);
        }
    }

    public void validateCreateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ENVIRONMENT_METADATA_INVALID", "Name is required");
        }
        if (name.trim().length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ENVIRONMENT_METADATA_INVALID", "Name exceeds 100 characters");
        }
    }

    public void validateProductionUniqueness(
            UUID organizationId, UUID projectId, EnvironmentType type, UUID excludeId, boolean existsProduction) {
        if (type != EnvironmentType.PRODUCTION || properties.isAllowMultipleProduction()) {
            return;
        }
        if (existsProduction && excludeId == null) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ENVIRONMENT_DUPLICATE_TYPE",
                    "A PRODUCTION environment already exists for this project");
        }
    }

    public void validateMetadata(List<LabelItem> labels, List<VariableMetadataItem> variables) {
        if (labels != null) {
            for (LabelItem label : labels) {
                if (label.key() == null || label.key().isBlank()) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST, "ENVIRONMENT_METADATA_INVALID", "Label key is required");
                }
                if (label.value() == null || label.value().isBlank()) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST, "ENVIRONMENT_METADATA_INVALID", "Label value is required");
                }
            }
            long distinctKeys = labels.stream().map(l -> l.key().trim().toLowerCase()).distinct().count();
            if (distinctKeys != labels.size()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "ENVIRONMENT_METADATA_INVALID", "Duplicate label keys");
            }
        }
        if (variables != null) {
            for (VariableMetadataItem variable : variables) {
                if (variable.name() == null || variable.name().isBlank()) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST, "ENVIRONMENT_METADATA_INVALID", "Variable name is required");
                }
                if (variable.scope() == null || variable.scope().isBlank()) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST, "ENVIRONMENT_METADATA_INVALID", "Variable scope is required");
                }
            }
            long distinctNames = variables.stream().map(v -> v.name().trim().toLowerCase()).distinct().count();
            if (distinctNames != variables.size()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "ENVIRONMENT_METADATA_INVALID", "Duplicate variable names");
            }
        }
    }

    public void validateStatusTransition(EnvironmentStatus current, EnvironmentStatus target) {
        if (current == target) {
            return;
        }
        if (current == EnvironmentStatus.ARCHIVED) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "ENVIRONMENT_INVALID_STATUS", "Archived environments cannot change status");
        }
        if (target == EnvironmentStatus.ARCHIVED) {
            return;
        }
        boolean valid = switch (current) {
            case ACTIVE -> target == EnvironmentStatus.DISABLED || target == EnvironmentStatus.MAINTENANCE;
            case DISABLED -> target == EnvironmentStatus.ACTIVE;
            case MAINTENANCE -> target == EnvironmentStatus.ACTIVE;
            case ARCHIVED -> false;
        };
        if (!valid) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ENVIRONMENT_INVALID_STATUS",
                    "Invalid status transition from " + current + " to " + target);
        }
    }

    public void validateManaged(DeploymentEnvironmentEntity entity) {
        if (!entity.isProjectScoped()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ENVIRONMENT_INVALID_CONFIGURATION",
                    "Global catalog environments are read-only");
        }
    }

    public Map<String, String> normalizeTags(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Map.of();
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ENVIRONMENT_METADATA_INVALID", "Tag key is required");
            }
            normalized.put(entry.getKey().trim(), entry.getValue() == null ? "" : entry.getValue().trim());
        }
        return normalized;
    }
}
