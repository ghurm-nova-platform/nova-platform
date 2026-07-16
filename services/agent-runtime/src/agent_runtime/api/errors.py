"""Global exception handlers producing the standard error contract."""

from __future__ import annotations

from datetime import UTC, datetime

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from agent_runtime.api.schemas import ApiErrorResponse
from agent_runtime.shared.exceptions import (
    AgentRuntimeError,
    ConflictError,
    NotFoundError,
    PermissionDeniedError,
    ValidationAppError,
)
from agent_runtime.shared.logging import get_logger

logger = get_logger(__name__)


def _correlation_id(request: Request) -> str | None:
    return getattr(request.state, "correlation_id", None)


def _error_response(
    *,
    request: Request,
    status: int,
    error: str,
    code: str,
    message: str,
    details: list[dict[str, object]] | None = None,
) -> JSONResponse:
    body = ApiErrorResponse(
        timestamp=datetime.now(UTC),
        status=status,
        error=error,
        code=code,
        message=message,
        path=request.url.path,
        correlation_id=_correlation_id(request),
        details=details,
    )
    return JSONResponse(status_code=status, content=body.model_dump(mode="json"))


def register_exception_handlers(app: FastAPI) -> None:
    """Attach exception handlers to the FastAPI application."""

    @app.exception_handler(NotFoundError)
    async def not_found_handler(request: Request, exc: NotFoundError) -> JSONResponse:
        return _error_response(
            request=request,
            status=404,
            error="Not Found",
            code=exc.code,
            message=exc.message,
        )

    @app.exception_handler(ConflictError)
    async def conflict_handler(request: Request, exc: ConflictError) -> JSONResponse:
        return _error_response(
            request=request,
            status=409,
            error="Conflict",
            code=exc.code,
            message=exc.message,
        )

    @app.exception_handler(PermissionDeniedError)
    async def permission_handler(
        request: Request, exc: PermissionDeniedError
    ) -> JSONResponse:
        return _error_response(
            request=request,
            status=403,
            error="Forbidden",
            code=exc.code,
            message=exc.message,
        )

    @app.exception_handler(ValidationAppError)
    async def validation_app_handler(
        request: Request, exc: ValidationAppError
    ) -> JSONResponse:
        return _error_response(
            request=request,
            status=400,
            error="Bad Request",
            code=exc.code,
            message=exc.message,
        )

    @app.exception_handler(AgentRuntimeError)
    async def runtime_handler(request: Request, exc: AgentRuntimeError) -> JSONResponse:
        logger.warning("runtime_error", code=exc.code, message=exc.message)
        return _error_response(
            request=request,
            status=400,
            error="Bad Request",
            code=exc.code,
            message=exc.message,
        )

    @app.exception_handler(RequestValidationError)
    async def request_validation_handler(
        request: Request, exc: RequestValidationError
    ) -> JSONResponse:
        details = [
            {
                "field": ".".join(str(part) for part in err.get("loc", ())),
                "message": err.get("msg"),
            }
            for err in exc.errors()
        ]
        return _error_response(
            request=request,
            status=422,
            error="Unprocessable Entity",
            code="VALIDATION_FAILED",
            message="Request validation failed",
            details=details,
        )

    @app.exception_handler(StarletteHTTPException)
    async def http_handler(request: Request, exc: StarletteHTTPException) -> JSONResponse:
        return _error_response(
            request=request,
            status=exc.status_code,
            error="HTTP Error",
            code="HTTP_ERROR",
            message=str(exc.detail),
        )

    @app.exception_handler(Exception)
    async def unhandled_handler(request: Request, exc: Exception) -> JSONResponse:
        logger.exception("unhandled_error", error=str(exc))
        return _error_response(
            request=request,
            status=500,
            error="Internal Server Error",
            code="INTERNAL_ERROR",
            message="An unexpected error occurred",
        )
