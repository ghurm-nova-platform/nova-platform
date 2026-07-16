"""Risk classification helpers that prevent client-side policy downgrades."""

from __future__ import annotations

from agent_runtime.domain.enums import RiskLevel
from agent_runtime.domain.models import AgentDefinition

_WRITE_TOOL_MARKERS = ("write", "execute", "deploy", "terminal", "mutate")

_RISK_RANK = {
    RiskLevel.LOW: 0,
    RiskLevel.MEDIUM: 1,
    RiskLevel.HIGH: 2,
    RiskLevel.CRITICAL: 3,
}


def is_write_capable_tool(tool_name: str) -> bool:
    """Return True when a tool name indicates a mutating capability."""

    lowered = tool_name.lower()
    return any(marker in lowered for marker in _WRITE_TOOL_MARKERS)


def baseline_risk_for_agent(agent: AgentDefinition) -> RiskLevel:
    """
    Compute the server-side baseline risk for an agent.

    Write-capable agents are elevated so clients cannot treat them as LOW.
    """

    if any(is_write_capable_tool(tool) for tool in agent.required_tools):
        return RiskLevel.HIGH
    if not agent.read_only_default:
        return RiskLevel.MEDIUM
    return RiskLevel.LOW


def effective_risk_level(
    agent: AgentDefinition,
    requested: RiskLevel | None,
) -> RiskLevel:
    """
    Resolve effective risk as max(server_baseline, client_requested).

    Clients may raise risk but cannot lower the server baseline.
    """

    baseline = baseline_risk_for_agent(agent)
    if requested is None:
        return baseline
    if _RISK_RANK[requested] >= _RISK_RANK[baseline]:
        return requested
    return baseline
