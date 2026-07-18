package ai.nova.platform.planner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import ai.nova.platform.orchestration.entity.DependencyType;
import ai.nova.platform.orchestration.entity.ExecutionMode;
import ai.nova.platform.orchestration.entity.FailurePolicy;
import ai.nova.platform.orchestration.entity.TaskType;
import ai.nova.platform.planner.config.PlannerProperties;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionDependency;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionPlan;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionTaskDefinition;
import ai.nova.platform.planner.entity.PlannerComplexity;
import ai.nova.platform.planner.entity.PlannerRiskLevel;
import ai.nova.platform.planner.service.PlannerEstimationService;

class PlannerEstimationServiceTest {

    private final PlannerEstimationService service = new PlannerEstimationService(new PlannerProperties());

    @Test
    void estimatesFromPlanShape() {
        ExecutionPlan plan = new ExecutionPlan(
                "obj",
                ExecutionMode.DEPENDENCY_GRAPH,
                FailurePolicy.FAIL_FAST,
                2,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(
                        new ExecutionTaskDefinition(
                                "a", "A", null, TaskType.AGENT_TURN, "coding", null, 1, null, null, null, null),
                        new ExecutionTaskDefinition(
                                "b",
                                "B",
                                null,
                                TaskType.HUMAN_APPROVAL,
                                "human",
                                null,
                                2,
                                null,
                                null,
                                null,
                                null)),
                List.of(new ExecutionDependency("a", "b", DependencyType.SUCCESS)),
                null);
        var estimate = service.estimate(plan);
        assertThat(estimate.estimatedTokens()).isGreaterThan(0);
        assertThat(estimate.estimatedDurationSeconds()).isGreaterThan(0);
        assertThat(estimate.complexity()).isIn(PlannerComplexity.values());
        assertThat(estimate.riskLevel()).isIn(PlannerRiskLevel.values());
    }
}
