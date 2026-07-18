package ai.nova.platform.planner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ai.nova.platform.agent.repository.AgentRepository;
import ai.nova.platform.orchestration.entity.DependencyType;
import ai.nova.platform.orchestration.entity.ExecutionMode;
import ai.nova.platform.orchestration.entity.FailurePolicy;
import ai.nova.platform.orchestration.entity.TaskType;
import ai.nova.platform.planner.config.PlannerProperties;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionDependency;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionPlan;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionTaskDefinition;
import ai.nova.platform.planner.service.PlannerPlanValidator;
import ai.nova.platform.planner.service.PlannerPromptBuilder;
import ai.nova.platform.tool.repository.ToolDefinitionRepository;
import ai.nova.platform.web.error.ApiException;

class PlannerValidationTest {

    private PlannerPlanValidator validator;

    @BeforeEach
    void setUp() {
        PlannerPromptBuilder promptBuilder =
                new PlannerPromptBuilder(mock(AgentRepository.class), mock(ToolDefinitionRepository.class), new PlannerProperties());
        validator = new PlannerPlanValidator(promptBuilder, new PlannerProperties());
    }

    @Test
    void rejectsEmptyTasks() {
        ExecutionPlan plan = basePlan(List.of(), List.of());
        assertThatThrownBy(() -> validator.validate(plan))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PLANNER_EMPTY_TASKS");
    }

    @Test
    void rejectsDuplicateTaskKeys() {
        ExecutionPlan plan = basePlan(
                List.of(task("a", "coding"), task("a", "coding")),
                List.of());
        assertThatThrownBy(() -> validator.validate(plan))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PLANNER_DUPLICATE_TASK_KEY");
    }

    @Test
    void rejectsCycle() {
        ExecutionPlan plan = basePlan(
                List.of(task("a", "coding"), task("b", "review")),
                List.of(
                        new ExecutionDependency("a", "b", DependencyType.SUCCESS),
                        new ExecutionDependency("b", "a", DependencyType.SUCCESS)));
        assertThatThrownBy(() -> validator.validate(plan))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PLANNER_GRAPH_CYCLE");
    }

    @Test
    void rejectsUnknownDependency() {
        ExecutionPlan plan = basePlan(
                List.of(task("a", "coding")),
                List.of(new ExecutionDependency("a", "missing", DependencyType.SUCCESS)));
        assertThatThrownBy(() -> validator.validate(plan))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PLANNER_UNKNOWN_DEPENDENCY");
    }

    @Test
    void rejectsMissingAgentRole() {
        ExecutionTaskDefinition bad = new ExecutionTaskDefinition(
                "a", "A", null, TaskType.AGENT_TURN, " ", null, 1, null, null, null, null);
        ExecutionPlan plan = basePlan(List.of(bad), List.of());
        assertThatThrownBy(() -> validator.validate(plan))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PLANNER_AGENT_ROLE_REQUIRED");
    }

    @Test
    void acceptsValidDag() {
        ExecutionPlan plan = basePlan(
                List.of(task("analysis", "research"), task("implementation", "coding")),
                List.of(new ExecutionDependency("analysis", "implementation", DependencyType.SUCCESS)));
        validator.validate(plan);
        assertThat(plan.tasks()).hasSize(2);
    }

    private static ExecutionPlan basePlan(
            List<ExecutionTaskDefinition> tasks, List<ExecutionDependency> deps) {
        return new ExecutionPlan(
                "Build auth",
                ExecutionMode.DEPENDENCY_GRAPH,
                FailurePolicy.FAIL_FAST,
                2,
                600000L,
                null,
                null,
                null,
                null,
                null,
                tasks,
                deps,
                null);
    }

    private static ExecutionTaskDefinition task(String key, String role) {
        return new ExecutionTaskDefinition(
                key, key, null, TaskType.AGENT_TURN, role, null, 1, null, null, null, null);
    }
}
