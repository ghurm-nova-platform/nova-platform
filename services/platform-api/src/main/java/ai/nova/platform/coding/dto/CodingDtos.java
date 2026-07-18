package ai.nova.platform.coding.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import ai.nova.platform.coding.entity.ArtifactLanguage;
import ai.nova.platform.coding.entity.ArtifactType;

public final class CodingDtos {

    private CodingDtos() {
    }

    public record CodeGenerationRequest(@NotNull UUID taskId) {
    }

    public record CodeGenerationResponse(CodingResult result) {
    }

    public record CodingTask(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID runId,
            String taskKey,
            String displayName,
            String description,
            String taskType,
            String status,
            String inputJson,
            String modelReference,
            UUID assignedAgentId) {
    }

    public record GeneratedArtifactDraft(
            ArtifactType type,
            ArtifactLanguage language,
            String path,
            String filename,
            String content) {
    }

    public record CodingResult(
            UUID taskId,
            UUID runId,
            UUID projectId,
            String summary,
            List<GeneratedArtifactResponse> artifacts,
            Long tokensUsed,
            String model,
            String provider,
            Long generationTimeMs,
            boolean validated) {
    }

    public record GeneratedArtifactResponse(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID runId,
            UUID taskId,
            ArtifactType artifactType,
            ArtifactLanguage language,
            String path,
            String filename,
            String content,
            String sha256,
            Long tokensUsed,
            String model,
            String provider,
            Long generationTimeMs,
            Instant createdAt) {
    }

    public record ParsedCodingOutput(String summary, List<GeneratedArtifactDraft> artifacts) {
    }

    public record CodingPromptContext(
            CodingTask task,
            String objective,
            List<DependencySummary> dependencies,
            Map<String, String> organizationSettings,
            Map<String, String> projectSettings) {
    }

    public record DependencySummary(
            UUID taskId, String taskKey, String displayName, String status, String outputPreview) {
    }
}
