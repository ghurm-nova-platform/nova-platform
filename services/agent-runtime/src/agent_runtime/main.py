"""FastAPI application factory and process entrypoint."""

from __future__ import annotations

from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI

from agent_runtime import __version__
from agent_runtime.api.errors import register_exception_handlers
from agent_runtime.api.middleware import CorrelationIdMiddleware
from agent_runtime.api.routes import router
from agent_runtime.application.container import build_container
from agent_runtime.shared.config import Settings, get_settings
from agent_runtime.shared.logging import configure_logging, get_logger


def create_app(settings: Settings | None = None) -> FastAPI:
    """Create and configure the FastAPI application."""

    resolved = settings or get_settings()
    configure_logging(log_level=resolved.log_level, json_logs=resolved.log_json)
    logger = get_logger(__name__)

    @asynccontextmanager
    async def lifespan(_app: FastAPI) -> AsyncIterator[None]:
        logger.info(
            "agent_runtime_started",
            app=resolved.app_name,
            env=resolved.app_env,
            version=resolved.app_version,
        )
        yield
        logger.info("agent_runtime_stopped")

    app = FastAPI(
        title="Nova Agent Runtime",
        description=(
            "Provider-neutral agent orchestration foundation for Nova Platform. "
            "Registers agents, plans workflows, evaluates permissions, and emits events."
        ),
        version=__version__,
        docs_url="/docs",
        redoc_url="/redoc",
        openapi_url="/openapi.json",
        lifespan=lifespan,
    )
    app.state.container = build_container(resolved)
    app.add_middleware(
        CorrelationIdMiddleware,
        header_name=resolved.correlation_header,
    )
    register_exception_handlers(app)
    app.include_router(router)
    return app


def run() -> None:
    """Run the service with uvicorn using environment configuration."""

    settings = get_settings()
    uvicorn.run(
        "agent_runtime.main:create_app",
        factory=True,
        host=settings.host,
        port=settings.port,
        log_level=settings.log_level.lower(),
    )


app = create_app()
