package ai.nova.platform.planner.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import ai.nova.platform.orchestration.dto.OrchestrationDtos.RunResponse;
import ai.nova.platform.orchestration.entity.DependencyType;
import ai.nova.platform.orchestration.entity.ExecutionMode;
import ai.nova.platform.orchestration.entity.FailurePolicy;
import ai.nova.platform.orchestration.entity.TaskType;
import ai.nova.platform.planner.entity.PlannerComplexity;
import ai.nova.platform.planner.entity.PlannerRiskLevel;
import ai.nova.platform.planner.entity.PlannerTaskClassification;

public final class PlannerDtos {

    private PlannerDtos() {
    }

    public record PlannerRequest(
            @NotNull UUID projectId,
            @NotBlank @Size(max = 4000) String objective,
            @Size(max = 255) String runName,
            UUID templateId,
            UUID plannerAgentId,
            @Size(max = 100000) String metadataJson,
            Map<String, Object> metadata) {
    }

    public record ExecutionTaskDefinition(
            @NotBlank @Size(max = 150) String taskKey,
            @NotBlank @Size(max = 255) String displayName,
            @Size(max = 2000) String description,
            @NotNull TaskType taskType,
            @NotBlank @Size(max = 80) String agentRole,
            PlannerTaskClassification classification,
            Integer priority,
            Integer sequenceOrder,
            UUID assignedAgentId,
            @Size(max = 150) String modelReference,
            @Size(max = 100000) String inputJson) {
    }

    public record ExecutionDependency(
            @NotBlank String from,
            @NotBlank String to,
            @NotNull DependencyType type) {
    }

    public record ExecutionEstimate(
            PlannerComplexity complexity,
            PlannerRiskLevel riskLevel,
            long estimatedTokens,
            long estimatedDurationSeconds,
            double estimatedCostUsd,
            String notes) {
    }

    public record ExecutionPlan(
            String objective,
            ExecutionMode executionMode,
            FailurePolicy failurePolicy,
            Integer maxParallelTasks,
            Long maximumDurationMs,
            PlannerComplexity estimatedComplexity,
            Long estimatedTokens,
            Long estimatedDurationSeconds,
            Double estimatedCostUsd,
            PlannerRiskLevel riskLevel,
            List<ExecutionTaskDefinition> tasks,
            List<ExecutionDependency> dependencies,
            Map<String, Object> metadata) {
    }

    public record PlannerResponse(
            ExecutionPlan plan,
            ExecutionEstimate estimate,
            String rawPlannerOutput,
            UUID templateId,
            boolean validated) {
    }

    public record PlanAndCreateResponse(
            PlannerResponse planner,
            RunResponse draftRun) {
    }

    public record ImportPlanRequest(
            @NotNull UUID projectId,
            @NotBlank @Size(max = 255) String runName,
            @NotNull @Valid ExecutionPlan plan) {
    }
}
