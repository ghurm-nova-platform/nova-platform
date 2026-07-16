"""FastAPI dependency providers."""

from __future__ import annotations

from typing import Annotated

from fastapi import Depends, Request

from agent_runtime.application.container import Container
from agent_runtime.application.services import (
    AgentService,
    ExecutionService,
    MetricsService,
    WorkflowService,
)


def get_container(request: Request) -> Container:
    """Resolve the application container from app state."""

    return request.app.state.container  # type: ignore[no-any-return]


ContainerDep = Annotated[Container, Depends(get_container)]


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
