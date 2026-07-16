"""Tool gateway with allowlisted stub tools."""

from __future__ import annotations

from typing import Any

from agent_runtime.application.risk import is_write_capable_tool
from agent_runtime.domain.interfaces import ToolGatewayPort
from agent_runtime.shared.exceptions import NotFoundError, PermissionDeniedError


class InMemoryToolGateway(ToolGatewayPort):
    """
    Controlled tool gateway with read-only stubs.

    Write/execute tools are registered but denied for read-only agents and
    until policy grants elevated write access.
    """

    def __init__(self, *, allow_writes: bool = False) -> None:
        self._allow_writes = allow_writes
        self._tools: dict[str, bool] = {
            "code.search": True,
            "docs.read": True,
            "repo.read_file": True,
            "repo.write_file": False,
            "terminal.execute": False,
        }

    def list_tools(self) -> list[str]:
        """Return registered tool names."""

        return sorted(self._tools)

    def invoke(
        self,
        tool_name: str,
        arguments: dict[str, Any],
        *,
        agent_read_only: bool = True,
    ) -> dict[str, Any]:
        """Invoke an allowlisted tool or raise when unavailable/denied."""

        if tool_name not in self._tools:
            raise NotFoundError(f"Tool '{tool_name}' is not registered")

        tool_is_read_only = self._tools[tool_name]
        write_tool = (not tool_is_read_only) or is_write_capable_tool(tool_name)
        if write_tool and (agent_read_only or not self._allow_writes):
            raise PermissionDeniedError(
                f"Tool '{tool_name}' requires elevated write permissions"
            )

        return {
            "tool": tool_name,
            "status": "ok",
            "arguments": arguments,
            "result": f"stub-result-for-{tool_name}",
        }
