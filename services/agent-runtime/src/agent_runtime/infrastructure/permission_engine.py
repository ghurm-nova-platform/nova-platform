"""Permission / policy engine with secure-by-default rules."""

from __future__ import annotations

from agent_runtime.domain.enums import PermissionAction, RiskLevel
from agent_runtime.domain.interfaces import PermissionEnginePort
from agent_runtime.domain.models import AgentDefinition
from agent_runtime.shared.logging import get_logger

logger = get_logger(__name__)

_WRITE_ACTIONS = {PermissionAction.WRITE, PermissionAction.DEPLOY}


class DefaultPermissionEngine(PermissionEnginePort):
    """
    Least-privilege permission engine.

    - Agent execution is always evaluated as EXECUTE (caller responsibility).
    - ``read_only_default`` blocks write-capable actions/tools, not EXECUTE.
    - ``dry_run`` never bypasses elevated-risk or write restrictions.
    """

    def evaluate(
        self,
        *,
        actor: str,
        action: PermissionAction,
        risk_level: RiskLevel,
        agent: AgentDefinition,
        dry_run: bool = False,
    ) -> bool:
        """Evaluate whether the actor may perform the action."""

        _ = dry_run  # Explicitly ignored: clients cannot bypass policy via dry_run.
        _ = risk_level  # Elevated risk is gated by workflow approval, not denial here.

        declared = set(agent.required_permissions) | {PermissionAction.READ}
        if action not in declared:
            logger.info(
                "permission_denied",
                reason="action_not_declared",
                actor=actor,
                action=action,
                agent_id=agent.agent_id,
            )
            return False

        if agent.read_only_default and action in _WRITE_ACTIONS:
            logger.info(
                "permission_denied",
                reason="read_only_default_blocks_write",
                actor=actor,
                action=action,
                agent_id=agent.agent_id,
            )
            return False

        return True
