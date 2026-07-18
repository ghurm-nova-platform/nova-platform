package ai.nova.platform.patch.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.patch.entity.PatchChangeType;
import ai.nova.platform.patch.entity.PatchStatus;
import ai.nova.platform.review.dto.ReviewDtos.ReviewFinding;
import ai.nova.platform.testing.dto.TestingDtos.GeneratedTest;

public final class PatchDtos {

    private PatchDtos() {
    }

    public record PatchRunRequest(@NotNull UUID taskId) {
    }

    public record ParsedPatchOutput(
            String summary,
            Integer filesChanged,
            Integer insertions,
            Integer deletions,
            String patch,
            PatchStatus status) {
    }

    public record PatchFile(
            UUID id,
            String path,
            String oldPath,
            String newPath,
            PatchChangeType changeType,
            int insertions,
            int deletions,
            String patchExcerpt) {
    }

    public record PatchStatistics(int filesChanged, int insertions, int deletions, int patchSize) {
    }

    public record PatchSummary(String summary, PatchStatus status, PatchStatistics statistics) {
    }

    public record PatchValidation(boolean valid, String message) {
    }

    public record ArtifactReference(
            UUID artifactId, String path, String filename, String language, String sha256) {
    }

    public record PatchResult(
            UUID id,
            UUID taskId,
            UUID runId,
            UUID projectId,
            String summary,
            PatchStatus status,
            PatchStatistics statistics,
            String patch,
            List<PatchFile> files,
            List<ArtifactReference> artifacts,
            PatchValidation validation,
            Long tokensUsed,
            String model,
            String provider,
            Long generationTimeMs,
            Instant createdAt) {
    }

    public record PatchPromptContext(
            UUID taskId,
            String taskKey,
            String displayName,
            String description,
            String objective,
            List<GeneratedArtifactResponse> artifacts,
            boolean reviewApproved,
            Integer reviewScore,
            String reviewSummary,
            List<ReviewFinding> reviewFindings,
            Integer coverageEstimate,
            String testingSummary,
            List<GeneratedTest> generatedTests,
            Map<String, String> organizationSettings,
            Map<String, String> projectSettings) {
    }
}
