"""Shared pytest fixtures for the agent runtime."""

from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from agent_runtime.application.container import build_container
from agent_runtime.main import create_app
from agent_runtime.shared.config import Settings

TEST_API_KEY = "test-internal-api-key"
TEST_ACTOR = "agent-runtime-test-actor"


@pytest.fixture
def settings() -> Settings:
    """Return isolated settings for tests."""

    return Settings(
        APP_NAME="nova-agent-runtime-test",
        APP_ENV="test",
        LOG_LEVEL="WARNING",
        LOG_JSON=False,
        PORT=8090,
        INTERNAL_API_KEY=TEST_API_KEY,
        API_KEY_HEADER="X-API-Key",
        SERVICE_ACTOR=TEST_ACTOR,
    )


@pytest.fixture
def auth_headers() -> dict[str, str]:
    """Return valid authentication headers for protected endpoints."""

    return {"X-API-Key": TEST_API_KEY}


@pytest.fixture
def client(settings: Settings) -> TestClient:
    """Return an HTTP test client with a fresh dependency container."""

    app = create_app(settings)
    app.state.container = build_container(settings)
    with TestClient(app) as test_client:
        yield test_client
