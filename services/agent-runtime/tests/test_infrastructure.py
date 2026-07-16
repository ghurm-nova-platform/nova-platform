"""Unit tests for infrastructure adapters."""

from __future__ import annotations

import pytest

from agent_runtime.domain.enums import PermissionAction, RiskLevel, WorkflowState
from agent_runtime.domain.models import AgentDefinition, Workflow
from agent_runtime.infrastructure.permission_engine import DefaultPermissionEngine
from agent_runtime.infrastructure.planner import DeterministicPlanner
from agent_runtime.infrastructure.tool_gateway import InMemoryToolGateway
from agent_runtime.infrastructure.workflow_engine import InMemoryWorkflowEngine
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


def test_permission_engine_denies_write_for_readonly_agent() -> None:
    engine = DefaultPermissionEngine()
    agent = AgentDefinition(
        agent_id="readonly",
        version="1.0.0",
        display_name="Readonly",
        description="Readonly agent",
        required_permissions=[PermissionAction.READ, PermissionAction.WRITE],
        read_only_default=True,
    )
    assert engine.evaluate(
        actor="user",
        action=PermissionAction.WRITE,
        risk_level=RiskLevel.LOW,
        agent=agent,
        dry_run=False,
    ) is False
    assert engine.evaluate(
        actor="user",
        action=PermissionAction.WRITE,
        risk_level=RiskLevel.LOW,
        agent=agent,
        dry_run=True,
    ) is True


def test_tool_gateway_denies_write_tools() -> None:
    gateway = InMemoryToolGateway(allow_writes=False)
    assert "code.search" in gateway.list_tools()
    with pytest.raises(PermissionDeniedError):
        gateway.invoke("repo.write_file", {"path": "x.py"})
