"""In-memory project memory adapter."""

from __future__ import annotations

from typing import Any

from agent_runtime.domain.interfaces import MemoryPort


class InMemoryMemoryStore(MemoryPort):
    """Ephemeral key/value memory for foundation development."""

    def __init__(self) -> None:
        self._store: dict[str, dict[str, Any]] = {}

    def store(self, key: str, value: dict[str, Any]) -> None:
        """Store a memory document under the given key."""

        self._store[key] = value

    def retrieve(self, key: str) -> dict[str, Any] | None:
        """Retrieve a memory document or None when missing."""

        return self._store.get(key)
