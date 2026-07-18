# AI Model Gateway and Routing Policies

Sprint 1 Phase 9 — secure, provider-neutral AI model gateway owned by Platform API.

## Boundary

```text
Browser
  → Platform API
      → Execution Service
      → Model Routing Service
      → AiModelGateway
      → Allowlisted AiModelProvider
      → Provider-neutral RuntimeTurnResult
      → Tool Calling / RAG / Final Response
```

The browser never calls providers, never receives credentials, and never submits trusted routing or usage data.

## Ownership

| Concern | Owner |
|---------|--------|
| Knowledge retrieval + snapshot (V19) | Knowledge services / ToolCallingOrchestrator |
| Model routing | ModelRoutingService |
| Provider invocation | AiModelGateway |
| Tool orchestration | ToolCallingOrchestrator |
| Execution lifecycle | ExecutionLifecycleService / ExecutionService |
| AgentRuntimeClient | NoOp (default) or gateway-backed mapping |

## Provider lifecycle

`DRAFT` → `ACTIVE` → `DISABLED` / `ARCHIVED`.

Activation requires a registered adapter matching `provider_type`. Reserved types (OPENAI, AZURE_OPENAI, ANTHROPIC, GOOGLE_GEMINI, AWS_BEDROCK, CUSTOM_MANAGED) may stay DRAFT until an adapter exists. Soft archive preserves history.

## Model lifecycle

`DRAFT` → `ACTIVE` → `DISABLED` / `ARCHIVED`.

Only `CHAT` and `TEXT_GENERATION` are executable in this phase. Provider must be ACTIVE before model activation. Cost metadata is administrative and never trusted from execution clients.

## Provider adapter allowlist

Spring-managed `AiModelProvider` beans registered by `adapter_key`. Duplicate keys fail startup. Database values resolve to beans — no reflection or dynamic class loading.

## Credential references

`credential_reference` stores references only (e.g. `env:NOVA_PROVIDER_OPENAI`). Plaintext API keys, bearer tokens, private keys, and JSON secrets are rejected. Resolved secrets never appear in REST, logs, or persistence. Deterministic local providers require `NULL`.

## Deterministic local provider

| Field | Value |
|-------|-------|
| Adapter | `DETERMINISTIC_LOCAL` |
| Model | `deterministic-chat-v1` |
| Network | None |
| Credentials | None |

Suitable for tests and local development only — not production semantic quality. Supports final responses, tool-call markers, knowledge-context acknowledgment, and safe failure/timeout simulation in tests.

## Routing strategies

| Strategy | Behavior |
|----------|----------|
| `FIXED_PRIMARY` | Enabled PRIMARY only (fallback only if policy allows and configured) |
| `PRIORITY_FALLBACK` | PRIMARY then FALLBACK by priority ASC, model ID ASC |
| `CAPABILITY_BASED` | Filter by required capabilities, then same ordering |

Agent-scoped ACTIVE policies override project policies. No scripts or expression engines.

## Retry and fallback

Retry only transient codes: timeout, unavailable, rate limited, temporary error. Permanent failures are not retried. Respect attempt and duration caps. Distinct `model_invocations` per attempt with `fallback_from_invocation_id` links. Recheck cancellation before every attempt.

## Concurrency

`ProviderConcurrencyManager` enforces per-provider process-local capacity (semaphores). Not distributed across instances. Permits released on success, failure, timeout, and cancellation.

## Transaction boundaries

1. Short TX: validate, create invocation RUNNING, commit.
2. Provider call **outside** any DB transaction.
3. Short TX: re-lock, recheck cancel, persist result, update usage, commit.

## Cancellation

Cancel before/during invocation prevents final assistant append and keeps execution `CANCELLED`. Provider completion after cancel does not overwrite status. Conversation archive during invocation skips assistant append.

## RAG and tools

Retrieval runs once before the initial turn; V19 knowledge snapshots restore context after tool approval. Tool results return through the same gateway path. Tool calling and RAG coexist.

## Usage and cost

Server-calculated daily aggregates. Estimated tokens/costs clearly labeled; no billing. Browser cannot set usage. Daily/monthly request limits enforced when configured.

## Migrations

- `V20__ai_model_gateway.sql`
- `V21__model_routing_and_usage.sql`
- `V22__model_gateway_permissions.sql`

V1–V19 unchanged (including knowledge + execution knowledge snapshot).

## Security

Tenant isolation, RBAC, no arbitrary URLs, no secrets in Angular, no prompt/completion/tool/RAG content in invocation records or INFO/WARN logs.

## Future work

Production provider adapters, secret vaults, streaming, distributed concurrency, billing reconciliation.
