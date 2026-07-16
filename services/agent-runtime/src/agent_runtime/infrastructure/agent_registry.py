"""In-memory agent registry adapter."""

from __future__ import annotations

from agent_runtime.domain.interfaces import AgentRegistryPort
from agent_runtime.domain.models import AgentDefinition, utc_now
from agent_runtime.shared.exceptions import ConflictError, NotFoundError


class InMemoryAgentRegistry(AgentRegistryPort):
    """Thread-unsafe in-memory registry suitable for local foundation use."""

    def __init__(self) -> None:
        self._agents: dict[str, AgentDefinition] = {}

    def register(self, agent: AgentDefinition) -> AgentDefinition:
        """Persist a new agent or reject duplicate identifiers."""

        if agent.agent_id in self._agents:
            existing = self._agents[agent.agent_id]
            if existing.version == agent.version:
                raise ConflictError(
                    f"Agent '{agent.agent_id}' version '{agent.version}' already registered"
                )
        updated = agent.model_copy(update={"updated_at": utc_now()})
        self._agents[agent.agent_id] = updated
        return updated

    def list_agents(self) -> list[AgentDefinition]:
        """Return registered agents sorted by identifier."""

        return sorted(self._agents.values(), key=lambda item: item.agent_id)

    def get(self, agent_id: str) -> AgentDefinition:
        """Return an agent or raise NotFoundError."""

        agent = self._agents.get(agent_id)
        if agent is None:
            raise NotFoundError(f"Agent '{agent_id}' was not found")
        return agent
