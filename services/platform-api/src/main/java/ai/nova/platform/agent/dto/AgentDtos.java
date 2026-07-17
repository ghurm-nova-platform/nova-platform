package ai.nova.platform.agent.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import ai.nova.platform.agent.entity.AgentStatus;
import ai.nova.platform.agent.entity.AgentVisibility;

public final class AgentDtos {

    private AgentDtos() {
    }

    public record AgentCreateRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 2000) String description,
            @NotBlank @Size(max = 20000) String systemPrompt,
            @NotBlank @Size(max = 64) String modelProvider,
            @NotBlank @Size(max = 128) String modelName,
            @NotNull @DecimalMin("0.0") @DecimalMax("2.0") BigDecimal temperature,
            @Positive Integer maxTokens,
            @NotNull AgentVisibility visibility) {
    }

    public record AgentUpdateRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 2000) String description,
            @NotBlank @Size(max = 20000) String systemPrompt,
            @NotBlank @Size(max = 64) String modelProvider,
            @NotBlank @Size(max = 128) String modelName,
            @NotNull @DecimalMin("0.0") @DecimalMax("2.0") BigDecimal temperature,
            @Positive Integer maxTokens,
            @NotNull AgentVisibility visibility,
            @NotNull Integer version) {
    }

    public record AgentStatusRequest(@NotNull AgentStatus status, @NotNull Integer version) {
    }

    public record AgentResponse(
            UUID id,
            UUID organizationId,
            UUID projectId,
            String name,
            String description,
            String systemPrompt,
            String modelProvider,
            String modelName,
            BigDecimal temperature,
            Integer maxTokens,
            AgentStatus status,
            AgentVisibility visibility,
            Integer version,
            UUID createdBy,
            UUID updatedBy,
            Instant createdAt,
            Instant updatedAt) {
    }
}
