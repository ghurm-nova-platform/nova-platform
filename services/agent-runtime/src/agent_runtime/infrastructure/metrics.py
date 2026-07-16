"""In-process metrics collector."""

from __future__ import annotations

from collections import Counter

from agent_runtime.domain.interfaces import (
    AgentRegistryPort,
    EventBusPort,
    MetricsPort,
    WorkflowEnginePort,
)
from agent_runtime.domain.models import MetricSnapshot


class InMemoryMetrics(MetricsPort):
    """Collects execution counters and composes a runtime snapshot."""

    def __init__(
        self,
        *,
        agent_registry: AgentRegistryPort,
        workflow_engine: WorkflowEnginePort,
        event_bus: EventBusPort,
    ) -> None:
        self._agent_registry = agent_registry
        self._workflow_engine = workflow_engine
        self._event_bus = event_bus
        self._executions_total = 0
        self._executions_succeeded = 0
        self._executions_failed = 0

    def record_execution(self, *, success: bool) -> None:
        """Increment execution counters."""

        self._executions_total += 1
        if success:
            self._executions_succeeded += 1
        else:
            self._executions_failed += 1

    def snapshot(self) -> MetricSnapshot:
        """Build a metrics snapshot from current runtime state."""

        workflows = self._workflow_engine.list_workflows()
        by_state = Counter(workflow.state.value for workflow in workflows)
        return MetricSnapshot(
            agents_registered=len(self._agent_registry.list_agents()),
            workflows_total=len(workflows),
            workflows_by_state=dict(by_state),
            executions_total=self._executions_total,
            executions_succeeded=self._executions_succeeded,
            executions_failed=self._executions_failed,
            events_published=len(self._event_bus.list_events(limit=10_000)),
        )
