"""Unit tests for infrastructure adapters."""

from __future__ import annotations

import pytest

from agent_runtime.application.container import build_container
from agent_runtime.domain.enums import PermissionAction, RiskLevel, WorkflowState
from agent_runtime.domain.models import AgentDefinition, ExecutionRequest, Workflow
from agent_runtime.infrastructure.permission_engine import DefaultPermissionEngine
from agent_runtime.infrastructure.planner import DeterministicPlanner
from agent_runtime.infrastructure.tool_gateway import InMemoryToolGateway
from agent_runtime.infrastructure.workflow_engine import InMemoryWorkflowEngine
from agent_runtime.shared.config import Settings
from agent_runtime.shared.exceptions import PermissionDeniedError, ValidationAppError


def test_planner_marks_approval_for_high_risk() -> None:
    planner = DeterministicPlanner()
    plan = planner.create_plan("Ship migration", risk_level=RiskLevel.HIGH)
    assert any(task.requires_approval for task in plan.tasks)
    assert plan.risks


def test_workflow_transition_validation() -> None:
    engine = InMemoryWorkflowEngine()
    workflow = engine.create(Workflow(name="w", goal="g"))
    engine.transition(workflow.workflow_id, WorkflowState.QUEUED.value)
    with pytest.raises(ValidationAppError):
        engine.transition(workflow.workflow_id, WorkflowState.COMPLETED.value)


def test_permission_engine_allows_execute_for_readonly_agent() -> None:
    engine = DefaultPermissionEngine()
    agent = AgentDefinition(
        agent_id="readonly",
        version="1.0.0",
        display_name="Readonly",
        description="Readonly agent",
        required_permissions=[PermissionAction.READ, PermissionAction.EXECUTE],
        read_only_default=True,
    )
    assert (
        engine.evaluate(
            actor="user",
            action=PermissionAction.EXECUTE,
            risk_level=RiskLevel.LOW,
            agent=agent,
            dry_run=False,
        )
        is True
    )


def test_permission_engine_denies_write_for_readonly_agent() -> None:
    engine = DefaultPermissionEngine()
    agent = AgentDefinition(
        agent_id="readonly",
        version="1.0.0",
        display_name="Readonly",
        description="Readonly agent",
        required_permissions=[
            PermissionAction.READ,
            PermissionAction.WRITE,
            PermissionAction.EXECUTE,
        ],
        read_only_default=True,
    )
    assert (
        engine.evaluate(
            actor="user",
            action=PermissionAction.WRITE,
            risk_level=RiskLevel.LOW,
            agent=agent,
            dry_run=False,
        )
        is False
    )
    # dry_run must not bypass write restrictions.
    assert (
        engine.evaluate(
            actor="user",
            action=PermissionAction.WRITE,
            risk_level=RiskLevel.LOW,
            agent=agent,
            dry_run=True,
        )
        is False
    )


def test_dry_run_does_not_bypass_high_risk_approval_gate() -> None:
    container = build_container(Settings(APP_ENV="test", LOG_LEVEL="WARNING"))
    container.agent_service.register(
        AgentDefinition(
            agent_id="high.risk",
            version="1.0.0",
            display_name="High Risk",
            description="High risk agent",
            required_permissions=[PermissionAction.READ, PermissionAction.EXECUTE],
        )
    )
    result = container.execution_service.execute(
        ExecutionRequest(
            agent_id="high.risk",
            goal="Risky work",
            actor="trusted-actor",
            risk_level=RiskLevel.HIGH,
            dry_run=True,
        )
    )
    assert result.status == WorkflowState.WAITING_FOR_APPROVAL


def test_tool_gateway_denies_write_tools_for_readonly_agents() -> None:
    gateway = InMemoryToolGateway(allow_writes=True)
    assert "code.search" in gateway.list_tools()
    with pytest.raises(PermissionDeniedError):
        gateway.invoke("repo.write_file", {"path": "x.py"}, agent_read_only=True)
