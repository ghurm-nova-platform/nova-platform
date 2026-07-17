# ADR-0007: Secure Tool Calling

## Status

Accepted (Sprint 1 Phase 7)

## Context

Agents need tools, but arbitrary code, shell, SQL, HTTP, or browser-side execution is unsafe. Platform API must remain the only orchestration and authorization authority.

## Decision

1. Register tools as data (schemas + policy + `executor_key`).
2. Resolve executors only from a Spring-managed allowlist of `ToolExecutor` beans.
3. Orchestrate tool calls inside Platform API after provider-neutral runtime turn results.
4. Persist immutable-ish `execution_tool_calls` with bounded payloads and safe error codes.
5. Support optional human approval without long-blocking HTTP waits.
6. Keep NoOp runtime deterministic for tests; no real third-party integrations in this phase.

## Consequences

- Strong security boundary and tenant isolation.
- Future integrations can add new allowlisted executors without browser changes.
- Real LLM providers must emit the same `RuntimeTurnResult` contract.
