package ai.nova.platform.coding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ai.nova.platform.coding.config.CodingProperties;
import ai.nova.platform.coding.dto.CodingDtos.CodingPromptContext;
import ai.nova.platform.coding.dto.CodingDtos.CodingTask;
import ai.nova.platform.coding.dto.CodingDtos.DependencySummary;
import ai.nova.platform.coding.service.CodingPromptBuilder;

class CodingPromptBuilderTest {

    @Test
    void buildsPromptWithObjectiveTaskDependenciesLanguagesAndSchema() {
        CodingProperties properties = new CodingProperties();
        CodingPromptBuilder builder = new CodingPromptBuilder(properties);
        CodingTask task = new CodingTask(
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                "implement-login",
                "Implement login",
                "Create login service",
                "AGENT_TURN",
                "READY",
                "{\"feature\":\"login\"}",
                "coding-local",
                null);
        String prompt = builder.buildUserPrompt(new CodingPromptContext(
                task,
                "Build authentication",
                List.of(new DependencySummary(
                        java.util.UUID.randomUUID(), "analyze", "Analyze", "SUCCEEDED", "{\"ok\":true}")),
                Map.of("organizationId", "org-1"),
                Map.of("projectName", "Demo")));

        assertThat(prompt).contains("Build authentication");
        assertThat(prompt).contains("implement-login");
        assertThat(prompt).contains("Create login service");
        assertThat(prompt).contains("analyze");
        assertThat(prompt).contains("JAVA");
        assertThat(prompt).contains("SOURCE_FILE");
        assertThat(prompt).contains("Output schema");
        assertThat(prompt).contains("organizationId=org-1");
        assertThat(prompt).contains("projectName=Demo");
        assertThat(builder.buildSystemPrompt()).contains("Coding Agent");
    }
}
