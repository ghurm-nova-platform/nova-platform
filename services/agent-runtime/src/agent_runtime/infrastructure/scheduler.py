"""In-memory scheduler for deferred runtime jobs."""

from __future__ import annotations

from datetime import UTC, datetime, timedelta
from typing import Any

from agent_runtime.domain.interfaces import SchedulerPort


class InMemoryScheduler(SchedulerPort):
    """Records scheduled jobs without a background worker (foundation only)."""

    def __init__(self) -> None:
        self._jobs: dict[str, dict[str, Any]] = {}

    def schedule(self, job_id: str, payload: dict[str, Any], *, delay_seconds: int = 0) -> None:
        """Schedule a job for a future run time."""

        run_at = datetime.now(UTC) + timedelta(seconds=delay_seconds)
        self._jobs[job_id] = {
            "job_id": job_id,
            "payload": payload,
            "run_at": run_at.isoformat(),
            "status": "SCHEDULED",
        }

    def list_jobs(self) -> list[dict[str, Any]]:
        """Return all scheduled jobs."""

        return list(self._jobs.values())
