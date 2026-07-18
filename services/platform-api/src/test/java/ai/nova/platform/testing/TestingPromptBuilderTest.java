package ai.nova.platform.testing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.coding.entity.ArtifactLanguage;
import ai.nova.platform.coding.entity.ArtifactType;
import ai.nova.platform.review.dto.ReviewDtos.ReviewFinding;
import ai.nova.platform.review.entity.ReviewCategory;
import ai.nova.platform.review.entity.ReviewSeverity;
import ai.nova.platform.testing.config.TestingProperties;
import ai.nova.platform.testing.dto.TestingDtos.TestingPromptContext;
import ai.nova.platform.testing.service.TestingPromptBuilder;

class TestingPromptBuilderTest {

    @Test
    void buildsPromptWithArtifactsFindingsAndSchema() {
        TestingPromptBuilder builder = new TestingPromptBuilder(new TestingProperties());
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
        String prompt = builder.buildUserPrompt(new TestingPromptContext(
                UUID.randomUUID(),
                "coding-1",
                "Generate login",
                "agentRole=coding",
                "Build login",
                List.of(artifact),
                List.of(new ReviewFinding(
                        UUID.randomUUID(),
                        ReviewSeverity.MEDIUM,
                        ReviewCategory.SECURITY,
                        "Input validation",
                        "Missing validation",
                        "Add Bean Validation",
                        null,
                        "src/LoginService.java")),
                Map.of("organizationId", "org"),
                Map.of("projectName", "Demo")));

        assertThat(prompt).contains("Build login");
        assertThat(prompt).contains("LoginService.java");
        assertThat(prompt).contains("Input validation");
        assertThat(prompt).contains("UNIT");
        assertThat(prompt).contains("coverageEstimate");
        assertThat(prompt).contains("Never execute");
        assertThat(builder.buildSystemPrompt()).contains("Testing Agent");
    }
}
