"""In-memory knowledge store implementing the knowledge port."""

from __future__ import annotations

from typing import Any

from agent_runtime.domain.interfaces import KnowledgePort


class InMemoryKnowledgeStore(KnowledgePort):
    """
    Naive keyword knowledge index.

    Replaced later by a vector store adapter (for example Qdrant) without
    changing application contracts.
    """

    def __init__(self) -> None:
        self._items: dict[str, dict[str, Any]] = {}

    def upsert(self, item_id: str, content: dict[str, Any]) -> None:
        """Insert or replace a knowledge item."""

        self._items[item_id] = content

    def search(self, query: str, *, limit: int = 5) -> list[dict[str, Any]]:
        """Return items whose serialized content contains query tokens."""

        tokens = [token.lower() for token in query.split() if token]
        matches: list[dict[str, Any]] = []
        for item_id, content in self._items.items():
            blob = f"{item_id} {content}".lower()
            if not tokens or any(token in blob for token in tokens):
                matches.append({"id": item_id, **content})
            if len(matches) >= limit:
                break
        return matches
