"""Authentication context and API-key security boundary."""

from __future__ import annotations

import secrets
from dataclasses import dataclass

from fastapi import Request

from agent_runtime.shared.config import Settings
from agent_runtime.shared.exceptions import AuthenticationError


@dataclass(frozen=True, slots=True)
class AuthContext:
    """
    Authenticated request identity.

    Actor is derived exclusively from the validated API key configuration,
    never from client-supplied request bodies.
    """

    actor: str
    auth_method: str = "api_key"


def authenticate_request(request: Request, settings: Settings) -> AuthContext:
    """
    Validate the internal API key header and return the trusted actor.

    Raises AuthenticationError when the key is missing or invalid.
    """

    configured = settings.internal_api_key
    if not configured:
        raise AuthenticationError("Internal API key is not configured")

    header_name = settings.api_key_header
    provided = request.headers.get(header_name)
    if provided is None or provided.strip() == "":
        raise AuthenticationError("Missing API key")

    if not secrets.compare_digest(provided.strip(), configured):
        raise AuthenticationError("Invalid API key")

    return AuthContext(actor=settings.service_actor, auth_method="api_key")
