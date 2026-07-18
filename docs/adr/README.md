# Architecture Decision Records

Architecture Decision Records (ADRs) capture decisions that materially affect Nova Platform.

## Rules

1. Copy `ADR_TEMPLATE.md`.
2. Use the next four-digit sequence number.
3. Open a pull request before implementation depends on the decision.
4. Never rewrite accepted history. Supersede an ADR with a new ADR.
5. Record security, operational, cost, and migration consequences.

## Index

| ADR | Title | Status |
|-----|-------|--------|
| [ADR-0001](ADR-0001-modular-monorepo.md) | Modular monorepo | Accepted |
| [ADR-0002](ADR-0002-jwt-auth-foundation.md) | JWT authentication foundation on Platform API | Accepted |
| [ADR-0003](ADR-0003-agent-lifecycle.md) | Project-scoped agent lifecycle on Platform API | Accepted |
| [ADR-0004](ADR-0004-prompt-versioning.md) | Immutable published prompt versions | Accepted |
| [ADR-0005](ADR-0005-agent-execution-noop.md) | NoOp-first agent execution behind Platform API | Accepted |
| [ADR-0006](ADR-0006-conversation-memory.md) | Bounded conversation memory on Platform API | Accepted |
| [ADR-0007](ADR-0007-secure-tool-calling.md) | Secure allowlisted tool calling on Platform API | Accepted |
| [ADR-0008](ADR-0008-provider-neutral-rag.md) | Provider-neutral knowledge bases and RAG on Platform API | Accepted |
| [ADR-0009](ADR-0009-provider-neutral-model-gateway.md) | Provider-neutral AI model gateway and routing on Platform API | Accepted |
| [ADR-0010](ADR-0010-provider-secret-vault.md) | Provider secret vault and production OpenAI / Azure adapters | Accepted |
