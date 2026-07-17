# ADR-0006: Bounded conversation memory on Platform API

## Status

Accepted

## Context

Agent execution already accepts optional `conversationId` but does not assemble
prior turns. Sprint 1 Phase 6 needs durable conversation history and bounded
context for NoOp (and future) runtimes without leaving the Platform API boundary.

## Decision

- Store conversations and messages in Platform API PostgreSQL (Flyway V12+).
- Assemble context in `ConversationMemoryService` with configurable message and
  character limits (no AI summarization).
- Pass provider-neutral `List<RuntimeMessage>` through `AgentRuntimeClient`.
- Keep `execution_messages` as immutable per-run snapshots distinct from durable
  `conversation_messages`.
- Require `clientRequestId` for conversation-scoped executes for idempotency.
- Preserve split-transaction cancel semantics from ADR-0005 / PR #38.
- Authorize with `CONVERSATION_*` and `CONVERSATION_MESSAGE_*` permissions.

## Consequences

- Portal conversation UI and playground talk only to Platform API.
- Future semantic memory can plug in behind the same conversation APIs.
- Operators must define retention/redaction before enabling real providers.
