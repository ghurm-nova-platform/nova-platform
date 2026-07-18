package ai.nova.platform.planner.service;

import org.springframework.stereotype.Service;

import ai.nova.platform.planner.config.PlannerProperties;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionEstimate;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionPlan;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionTaskDefinition;
import ai.nova.platform.planner.entity.PlannerComplexity;
import ai.nova.platform.planner.entity.PlannerRiskLevel;
import ai.nova.platform.orchestration.entity.TaskType;

@Service
public class PlannerEstimationService {

    private final PlannerProperties properties;

    public PlannerEstimationService(PlannerProperties properties) {
        this.properties = properties;
    }

    public ExecutionEstimate estimate(ExecutionPlan plan) {
        int taskCount = plan.tasks() == null ? 0 : plan.tasks().size();
        int depCount = plan.dependencies() == null ? 0 : plan.dependencies().size();
        int humanApprovals = 0;
        int agentTurns = 0;
        if (plan.tasks() != null) {
            for (ExecutionTaskDefinition task : plan.tasks()) {
                if (task.taskType() == TaskType.HUMAN_APPROVAL) {
                    humanApprovals++;
                } else if (task.taskType() == TaskType.AGENT_TURN) {
                    agentTurns++;
                }
            }
        }

        long tokens = plan.estimatedTokens() != null && plan.estimatedTokens() > 0
                ? plan.estimatedTokens()
                : Math.max(1500L, agentTurns * 2500L + taskCount * 400L);
        long duration = plan.estimatedDurationSeconds() != null && plan.estimatedDurationSeconds() > 0
                ? plan.estimatedDurationSeconds()
                : Math.max(30L, agentTurns * 45L + humanApprovals * 120L + depCount * 5L);
        double cost = plan.estimatedCostUsd() != null && plan.estimatedCostUsd() > 0
                ? plan.estimatedCostUsd()
                : (tokens / 1000.0) * properties.getCostPerThousandTokensUsd();

        PlannerComplexity complexity = plan.estimatedComplexity() != null
                ? plan.estimatedComplexity()
                : deriveComplexity(taskCount, depCount, humanApprovals);
        PlannerRiskLevel risk = deriveRisk(complexity, humanApprovals, taskCount);

        String notes = "Deterministic estimate from plan shape; refine with live usage after execution.";
        return new ExecutionEstimate(complexity, risk, tokens, duration, round2(cost), notes);
    }

    private static PlannerComplexity deriveComplexity(int tasks, int deps, int approvals) {
        int score = tasks + deps / 2 + approvals * 2;
        if (score <= 3) {
            return PlannerComplexity.LOW;
        }
        if (score <= 8) {
            return PlannerComplexity.MEDIUM;
        }
        if (score <= 16) {
            return PlannerComplexity.HIGH;
        }
        return PlannerComplexity.VERY_HIGH;
    }

    private static PlannerRiskLevel deriveRisk(PlannerComplexity complexity, int approvals, int tasks) {
        if (approvals > 0 || complexity == PlannerComplexity.VERY_HIGH || tasks > 12) {
            return PlannerRiskLevel.VERY_HIGH;
        }
        return switch (complexity) {
            case LOW -> PlannerRiskLevel.LOW;
            case MEDIUM -> PlannerRiskLevel.MEDIUM;
            case HIGH -> PlannerRiskLevel.HIGH;
            case VERY_HIGH -> PlannerRiskLevel.VERY_HIGH;
        };
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
