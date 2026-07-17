"""In-memory event bus adapter."""

from __future__ import annotations

from collections.abc import Callable

from agent_runtime.domain.interfaces import EventBusPort
from agent_runtime.domain.models import RuntimeEvent
from agent_runtime.shared.logging import get_logger

logger = get_logger(__name__)


class InMemoryEventBus(EventBusPort):
    """Simple pub/sub bus that retains events for local inspection."""

    def __init__(self) -> None:
        self._events: list[RuntimeEvent] = []
        self._subscribers: list[Callable[[RuntimeEvent], None]] = []

    def subscribe(self, handler: Callable[[RuntimeEvent], None]) -> None:
        """Register a synchronous event subscriber."""

        self._subscribers.append(handler)

    def publish(self, event: RuntimeEvent) -> None:
        """Publish an event to memory and notify subscribers."""

        self._events.append(event)
        logger.info(
            "event_published",
            event_type=event.event_type,
            event_id=event.event_id,
            correlation_id=event.correlation_id,
        )
        for handler in self._subscribers:
            handler(event)

    def list_events(self, *, limit: int = 100) -> list[RuntimeEvent]:
        """Return the most recent events."""

        return list(reversed(self._events[-limit:]))
