"""Permission / policy engine with secure-by-default rules."""

from __future__ import annotations

from agent_runtime.domain.enums import PermissionAction, RiskLevel
from agent_runtime.domain.interfaces import PermissionEnginePort
from agent_runtime.domain.models import AgentDefinition
from agent_runtime.shared.logging import get_logger

logger = get_logger(__name__)


class DefaultPermissionEngine(PermissionEnginePort):
    """
    Least-privilege permission engine.

    Defaults: read-only agents may only READ. HIGH/CRITICAL write/execute/deploy
    actions are denied unless dry_run is true (planning-only).
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

        if agent.read_only_default and action is not PermissionAction.READ:
            if dry_run:
                return True
            logger.info(
                "permission_denied",
                reason="read_only_default",
                actor=actor,
                action=action,
                agent_id=agent.agent_id,
            )
            return False

        elevated = risk_level in {RiskLevel.HIGH, RiskLevel.CRITICAL}
        if elevated and action is not PermissionAction.READ:
            if dry_run:
                return True
            logger.info(
                "permission_denied",
                reason="elevated_risk_requires_approval",
                actor=actor,
                action=action,
                risk_level=risk_level,
            )
            return False

        return True
