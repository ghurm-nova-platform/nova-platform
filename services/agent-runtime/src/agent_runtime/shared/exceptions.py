"""Shared exception types for the agent runtime."""

from __future__ import annotations


class AgentRuntimeError(Exception):
    """Base error for agent runtime failures."""

    def __init__(self, message: str, *, code: str = "RUNTIME_ERROR") -> None:
        super().__init__(message)
        self.message = message
        self.code = code


class NotFoundError(AgentRuntimeError):
    """Raised when a requested resource does not exist."""

    def __init__(self, message: str) -> None:
        super().__init__(message, code="NOT_FOUND")


class ConflictError(AgentRuntimeError):
    """Raised when a create/update conflicts with existing state."""

    def __init__(self, message: str) -> None:
        super().__init__(message, code="CONFLICT")


class PermissionDeniedError(AgentRuntimeError):
    """Raised when a policy evaluation rejects an action."""

    def __init__(self, message: str) -> None:
        super().__init__(message, code="PERMISSION_DENIED")


class AuthenticationError(AgentRuntimeError):
    """Raised when the internal API authentication boundary rejects a request."""

    def __init__(self, message: str) -> None:
        super().__init__(message, code="UNAUTHORIZED")


class ValidationAppError(AgentRuntimeError):
    """Raised when domain validation fails outside request schemas."""

    def __init__(self, message: str) -> None:
        super().__init__(message, code="VALIDATION_ERROR")
