package ai.nova.platform.planner.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import ai.nova.platform.agent.entity.Agent;
import ai.nova.platform.agent.entity.AgentStatus;
import ai.nova.platform.agent.repository.AgentRepository;
import ai.nova.platform.planner.config.PlannerProperties;
import ai.nova.platform.planner.entity.PlannerTemplate;
import ai.nova.platform.project.Project;
import ai.nova.platform.tool.repository.ToolDefinitionRepository;

@Service
public class PlannerPromptBuilder {

    private static final List<String> ALLOWED_ROLES = List.of(
            "planner",
            "research",
            "coding",
            "review",
            "testing",
            "documentation",
            "security",
            "devops",
            "database",
            "ui",
            "backend",
            "frontend",
            "architecture",
            "human",
            "transform",
            "aggregation");

    private final AgentRepository agentRepository;
    private final ToolDefinitionRepository toolDefinitionRepository;
    private final PlannerProperties properties;

    public PlannerPromptBuilder(
            AgentRepository agentRepository,
            ToolDefinitionRepository toolDefinitionRepository,
            PlannerProperties properties) {
        this.agentRepository = agentRepository;
        this.toolDefinitionRepository = toolDefinitionRepository;
        this.properties = properties;
    }

    public String buildSystemPrompt(PlannerTemplate template) {
        if (template != null && template.getSystemPrompt() != null && !template.getSystemPrompt().isBlank()) {
            return template.getSystemPrompt().trim();
        }
        return properties.getDefaultSystemPrompt().trim();
    }

    public String buildUserPrompt(
            Project project,
            String objective,
            String metadataJson) {
        List<Agent> agents = agentRepository
                .search(
                        project.getOrganizationId(),
                        project.getId(),
                        null,
                        AgentStatus.ACTIVE,
                        PageRequest.of(0, 50))
                .getContent();

        Set<String> toolKeys = new LinkedHashSet<>();
        toolDefinitionRepository
                .search(
                        project.getOrganizationId(),
                        project.getId(),
                        null,
                        null,
                        null,
                        PageRequest.of(0, 40))
                .forEach(tool -> toolKeys.add(tool.getToolKey()));

        StringBuilder agentsBlock = new StringBuilder();
        for (Agent agent : agents) {
            agentsBlock
                    .append("- id=")
                    .append(agent.getId())
                    .append(" name=")
                    .append(agent.getName())
                    .append(" provider=")
                    .append(agent.getModelProvider())
                    .append(" model=")
                    .append(agent.getModelName())
                    .append('\n');
        }
        if (agentsBlock.isEmpty()) {
            agentsBlock.append("- (no active project agents; leave assignedAgentId null)\n");
        }

        return """
                OrganizationId: %s
                ProjectId: %s
                ProjectName: %s

                Objective:
                %s

                Metadata (optional JSON):
                %s

                Available agents in this project:
                %s
                Available tool keys (context only — do not invoke):
                %s

                Allowed agentRole values:
                %s

                Allowed taskType values:
                AGENT_TURN, HUMAN_APPROVAL, TRANSFORM, AGGREGATION

                Allowed executionMode values:
                SEQUENTIAL, DEPENDENCY_GRAPH

                Allowed failurePolicy values:
                FAIL_FAST, CONTINUE_INDEPENDENT, BEST_EFFORT

                Constraints:
                - Return JSON only (no markdown fences, no commentary).
                - Do not execute tools, shell, git, MCP, or browser actions.
                - Do not invent secrets or credentials.
                - Include at least one task.
                - Dependencies must reference taskKey values via from/to.
                - Prefer SUCCESS dependencies unless COMPLETION is required.

                Required JSON schema:
                {
                  "objective":"string",
                  "executionMode":"DEPENDENCY_GRAPH|SEQUENTIAL",
                  "failurePolicy":"FAIL_FAST|CONTINUE_INDEPENDENT|BEST_EFFORT",
                  "maxParallelTasks":1,
                  "estimatedComplexity":"LOW|MEDIUM|HIGH|VERY_HIGH",
                  "estimatedTokens":0,
                  "estimatedDurationSeconds":0,
                  "tasks":[
                    {
                      "taskKey":"lowercase-key",
                      "displayName":"string",
                      "description":"string",
                      "taskType":"AGENT_TURN",
                      "agentRole":"coding",
                      "classification":"CODING",
                      "priority":1,
                      "sequenceOrder":1
                    }
                  ],
                  "dependencies":[
                    {"from":"task-a","to":"task-b","type":"SUCCESS"}
                  ]
                }
                """
                .formatted(
                        project.getOrganizationId(),
                        project.getId(),
                        project.getName(),
                        objective.trim(),
                        metadataJson == null || metadataJson.isBlank() ? "{}" : metadataJson.trim(),
                        agentsBlock,
                        toolKeys.isEmpty() ? "(none)" : String.join(", ", toolKeys),
                        String.join(", ", ALLOWED_ROLES));
    }

    public List<String> allowedRoles() {
        return new ArrayList<>(ALLOWED_ROLES);
    }

    public static String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        return role.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isAllowedRole(String role) {
        String normalized = normalizeRole(role);
        return ALLOWED_ROLES.contains(normalized);
    }

    public String rolesCsv() {
        return ALLOWED_ROLES.stream().collect(Collectors.joining(", "));
    }
}
