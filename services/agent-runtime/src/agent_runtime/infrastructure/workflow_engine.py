"""In-memory workflow engine adapter."""

from __future__ import annotations

from agent_runtime.domain.enums import WorkflowState
from agent_runtime.domain.interfaces import WorkflowEnginePort
from agent_runtime.domain.models import Workflow, utc_now
from agent_runtime.shared.exceptions import NotFoundError, ValidationAppError

_ALLOWED_TRANSITIONS: dict[WorkflowState, set[WorkflowState]] = {
    WorkflowState.CREATED: {
        WorkflowState.QUEUED,
        WorkflowState.PLANNING,
        WorkflowState.CANCELLED,
    },
    WorkflowState.QUEUED: {
        WorkflowState.PLANNING,
        WorkflowState.RUNNING,
        WorkflowState.CANCELLED,
    },
    WorkflowState.PLANNING: {
        WorkflowState.WAITING_FOR_APPROVAL,
        WorkflowState.RUNNING,
        WorkflowState.FAILED,
        WorkflowState.CANCELLED,
    },
    WorkflowState.WAITING_FOR_APPROVAL: {
        WorkflowState.RUNNING,
        WorkflowState.CANCELLED,
        WorkflowState.ESCALATED,
    },
    WorkflowState.RUNNING: {
        WorkflowState.WAITING_FOR_TOOL,
        WorkflowState.VALIDATING,
        WorkflowState.REVIEWING,
        WorkflowState.COMPLETED,
        WorkflowState.FAILED,
        WorkflowState.CANCELLED,
    },
    WorkflowState.WAITING_FOR_TOOL: {
        WorkflowState.RUNNING,
        WorkflowState.FAILED,
        WorkflowState.CANCELLED,
    },
    WorkflowState.VALIDATING: {
        WorkflowState.REVIEWING,
        WorkflowState.COMPLETED,
        WorkflowState.FAILED,
    },
    WorkflowState.REVIEWING: {
        WorkflowState.COMPLETED,
        WorkflowState.FAILED,
        WorkflowState.ESCALATED,
    },
    WorkflowState.COMPLETED: set(),
    WorkflowState.FAILED: set(),
    WorkflowState.CANCELLED: set(),
    WorkflowState.ESCALATED: {
        WorkflowState.WAITING_FOR_APPROVAL,
        WorkflowState.CANCELLED,
    },
}


class InMemoryWorkflowEngine(WorkflowEnginePort):
    """In-memory workflow state machine for foundation development."""

    def __init__(self) -> None:
        self._workflows: dict[str, Workflow] = {}

    def create(self, workflow: Workflow) -> Workflow:
        """Create a workflow instance."""

        self._workflows[workflow.workflow_id] = workflow
        return workflow

    def list_workflows(self) -> list[Workflow]:
        """Return workflows sorted by creation time descending."""

        return sorted(
            self._workflows.values(),
            key=lambda item: item.created_at,
            reverse=True,
        )

    def get(self, workflow_id: str) -> Workflow:
        """Return a workflow or raise NotFoundError."""

        workflow = self._workflows.get(workflow_id)
        if workflow is None:
            raise NotFoundError(f"Workflow '{workflow_id}' was not found")
        return workflow

    def save(self, workflow: Workflow) -> Workflow:
        """Persist an updated workflow document."""

        if workflow.workflow_id not in self._workflows:
            raise NotFoundError(f"Workflow '{workflow.workflow_id}' was not found")
        updated = workflow.model_copy(update={"updated_at": utc_now()})
        self._workflows[workflow.workflow_id] = updated
        return updated

    def transition(self, workflow_id: str, new_state: str) -> Workflow:
        """Apply a validated state transition."""

        workflow = self.get(workflow_id)
        try:
            target = WorkflowState(new_state)
        except ValueError as exc:
            raise ValidationAppError(f"Unknown workflow state '{new_state}'") from exc

        allowed = _ALLOWED_TRANSITIONS.get(workflow.state, set())
        if target not in allowed:
            raise ValidationAppError(
                f"Cannot transition workflow from {workflow.state} to {target}"
            )

        updated = workflow.model_copy(update={"state": target, "updated_at": utc_now()})
        self._workflows[workflow_id] = updated
        return updated
