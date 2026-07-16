"""Domain ports (interfaces) — no concrete AI provider bindings."""

from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any

from agent_runtime.domain.enums import PermissionAction, RiskLevel
from agent_runtime.domain.models import (
    AgentDefinition,
    ExecutionPlan,
    ExecutionRequest,
    ExecutionResult,
    MetricSnapshot,
    RuntimeEvent,
    Workflow,
)


class AgentRegistryPort(ABC):
    """Port for discovering and registering agents."""

    @abstractmethod
    def register(self, agent: AgentDefinition) -> AgentDefinition:
        """Persist a new or updated agent definition."""

    @abstractmethod
    def list_agents(self) -> list[AgentDefinition]:
        """Return all registered agents."""

    @abstractmethod
    def get(self, agent_id: str) -> AgentDefinition:
        """Return a single agent by identifier."""


class WorkflowEnginePort(ABC):
    """Port for durable workflow state management."""

    @abstractmethod
    def create(self, workflow: Workflow) -> Workflow:
        """Create a workflow in CREATED state."""

    @abstractmethod
    def list_workflows(self) -> list[Workflow]:
        """Return all workflows."""

    @abstractmethod
    def get(self, workflow_id: str) -> Workflow:
        """Return a workflow by identifier."""

    @abstractmethod
    def save(self, workflow: Workflow) -> Workflow:
        """Persist an updated workflow document."""

    @abstractmethod
    def transition(self, workflow_id: str, new_state: str) -> Workflow:
        """Transition a workflow to a new state."""


class PlannerPort(ABC):
    """Port that turns a goal into a structured execution plan."""

    @abstractmethod
    def create_plan(
        self,
        goal: str,
        *,
        agent_id: str | None = None,
        risk_level: RiskLevel = RiskLevel.LOW,
    ) -> ExecutionPlan:
        """Create a plan with tasks, risks, and acceptance evidence."""


class ExecutionEnginePort(ABC):
    """Port that coordinates policy checks and agent execution."""

    @abstractmethod
    def execute(self, request: ExecutionRequest) -> ExecutionResult:
        """Execute an agent request and return the result."""


class EventBusPort(ABC):
    """Port for publishing and consuming runtime events."""

    @abstractmethod
    def publish(self, event: RuntimeEvent) -> None:
        """Publish an event to subscribers."""

    @abstractmethod
    def list_events(self, *, limit: int = 100) -> list[RuntimeEvent]:
        """Return recent events for diagnostics."""


class PermissionEnginePort(ABC):
    """Port that evaluates whether an action is allowed."""

    @abstractmethod
    def evaluate(
        self,
        *,
        actor: str,
        action: PermissionAction,
        risk_level: RiskLevel,
        agent: AgentDefinition,
        dry_run: bool = False,
    ) -> bool:
        """Return True when the action is permitted."""


class MemoryPort(ABC):
    """Port for project / session memory retrieval and storage."""

    @abstractmethod
    def store(self, key: str, value: dict[str, Any]) -> None:
        """Store a memory entry."""

    @abstractmethod
    def retrieve(self, key: str) -> dict[str, Any] | None:
        """Retrieve a memory entry by key."""


class KnowledgePort(ABC):
    """Port for semantic / architectural knowledge retrieval."""

    @abstractmethod
    def search(self, query: str, *, limit: int = 5) -> list[dict[str, Any]]:
        """Search knowledge items relevant to a query."""

    @abstractmethod
    def upsert(self, item_id: str, content: dict[str, Any]) -> None:
        """Insert or update a knowledge item."""


class ToolGatewayPort(ABC):
    """Port for invoking approved tools through a controlled gateway."""

    @abstractmethod
    def list_tools(self) -> list[str]:
        """Return tool identifiers available to the runtime."""

    @abstractmethod
    def invoke(
        self,
        tool_name: str,
        arguments: dict[str, Any],
        *,
        agent_read_only: bool = True,
    ) -> dict[str, Any]:
        """Invoke a tool after permission evaluation."""


class SchedulerPort(ABC):
    """Port for scheduling deferred or recurring runtime work."""

    @abstractmethod
    def schedule(self, job_id: str, payload: dict[str, Any], *, delay_seconds: int = 0) -> None:
        """Schedule a job for later execution."""

    @abstractmethod
    def list_jobs(self) -> list[dict[str, Any]]:
        """Return scheduled jobs."""


class MetricsPort(ABC):
    """Port for collecting operational metrics."""

    @abstractmethod
    def record_execution(self, *, success: bool) -> None:
        """Record an execution outcome."""

    @abstractmethod
    def snapshot(self) -> MetricSnapshot:
        """Return the current metrics snapshot."""


class ModelProviderPort(ABC):
    """
    Provider-neutral model adapter interface.

    Concrete vendor SDKs must implement this port in infrastructure adapters.
    Core domain and application layers must never import vendor clients.
    """

    @abstractmethod
    def complete(self, *, prompt: str, model_class: str, max_tokens: int) -> str:
        """Generate a completion for the given prompt."""

    @abstractmethod
    def provider_name(self) -> str:
        """Return the adapter name without implying a vendor dependency."""
