package ai.nova.platform.environment.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import ai.nova.platform.deployment.entity.EnvironmentType;
import ai.nova.platform.environment.entity.EnvironmentStatus;

public final class EnvironmentDtos {

    private EnvironmentDtos() {
    }

    public record LabelItem(@NotBlank @Size(max = 120) String key, @NotBlank @Size(max = 500) String value) {
    }

    public record VariableMetadataItem(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description,
            boolean required,
            boolean masked,
            @NotBlank @Size(max = 60) String scope) {
    }

    public record CreateEnvironmentRequest(
            @NotNull UUID projectId,
            @NotBlank @Size(max = 100) String name,
            @Size(max = 2000) String description,
            @NotNull EnvironmentType environmentType,
            @Size(max = 80) String region,
            @Size(max = 80) String provider,
            @Size(max = 120) String clusterName,
            @Size(max = 120) String namespaceName,
            @Size(max = 80) String cloudProvider,
            @Size(max = 80) String platform,
            @Size(max = 120) String ownerName,
            @Size(max = 120) String businessUnit,
            @Size(max = 80) String costCenter,
            Map<String, String> tags,
            @Valid List<LabelItem> labels,
            @Valid List<VariableMetadataItem> variables) {
    }

    public record UpdateEnvironmentRequest(
            @Size(max = 100) String name,
            @Size(max = 2000) String description,
            @Size(max = 80) String region,
            @Size(max = 80) String provider,
            @Size(max = 120) String clusterName,
            @Size(max = 120) String namespaceName,
            @Size(max = 80) String cloudProvider,
            @Size(max = 80) String platform,
            @Size(max = 120) String ownerName,
            @Size(max = 120) String businessUnit,
            @Size(max = 80) String costCenter,
            Map<String, String> tags,
            @Valid List<LabelItem> labels,
            @Valid List<VariableMetadataItem> variables) {
    }

    public record TimelineEvent(String eventType, Instant at, String detail) {
    }

    public record HistoryEntry(
            UUID id, String changeType, String snapshotJson, UUID createdBy, Instant createdAt) {
    }

    public record LabelView(String key, String value, Instant createdAt) {
    }

    public record VariableMetadataView(
            UUID id,
            String name,
            String description,
            boolean required,
            boolean masked,
            String scope,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record Environment(
            UUID id,
            UUID organizationId,
            UUID projectId,
            String code,
            String name,
            String description,
            EnvironmentType environmentType,
            EnvironmentStatus status,
            boolean active,
            String region,
            String provider,
            String clusterName,
            String namespaceName,
            String cloudProvider,
            String platform,
            String ownerName,
            String businessUnit,
            String costCenter,
            Map<String, String> tags,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt,
            List<LabelView> labels,
            List<VariableMetadataView> variables,
            List<TimelineEvent> timeline,
            List<HistoryEntry> history) {
    }
}
