"""Unit tests for application services."""

from __future__ import annotations

from agent_runtime.application.container import build_container
from agent_runtime.domain.models import AgentDefinition, ExecutionRequest
from agent_runtime.shared.config import Settings


def test_agent_service_register_and_list() -> None:
    container = build_container(Settings(APP_ENV="test", LOG_LEVEL="WARNING"))
    agent = AgentDefinition(
        agent_id="app.agent",
        version="1.0.0",
        display_name="App Agent",
        description="Application layer test agent",
    )
    container.agent_service.register(agent)
    assert len(container.agent_service.list_agents()) == 1


def test_execution_service_persists_memory() -> None:
    container = build_container(Settings(APP_ENV="test", LOG_LEVEL="WARNING"))
    container.agent_service.register(
        AgentDefinition(
            agent_id="exec.agent",
            version="1.0.0",
            display_name="Exec",
            description="Execution test",
        )
    )
    result = container.execution_service.execute(
        ExecutionRequest(agent_id="exec.agent", goal="Do work")
    )
    assert result.workflow_id is not None
    metrics = container.metrics_service.get_metrics()
    assert metrics.executions_total == 1
    assert metrics.executions_succeeded == 1
