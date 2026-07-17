"""Domain enumerations aligned with the agent runtime specification."""

from enum import StrEnum


class WorkflowState(StrEnum):
    """Lifecycle states for a durable workflow."""

    CREATED = "CREATED"
    QUEUED = "QUEUED"
    PLANNING = "PLANNING"
    WAITING_FOR_APPROVAL = "WAITING_FOR_APPROVAL"
    RUNNING = "RUNNING"
    WAITING_FOR_TOOL = "WAITING_FOR_TOOL"
    VALIDATING = "VALIDATING"
    REVIEWING = "REVIEWING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"
    ESCALATED = "ESCALATED"


class RiskLevel(StrEnum):
    """Risk classification for agent actions."""

    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"


class PermissionAction(StrEnum):
    """Permission actions evaluated by the policy engine."""

    READ = "READ"
    WRITE = "WRITE"
    EXECUTE = "EXECUTE"
    DEPLOY = "DEPLOY"
    ADMIN = "ADMIN"


class AgentHealth(StrEnum):
    """Operational health of a registered agent."""

    HEALTHY = "HEALTHY"
    DEGRADED = "DEGRADED"
    UNAVAILABLE = "UNAVAILABLE"


class ModelClass(StrEnum):
    """Provider-neutral model capability classes."""

    GENERAL = "GENERAL"
    CODE = "CODE"
    REASONING = "REASONING"
    EMBEDDING = "EMBEDDING"
