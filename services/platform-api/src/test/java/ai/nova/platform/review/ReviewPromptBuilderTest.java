package ai.nova.platform.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.coding.entity.ArtifactLanguage;
import ai.nova.platform.coding.entity.ArtifactType;
import ai.nova.platform.review.config.ReviewProperties;
import ai.nova.platform.review.dto.ReviewDtos.ReviewPromptContext;
import ai.nova.platform.review.service.ReviewPromptBuilder;

class ReviewPromptBuilderTest {

    @Test
    void buildsPromptWithArtifactsSchemaAndRules() {
        ReviewPromptBuilder builder = new ReviewPromptBuilder(new ReviewProperties());
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
        String prompt = builder.buildUserPrompt(new ReviewPromptContext(
                UUID.randomUUID(),
                "coding-1",
                "Generate login",
                "agentRole=coding",
                "Build login",
                List.of(artifact),
                Map.of("organizationId", "org"),
                Map.of("projectName", "Demo")));

        assertThat(prompt).contains("Build login");
        assertThat(prompt).contains("LoginService.java");
        assertThat(prompt).contains("SECURITY");
        assertThat(prompt).contains("CRITICAL");
        assertThat(prompt).contains("Output schema");
        assertThat(prompt).contains("Do not modify artifacts");
        assertThat(builder.buildSystemPrompt()).contains("Review Agent");
    }
}
