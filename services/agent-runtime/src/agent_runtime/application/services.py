"""Application services for agents, workflows, execution, and metrics."""

from __future__ import annotations

from agent_runtime.domain.enums import RiskLevel, WorkflowState
from agent_runtime.domain.interfaces import (
    AgentRegistryPort,
    EventBusPort,
    ExecutionEnginePort,
    MetricsPort,
    WorkflowEnginePort,
)
from agent_runtime.domain.models import (
    AgentDefinition,
    ExecutionRequest,
    ExecutionResult,
    MetricSnapshot,
    RuntimeEvent,
    Workflow,
)
from agent_runtime.shared.logging import get_logger

logger = get_logger(__name__)


class AgentService:
    """Application service for agent registration and discovery."""

    def __init__(
        self,
        registry: AgentRegistryPort,
        event_bus: EventBusPort,
    ) -> None:
        self._registry = registry
        self._event_bus = event_bus

    def list_agents(self) -> list[AgentDefinition]:
        """List all registered agents."""

        return self._registry.list_agents()

    def register(self, agent: AgentDefinition) -> AgentDefinition:
        """Register an agent and emit a domain event."""

        registered = self._registry.register(agent)
        self._event_bus.publish(
            RuntimeEvent(
                event_type="agent.registered",
                source="agent_service",
                payload={"agent_id": registered.agent_id, "version": registered.version},
            )
        )
        logger.info("agent_registered", agent_id=registered.agent_id, version=registered.version)
        return registered


class WorkflowService:
    """Application service for workflow creation and listing."""

    def __init__(
        self,
        workflow_engine: WorkflowEnginePort,
        event_bus: EventBusPort,
    ) -> None:
        self._workflows = workflow_engine
        self._event_bus = event_bus

    def list_workflows(self) -> list[Workflow]:
        """List workflows."""

        return self._workflows.list_workflows()

    def create_workflow(
        self,
        *,
        name: str,
        goal: str,
        agent_id: str | None = None,
        risk_level: RiskLevel = RiskLevel.LOW,
    ) -> Workflow:
        """Create a workflow in CREATED then QUEUED state."""

        workflow = self._workflows.create(
            Workflow(
                name=name,
                goal=goal,
                agent_id=agent_id,
                risk_level=risk_level,
            )
        )
        workflow = self._workflows.transition(workflow.workflow_id, WorkflowState.QUEUED.value)
        self._event_bus.publish(
            RuntimeEvent(
                event_type="workflow.created",
                source="workflow_service",
                payload={"workflow_id": workflow.workflow_id, "state": workflow.state.value},
            )
        )
        logger.info("workflow_created", workflow_id=workflow.workflow_id)
        return workflow


class ExecutionService:
    """Application service for agent execution requests."""

    def __init__(self, execution_engine: ExecutionEnginePort) -> None:
        self._engine = execution_engine

    def execute(self, request: ExecutionRequest) -> ExecutionResult:
        """Delegate execution to the execution engine."""

        return self._engine.execute(request)


class MetricsService:
    """Application service for operational metrics."""

    def __init__(self, metrics: MetricsPort) -> None:
        self._metrics = metrics

    def get_metrics(self) -> MetricSnapshot:
        """Return the current metrics snapshot."""

        return self._metrics.snapshot()
