# ADR-0005: NoOp-first agent execution behind Platform API

## Status

Accepted

## Context

Sprint 1 Phase 5 needs an agent execution path that resolves published prompts and
records executions without connecting to a real AI provider yet, while preserving
Browser → Platform API → Agent Runtime boundaries.

## Decision

- Add `agent_executions`, `execution_messages`, and `execution_metrics` via Flyway.
- Expose execute/list/get/cancel APIs only on Platform API.
- Extend `AgentRuntimeClient` with `execute` / `cancel` and ship `NoOpAgentRuntimeClient`
  as the default replaceable implementation.
- Resolve prompt content from the agent's published (or pinned superseded) prompt
  version using the existing `PromptVariableParser`.
- Authorize with `AGENT_EXECUTE`, `EXECUTION_READ`, and `EXECUTION_CANCEL`.
- Do not send provider secrets to the browser and do not call providers from the portal.

## Consequences

- Portal playground talks only to Platform API.
- Switching to real providers is an adapter swap behind `AgentRuntimeClient`.
- Conversation IDs are persisted for forward compatibility without memory yet.
