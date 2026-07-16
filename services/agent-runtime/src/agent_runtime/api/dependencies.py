"""FastAPI dependency providers."""

from __future__ import annotations

from typing import Annotated

from fastapi import Depends, Request

from agent_runtime.api.auth import AuthContext, authenticate_request
from agent_runtime.application.container import Container
from agent_runtime.application.services import (
    AgentService,
    ExecutionService,
    MetricsService,
    WorkflowService,
)
from agent_runtime.shared.config import Settings


def get_container(request: Request) -> Container:
    """Resolve the application container from app state."""

    return request.app.state.container  # type: ignore[no-any-return]


ContainerDep = Annotated[Container, Depends(get_container)]


def get_settings_dep(container: ContainerDep) -> Settings:
    """Provide application settings."""

    return container.settings


def require_auth(
    request: Request,
    settings: Annotated[Settings, Depends(get_settings_dep)],
) -> AuthContext:
    """Enforce the internal API-key authentication boundary."""

    return authenticate_request(request, settings)


AuthDep = Annotated[AuthContext, Depends(require_auth)]


def get_agent_service(container: ContainerDep) -> AgentService:
    """Provide the agent application service."""

    return container.agent_service


def get_workflow_service(container: ContainerDep) -> WorkflowService:
    """Provide the workflow application service."""

    return container.workflow_service


def get_execution_service(container: ContainerDep) -> ExecutionService:
    """Provide the execution application service."""

    return container.execution_service


def get_metrics_service(container: ContainerDep) -> MetricsService:
    """Provide the metrics application service."""

    return container.metrics_service


AgentServiceDep = Annotated[AgentService, Depends(get_agent_service)]
WorkflowServiceDep = Annotated[WorkflowService, Depends(get_workflow_service)]
ExecutionServiceDep = Annotated[ExecutionService, Depends(get_execution_service)]
MetricsServiceDep = Annotated[MetricsService, Depends(get_metrics_service)]
