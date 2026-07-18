package ai.nova.platform.testing.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.review.dto.ReviewDtos.ReviewFinding;
import ai.nova.platform.testing.entity.TestPriority;
import ai.nova.platform.testing.entity.TestType;

public final class TestingDtos {

    private TestingDtos() {
    }

    public record TestingRunRequest(@NotNull UUID taskId) {
    }

    public record TestCaseDraft(String name, String steps, String expectedResult, TestPriority priority) {
    }

    public record GeneratedTestDraft(
            TestType type,
            TestPriority priority,
            String title,
            String description,
            String artifactPath,
            List<TestCaseDraft> cases) {
    }

    public record ParsedTestingOutput(
            String summary, Integer coverageEstimate, List<GeneratedTestDraft> generatedTests) {
    }

    public record TestCase(
            UUID id,
            UUID generatedTestId,
            String name,
            String steps,
            String expectedResult,
            TestPriority priority) {
    }

    public record GeneratedTest(
            UUID id,
            TestType type,
            TestPriority priority,
            String title,
            String description,
            UUID artifactId,
            String artifactPath,
            List<TestCase> cases) {
    }

    public record TestSuite(String name, TestType type, List<GeneratedTest> tests) {
    }

    public record CoverageEstimate(int value) {
    }

    public record ArtifactReference(
            UUID artifactId, String path, String filename, String language, String sha256) {
    }

    public record TestingResult(
            UUID id,
            UUID taskId,
            UUID runId,
            UUID projectId,
            String summary,
            int coverageEstimate,
            List<GeneratedTest> generatedTests,
            List<TestCase> testCases,
            List<ArtifactReference> reviewedArtifacts,
            Map<String, Long> typeCounts,
            Map<String, Long> priorityCounts,
            Long tokensUsed,
            String model,
            String provider,
            Long generationTimeMs,
            Instant createdAt,
            boolean validated) {
    }

    public record TestingPromptContext(
            UUID taskId,
            String taskKey,
            String displayName,
            String description,
            String objective,
            List<GeneratedArtifactResponse> artifacts,
            List<ReviewFinding> reviewFindings,
            Map<String, String> organizationSettings,
            Map<String, String> projectSettings) {
    }
}
