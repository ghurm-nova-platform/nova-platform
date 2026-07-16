"""Execution engine coordinating registry, policy, planner, and events."""

from __future__ import annotations

from agent_runtime.domain.enums import PermissionAction, WorkflowState
from agent_runtime.domain.interfaces import (
    AgentRegistryPort,
    EventBusPort,
    ExecutionEnginePort,
    MemoryPort,
    MetricsPort,
    PermissionEnginePort,
    PlannerPort,
    WorkflowEnginePort,
)
from agent_runtime.domain.models import (
    ExecutionRequest,
    ExecutionResult,
    RuntimeEvent,
    Workflow,
    utc_now,
)
from agent_runtime.shared.exceptions import PermissionDeniedError
from agent_runtime.shared.logging import get_logger

logger = get_logger(__name__)


class DefaultExecutionEngine(ExecutionEnginePort):
    """
    Foundation execution engine.

    Flow: load agent → policy check → create workflow → plan → emit events.
    Does not call vendor model APIs; produces structured foundation results.
    """

    def __init__(
        self,
        *,
        agent_registry: AgentRegistryPort,
        permission_engine: PermissionEnginePort,
        planner: PlannerPort,
        workflow_engine: WorkflowEnginePort,
        event_bus: EventBusPort,
        memory: MemoryPort,
        metrics: MetricsPort,
    ) -> None:
        self._agents = agent_registry
        self._permissions = permission_engine
        self._planner = planner
        self._workflows = workflow_engine
        self._events = event_bus
        self._memory = memory
        self._metrics = metrics

    def execute(self, request: ExecutionRequest) -> ExecutionResult:
        """Execute an agent request under policy and emit lifecycle events."""

        logger.info(
            "execution_started",
            execution_id=request.execution_id,
            agent_id=request.agent_id,
            actor=request.actor,
        )

        agent = self._agents.get(request.agent_id)
        allowed = self._permissions.evaluate(
            actor=request.actor,
            action=PermissionAction.READ if agent.read_only_default else PermissionAction.EXECUTE,
            risk_level=request.risk_level,
            agent=agent,
            dry_run=request.dry_run,
        )
        if not allowed:
            self._metrics.record_execution(success=False)
            raise PermissionDeniedError(
                f"Actor '{request.actor}' is not permitted to execute agent '{agent.agent_id}'"
            )

        workflow = self._workflows.create(
            Workflow(
                name=f"execute:{agent.agent_id}",
                goal=request.goal,
                agent_id=agent.agent_id,
                risk_level=request.risk_level,
                metadata={
                    "execution_id": request.execution_id,
                    "actor": request.actor,
                    "organization_id": request.organization_id,
                    "project_id": request.project_id,
                },
            )
        )
        self._workflows.transition(workflow.workflow_id, WorkflowState.PLANNING.value)
        plan = self._planner.create_plan(
            request.goal,
            agent_id=agent.agent_id,
            risk_level=request.risk_level,
        )
        workflow = self._workflows.get(workflow.workflow_id)
        workflow = self._workflows.save(workflow.model_copy(update={"plan": plan}))

        if any(task.requires_approval for task in plan.tasks):
            workflow = self._workflows.transition(
                workflow.workflow_id,
                WorkflowState.WAITING_FOR_APPROVAL.value,
            )
            status = WorkflowState.WAITING_FOR_APPROVAL
            message = "Plan created; waiting for human approval"
        else:
            workflow = self._workflows.transition(
                workflow.workflow_id,
                WorkflowState.RUNNING.value,
            )
            workflow = self._workflows.transition(
                workflow.workflow_id,
                WorkflowState.COMPLETED.value,
            )
            status = WorkflowState.COMPLETED
            message = "Foundation execution completed without provider call"

        self._memory.store(
            f"execution:{request.execution_id}",
            {"goal": request.goal, "agent_id": agent.agent_id, "status": status.value},
        )
        self._events.publish(
            RuntimeEvent(
                event_type="agent.execution.finished",
                source="execution_engine",
                payload={
                    "execution_id": request.execution_id,
                    "workflow_id": workflow.workflow_id,
                    "status": status.value,
                },
                correlation_id=request.execution_id,
            )
        )
        self._metrics.record_execution(success=status is not WorkflowState.FAILED)

        result = ExecutionResult(
            execution_id=request.execution_id,
            agent_id=agent.agent_id,
            workflow_id=workflow.workflow_id,
            status=status,
            output={
                "plan_id": plan.plan_id,
                "tasks": [task.model_dump(mode="json") for task in plan.tasks],
                "provider": "none",
            },
            message=message,
            completed_at=utc_now(),
        )
        logger.info(
            "execution_finished",
            execution_id=result.execution_id,
            status=result.status.value,
            workflow_id=result.workflow_id,
        )
        return result
