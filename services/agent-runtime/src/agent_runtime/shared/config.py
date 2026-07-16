"""Application configuration loaded from environment variables."""

from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Runtime configuration externalized through environment variables."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        populate_by_name=True,
    )

    app_name: str = Field(default="nova-agent-runtime", alias="APP_NAME")
    app_env: str = Field(default="local", alias="APP_ENV")
    app_version: str = Field(default="0.1.0", alias="APP_VERSION")
    host: str = Field(default="0.0.0.0", alias="HOST")
    port: int = Field(default=8090, alias="PORT")
    log_level: str = Field(default="INFO", alias="LOG_LEVEL")
    log_json: bool = Field(default=False, alias="LOG_JSON")
    default_agent_readonly: bool = Field(default=True, alias="DEFAULT_AGENT_READONLY")
    max_concurrent_executions: int = Field(default=10, alias="MAX_CONCURRENT_EXECUTIONS")
    correlation_header: str = Field(default="X-Correlation-Id", alias="CORRELATION_HEADER")


@lru_cache
def get_settings() -> Settings:
    """Return a cached settings instance."""

    return Settings()
