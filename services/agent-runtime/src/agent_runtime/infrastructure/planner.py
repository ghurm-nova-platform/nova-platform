"""Deterministic planner that produces structured execution plans."""

from __future__ import annotations

from agent_runtime.domain.enums import RiskLevel
from agent_runtime.domain.interfaces import PlannerPort
from agent_runtime.domain.models import ExecutionPlan, PlanTask


class DeterministicPlanner(PlannerPort):
    """
    Foundation planner that does not call an AI provider.

    Produces a reproducible plan suitable for Sprint 0 scaffolding.
    A later adapter may wrap ModelProviderPort behind this port.
    """

    def create_plan(
        self,
        goal: str,
        *,
        agent_id: str | None = None,
        risk_level: RiskLevel = RiskLevel.LOW,
    ) -> ExecutionPlan:
        """Create a three-step plan with approval flags for elevated risk."""

        requires_approval = risk_level in {RiskLevel.HIGH, RiskLevel.CRITICAL}
        tasks = [
            PlanTask(
                title="Analyze goal",
                description=f"Inspect requirements for: {goal}",
                agent_id=agent_id,
                risk_level=RiskLevel.LOW,
                requires_approval=False,
                acceptance_evidence=["goal_normalized"],
            ),
            PlanTask(
                title="Propose actions",
                description="Produce structured actions within policy bounds",
                agent_id=agent_id,
                depends_on=[],
                risk_level=risk_level,
                requires_approval=requires_approval,
                acceptance_evidence=["action_list"],
            ),
            PlanTask(
                title="Validate outcome",
                description="Validate outputs against acceptance criteria",
                agent_id=agent_id,
                risk_level=RiskLevel.LOW,
                requires_approval=False,
                acceptance_evidence=["validation_report"],
            ),
        ]
        tasks[1].depends_on = [tasks[0].task_id]
        tasks[2].depends_on = [tasks[1].task_id]

        risks = []
        if requires_approval:
            risks.append("Elevated risk requires explicit human approval")

        return ExecutionPlan(
            goal=goal,
            assumptions=["Agents remain read-only unless policy grants write access"],
            risks=risks,
            tasks=tasks,
        )
