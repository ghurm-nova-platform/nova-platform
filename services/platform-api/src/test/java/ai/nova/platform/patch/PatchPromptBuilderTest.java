package ai.nova.platform.patch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.coding.entity.ArtifactLanguage;
import ai.nova.platform.coding.entity.ArtifactType;
import ai.nova.platform.patch.config.PatchProperties;
import ai.nova.platform.patch.dto.PatchDtos.PatchPromptContext;
import ai.nova.platform.patch.service.PatchPromptBuilder;
import ai.nova.platform.review.dto.ReviewDtos.ReviewFinding;
import ai.nova.platform.review.entity.ReviewCategory;
import ai.nova.platform.review.entity.ReviewSeverity;
import ai.nova.platform.testing.dto.TestingDtos.GeneratedTest;
import ai.nova.platform.testing.entity.TestPriority;
import ai.nova.platform.testing.entity.TestType;

class PatchPromptBuilderTest {

    @Test
    void buildsPromptWithArtifactsReviewAndTesting() {
        PatchPromptBuilder builder = new PatchPromptBuilder(new PatchProperties());
        assertThat(builder.buildSystemPrompt()).contains("Patch Agent");

        GeneratedArtifactResponse artifact = new GeneratedArtifactResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                ArtifactType.SOURCE_FILE,
                ArtifactLanguage.JAVA,
                "src/LoginService.java",
                "LoginService.java",
                "class LoginService {}",
                "abc",
                10L,
                "coding-local",
                "LOCAL",
                5L,
                java.time.Instant.now());

        String prompt = builder.buildUserPrompt(new PatchPromptContext(
                UUID.randomUUID(),
                "patch-1",
                "Generate patch",
                "agentRole=patch",
                "Ship login",
                List.of(artifact),
                true,
                92,
                "Looks good",
                List.of(new ReviewFinding(
                        UUID.randomUUID(),
                        ReviewSeverity.LOW,
                        ReviewCategory.NAMING,
                        "Rename helper",
                        "Minor naming",
                        "Rename method",
                        artifact.id(),
                        artifact.path())),
                84,
                "Unit tests generated",
                List.of(new GeneratedTest(
                        UUID.randomUUID(),
                        TestType.UNIT,
                        TestPriority.HIGH,
                        "Login validation",
                        "Reject blank password",
                        artifact.id(),
                        artifact.path(),
                        List.of())),
                Map.of("organizationId", "org"),
                Map.of("projectName", "demo")));

        assertThat(prompt).contains("LoginService.java");
        assertThat(prompt).contains("approved=true");
        assertThat(prompt).contains("Rename helper");
        assertThat(prompt).contains("Login validation");
        assertThat(prompt).contains("Unified Diff");
        assertThat(prompt).contains("\"status\": \"VALID\"");
    }
}
