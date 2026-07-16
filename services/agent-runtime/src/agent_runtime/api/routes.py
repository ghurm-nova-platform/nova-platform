"""REST route handlers for the agent runtime foundation."""

from __future__ import annotations

from datetime import UTC, datetime

from fastapi import APIRouter

from agent_runtime.api.dependencies import (
    AgentServiceDep,
    ExecutionServiceDep,
    MetricsServiceDep,
    WorkflowServiceDep,
)
from agent_runtime.api.schemas import (
    AgentExecuteRequest,
    AgentRegisterRequest,
    AgentResponse,
    ExecutionResponse,
    HealthResponse,
    MetricsResponse,
    WorkflowCreateRequest,
    WorkflowResponse,
)
from agent_runtime.domain.models import AgentDefinition, ExecutionRequest
from agent_runtime.shared.config import get_settings
from agent_runtime.shared.logging import get_logger

logger = get_logger(__name__)

router = APIRouter()


@router.get(
    "/health",
    response_model=HealthResponse,
    tags=["health"],
    summary="Service health",
)
async def health() -> HealthResponse:
    """Return process health for readiness and liveness probes."""

    settings = get_settings()
    return HealthResponse(
        status="UP",
        service=settings.app_name,
        version=settings.app_version,
        timestamp=datetime.now(UTC),
    )


@router.get(
    "/agents",
    response_model=list[AgentResponse],
    tags=["agents"],
    summary="List registered agents",
)
async def list_agents(service: AgentServiceDep) -> list[AgentResponse]:
    """List all agents in the registry."""

    agents = service.list_agents()
    logger.info("agents_listed", count=len(agents))
    return [AgentResponse.model_validate(agent.model_dump()) for agent in agents]


@router.post(
    "/agents/register",
    response_model=AgentResponse,
    status_code=201,
    tags=["agents"],
    summary="Register an agent",
)
async def register_agent(
    payload: AgentRegisterRequest,
    service: AgentServiceDep,
) -> AgentResponse:
    """Register a new agent definition."""

    agent = AgentDefinition.model_validate(payload.model_dump())
    registered = service.register(agent)
    return AgentResponse.model_validate(registered.model_dump())


@router.post(
    "/agents/execute",
    response_model=ExecutionResponse,
    tags=["agents"],
    summary="Execute an agent",
)
async def execute_agent(
    payload: AgentExecuteRequest,
    service: ExecutionServiceDep,
) -> ExecutionResponse:
    """Execute a registered agent against a goal under policy controls."""

    request = ExecutionRequest.model_validate(payload.model_dump())
    result = service.execute(request)
    return ExecutionResponse.model_validate(result.model_dump())


@router.get(
    "/workflows",
    response_model=list[WorkflowResponse],
    tags=["workflows"],
    summary="List workflows",
)
async def list_workflows(service: WorkflowServiceDep) -> list[WorkflowResponse]:
    """List all workflows tracked by the runtime."""

    workflows = service.list_workflows()
    return [WorkflowResponse.model_validate(item.model_dump()) for item in workflows]


@router.post(
    "/workflows",
    response_model=WorkflowResponse,
    status_code=201,
    tags=["workflows"],
    summary="Create a workflow",
)
async def create_workflow(
    payload: WorkflowCreateRequest,
    service: WorkflowServiceDep,
) -> WorkflowResponse:
    """Create a queued workflow instance."""

    workflow = service.create_workflow(
        name=payload.name,
        goal=payload.goal,
        agent_id=payload.agent_id,
        risk_level=payload.risk_level,
    )
    return WorkflowResponse.model_validate(workflow.model_dump())


@router.get(
    "/metrics",
    response_model=MetricsResponse,
    tags=["metrics"],
    summary="Runtime metrics",
)
async def get_metrics(service: MetricsServiceDep) -> MetricsResponse:
    """Return operational metrics for the agent runtime."""

    snapshot = service.get_metrics()
    return MetricsResponse.model_validate(snapshot.model_dump())
