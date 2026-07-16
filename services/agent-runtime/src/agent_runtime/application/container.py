"""Composition root wiring infrastructure adapters to application services."""

from __future__ import annotations

from dataclasses import dataclass

from agent_runtime.application.services import (
    AgentService,
    ExecutionService,
    MetricsService,
    WorkflowService,
)
from agent_runtime.domain.interfaces import ModelProviderPort
from agent_runtime.infrastructure.agent_registry import InMemoryAgentRegistry
from agent_runtime.infrastructure.event_bus import InMemoryEventBus
from agent_runtime.infrastructure.execution_engine import DefaultExecutionEngine
from agent_runtime.infrastructure.knowledge import InMemoryKnowledgeStore
from agent_runtime.infrastructure.memory import InMemoryMemoryStore
from agent_runtime.infrastructure.metrics import InMemoryMetrics
from agent_runtime.infrastructure.model_provider import NullModelProvider
from agent_runtime.infrastructure.permission_engine import DefaultPermissionEngine
from agent_runtime.infrastructure.planner import DeterministicPlanner
from agent_runtime.infrastructure.scheduler import InMemoryScheduler
from agent_runtime.infrastructure.tool_gateway import InMemoryToolGateway
from agent_runtime.infrastructure.workflow_engine import InMemoryWorkflowEngine
from agent_runtime.shared.config import Settings, get_settings


@dataclass
class Container:
    """Application dependency container."""

    settings: Settings
    agent_service: AgentService
    workflow_service: WorkflowService
    execution_service: ExecutionService
    metrics_service: MetricsService
    model_provider: ModelProviderPort
    tool_gateway: InMemoryToolGateway
    knowledge: InMemoryKnowledgeStore
    scheduler: InMemoryScheduler


def build_container(settings: Settings | None = None) -> Container:
    """Construct the runtime dependency graph."""

    resolved = settings or get_settings()
    registry = InMemoryAgentRegistry()
    workflows = InMemoryWorkflowEngine()
    events = InMemoryEventBus()
    permissions = DefaultPermissionEngine()
    planner = DeterministicPlanner()
    memory = InMemoryMemoryStore()
    knowledge = InMemoryKnowledgeStore()
    tools = InMemoryToolGateway(allow_writes=False)
    scheduler = InMemoryScheduler()
    model_provider: ModelProviderPort = NullModelProvider()
    metrics = InMemoryMetrics(
        agent_registry=registry,
        workflow_engine=workflows,
        event_bus=events,
    )
    execution_engine = DefaultExecutionEngine(
        agent_registry=registry,
        permission_engine=permissions,
        planner=planner,
        workflow_engine=workflows,
        event_bus=events,
        memory=memory,
        metrics=metrics,
        tool_gateway=tools,
    )
    return Container(
        settings=resolved,
        agent_service=AgentService(registry, events),
        workflow_service=WorkflowService(workflows, events),
        execution_service=ExecutionService(execution_engine),
        metrics_service=MetricsService(metrics),
        model_provider=model_provider,
        tool_gateway=tools,
        knowledge=knowledge,
        scheduler=scheduler,
    )
