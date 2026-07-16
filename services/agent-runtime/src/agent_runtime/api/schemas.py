"""Request and response DTOs with Pydantic v2 validation."""

from __future__ import annotations

from datetime import datetime
from typing import Any

from pydantic import BaseModel, ConfigDict, Field

from agent_runtime.domain.enums import (
    AgentHealth,
    ModelClass,
    PermissionAction,
    RiskLevel,
    WorkflowState,
)


class HealthResponse(BaseModel):
    """Health check payload."""

    status: str = Field(examples=["UP"])
    service: str = Field(examples=["nova-agent-runtime"])
    version: str
    timestamp: datetime


class AgentRegisterRequest(BaseModel):
    """Payload for registering an agent."""

    agent_id: str = Field(min_length=1, max_length=128, pattern=r"^[a-z0-9][a-z0-9_.-]*$")
    version: str = Field(min_length=1, max_length=64)
    display_name: str = Field(min_length=1, max_length=256)
    description: str = Field(min_length=1, max_length=2000)
    capabilities: list[str] = Field(default_factory=list)
    input_schema: dict[str, Any] = Field(default_factory=dict)
    output_schema: dict[str, Any] = Field(default_factory=dict)
    required_tools: list[str] = Field(default_factory=list)
    required_permissions: list[PermissionAction] = Field(
        default_factory=lambda: [PermissionAction.READ, PermissionAction.EXECUTE]
    )
    supported_model_classes: list[ModelClass] = Field(
        default_factory=lambda: [ModelClass.GENERAL]
    )
    max_execution_seconds: int = Field(default=300, ge=1, le=86_400)
    retry_limit: int = Field(default=1, ge=0, le=10)
    cost_limit_usd: float = Field(default=1.0, ge=0)
    token_limit: int = Field(default=100_000, ge=1)
    data_classification_limit: str = "internal"
    events_consumed: list[str] = Field(default_factory=list)
    events_emitted: list[str] = Field(default_factory=list)
    evaluation_rules: list[str] = Field(default_factory=list)
    health: AgentHealth = AgentHealth.HEALTHY
    read_only_default: bool = True


class AgentResponse(BaseModel):
    """Registered agent representation."""

    agent_id: str
    version: str
    display_name: str
    description: str
    capabilities: list[str]
    required_tools: list[str]
    required_permissions: list[PermissionAction]
    supported_model_classes: list[ModelClass]
    health: AgentHealth
    read_only_default: bool
    created_at: datetime
    updated_at: datetime


class AgentExecuteRequest(BaseModel):
    """
    Payload for executing a registered agent.

    Actor identity and dry-run are intentionally absent. Actor is taken from the
    authenticated request context. Clients cannot supply dry_run to bypass policy.
    """

    model_config = ConfigDict(extra="forbid")

    agent_id: str = Field(min_length=1)
    goal: str = Field(min_length=1, max_length=4000)
    inputs: dict[str, Any] = Field(default_factory=dict)
    organization_id: str | None = None
    project_id: str | None = None
    risk_level: RiskLevel = RiskLevel.LOW


class ExecutionResponse(BaseModel):
    """Agent execution result."""

    execution_id: str
    agent_id: str
    workflow_id: str | None
    status: WorkflowState
    output: dict[str, Any]
    message: str
    started_at: datetime
    completed_at: datetime | None


class WorkflowCreateRequest(BaseModel):
    """Payload for creating a workflow."""

    name: str = Field(min_length=1, max_length=256)
    goal: str = Field(min_length=1, max_length=4000)
    agent_id: str | None = None
    risk_level: RiskLevel = RiskLevel.LOW


class WorkflowResponse(BaseModel):
    """Workflow representation."""

    workflow_id: str
    name: str
    goal: str
    state: WorkflowState
    agent_id: str | None
    risk_level: RiskLevel
    metadata: dict[str, Any]
    created_at: datetime
    updated_at: datetime


class MetricsResponse(BaseModel):
    """Operational metrics payload."""

    agents_registered: int
    workflows_total: int
    workflows_by_state: dict[str, int]
    executions_total: int
    executions_succeeded: int
    executions_failed: int
    events_published: int
    collected_at: datetime


class ApiErrorResponse(BaseModel):
    """Standard API error contract."""

    timestamp: datetime
    status: int
    error: str
    code: str
    message: str
    path: str
    correlation_id: str | None = None
    details: list[dict[str, Any]] | None = None
