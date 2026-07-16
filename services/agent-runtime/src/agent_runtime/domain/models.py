"""Domain entities for agents, workflows, plans, and executions."""

from __future__ import annotations

from datetime import UTC, datetime
from typing import Any
from uuid import uuid4

from pydantic import BaseModel, Field

from agent_runtime.domain.enums import (
    AgentHealth,
    ModelClass,
    PermissionAction,
    RiskLevel,
    WorkflowState,
)


def utc_now() -> datetime:
    """Return the current UTC timestamp."""

    return datetime.now(UTC)


class AgentDefinition(BaseModel):
    """Registered agent metadata as required by the runtime specification."""

    agent_id: str
    version: str
    display_name: str
    description: str
    capabilities: list[str] = Field(default_factory=list)
    input_schema: dict[str, Any] = Field(default_factory=dict)
    output_schema: dict[str, Any] = Field(default_factory=dict)
    required_tools: list[str] = Field(default_factory=list)
    required_permissions: list[PermissionAction] = Field(
        default_factory=lambda: [PermissionAction.READ]
    )
    supported_model_classes: list[ModelClass] = Field(
        default_factory=lambda: [ModelClass.GENERAL]
    )
    max_execution_seconds: int = 300
    retry_limit: int = 1
    cost_limit_usd: float = 1.0
    token_limit: int = 100_000
    data_classification_limit: str = "internal"
    events_consumed: list[str] = Field(default_factory=list)
    events_emitted: list[str] = Field(default_factory=list)
    evaluation_rules: list[str] = Field(default_factory=list)
    health: AgentHealth = AgentHealth.HEALTHY
    read_only_default: bool = True
    created_at: datetime = Field(default_factory=utc_now)
    updated_at: datetime = Field(default_factory=utc_now)


class PlanTask(BaseModel):
    """A single planned task produced by the planner."""

    task_id: str = Field(default_factory=lambda: str(uuid4()))
    title: str
    description: str
    agent_id: str | None = None
    depends_on: list[str] = Field(default_factory=list)
    risk_level: RiskLevel = RiskLevel.LOW
    requires_approval: bool = False
    acceptance_evidence: list[str] = Field(default_factory=list)


class ExecutionPlan(BaseModel):
    """Structured plan transforming a goal into ordered tasks."""

    plan_id: str = Field(default_factory=lambda: str(uuid4()))
    goal: str
    assumptions: list[str] = Field(default_factory=list)
    risks: list[str] = Field(default_factory=list)
    tasks: list[PlanTask] = Field(default_factory=list)
    created_at: datetime = Field(default_factory=utc_now)


class Workflow(BaseModel):
    """Durable workflow instance tracked by the workflow engine."""

    workflow_id: str = Field(default_factory=lambda: str(uuid4()))
    name: str
    goal: str
    state: WorkflowState = WorkflowState.CREATED
    agent_id: str | None = None
    plan: ExecutionPlan | None = None
    risk_level: RiskLevel = RiskLevel.LOW
    metadata: dict[str, Any] = Field(default_factory=dict)
    created_at: datetime = Field(default_factory=utc_now)
    updated_at: datetime = Field(default_factory=utc_now)


class ExecutionRequest(BaseModel):
    """Request to execute a registered agent against a goal."""

    execution_id: str = Field(default_factory=lambda: str(uuid4()))
    agent_id: str
    goal: str
    inputs: dict[str, Any] = Field(default_factory=dict)
    actor: str = "system"
    organization_id: str | None = None
    project_id: str | None = None
    risk_level: RiskLevel = RiskLevel.LOW
    dry_run: bool = False


class ExecutionResult(BaseModel):
    """Outcome of an agent execution attempt."""

    execution_id: str
    agent_id: str
    workflow_id: str | None = None
    status: WorkflowState
    output: dict[str, Any] = Field(default_factory=dict)
    message: str
    started_at: datetime = Field(default_factory=utc_now)
    completed_at: datetime | None = None


class RuntimeEvent(BaseModel):
    """Domain event published on the event bus."""

    event_id: str = Field(default_factory=lambda: str(uuid4()))
    event_type: str
    source: str
    payload: dict[str, Any] = Field(default_factory=dict)
    correlation_id: str | None = None
    created_at: datetime = Field(default_factory=utc_now)


class MetricSnapshot(BaseModel):
    """Point-in-time operational metrics for the runtime."""

    agents_registered: int
    workflows_total: int
    workflows_by_state: dict[str, int]
    executions_total: int
    executions_succeeded: int
    executions_failed: int
    events_published: int
    collected_at: datetime = Field(default_factory=utc_now)
