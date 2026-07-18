package ai.nova.platform.planner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import ai.nova.platform.agent.entity.AgentStatus;
import ai.nova.platform.agent.repository.AgentRepository;
import ai.nova.platform.planner.config.PlannerProperties;
import ai.nova.platform.planner.service.PlannerPromptBuilder;
import ai.nova.platform.project.Project;
import ai.nova.platform.project.ProjectStatus;
import ai.nova.platform.project.ProjectVisibility;
import ai.nova.platform.tool.repository.ToolDefinitionRepository;

class PlannerPromptBuilderTest {

    @Test
    void buildsPromptWithSchemaAndRoles() {
        AgentRepository agents = mock(AgentRepository.class);
        ToolDefinitionRepository tools = mock(ToolDefinitionRepository.class);
        when(agents.search(any(), any(), any(), eq(AgentStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(tools.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PlannerPromptBuilder builder = new PlannerPromptBuilder(agents, tools, new PlannerProperties());
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        UUID userId = UUID.fromString("44444444-4444-4444-4444-444444444401");
        Project project = new Project(
                UUID.fromString("55555555-5555-5555-5555-555555555501"),
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Demo",
                "desc",
                ProjectStatus.ACTIVE,
                ProjectVisibility.PRIVATE,
                now,
                now,
                userId,
                userId);

        String prompt = builder.buildUserPrompt(project, "Ship feature X", "{\"priority\":\"high\"}");
        assertThat(prompt).contains("Ship feature X");
        assertThat(prompt).contains("agentRole");
        assertThat(prompt).contains("DEPENDENCY_GRAPH");
        assertThat(prompt).contains("Do not execute tools");
        assertThat(builder.isAllowedRole("coding")).isTrue();
        assertThat(builder.isAllowedRole("not-a-role")).isFalse();
    }
}
