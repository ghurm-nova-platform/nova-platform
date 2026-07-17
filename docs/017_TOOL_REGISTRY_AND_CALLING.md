# Tool Registry and Agent Tool Calling

Sprint 1 Phase 7 — secure, provider-neutral tool registry with allowlisted internal executors.

## Boundary

```text
Browser → Platform API
             → Execution Service
             → AgentRuntimeClient
                 → RuntimeTurnResult (final | tool calls)
             → ToolCallingOrchestrator
                 → ToolAuthorization / InputValidator
                 → Allowlisted ToolExecutor
             → Runtime continuation
             → Final assistant response
```

The browser never executes tools, never calls Agent Runtime, and never marks approvals itself without Platform API.

## Tool definition vs executor

| Concept | Responsibility |
|---------|----------------|
| `tools` row | Tenant-scoped definition: schema, policy, status, executor **key** |
| `ToolExecutor` bean | Server-side allowlisted implementation registered at startup |
| `executor_key` | Must resolve to exactly one Spring `ToolExecutor` bean |

Java class names are never stored in the database. Unknown keys are rejected.

## Built-in executors

1. `CURRENT_DATETIME` — allowlisted IANA timezones only
2. `CALCULATOR` — ADD/SUBTRACT/MULTIPLY/DIVIDE via `BigDecimal` (no expressions)
3. `TEXT_STATISTICS` — deterministic character/word/line counts

No HTTP, shell, SQL, file system, scripting, or reflection-based invocation.

## Agent assignments

`agent_tool_assignments` links ACTIVE tools to agents. Runtime may only request tools that are assigned, enabled, and ACTIVE. Assignment is re-checked immediately before each execution.

## Orchestration lifecycle

1. Load assigned ACTIVE tool specs into the runtime request.
2. Process `RuntimeTurnResult`.
3. For each tool call (sequential in this phase): validate, authorize, persist, approve if required, execute with timeout, persist bounded output.
4. Return tool results to the runtime.
5. Stop on final response, approval wait, cancel, failure, or configured round/call limits.

## Approval

When `requiresApproval=true`, the tool call becomes `APPROVAL_REQUIRED` and the HTTP execute call returns without blocking. Authorized actors approve/reject and call `continue`.

## Idempotency

Unique `(execution_id, runtime_call_id)`. Duplicate calls reuse the stored result and never re-execute.

## Cancellation

Re-check execution status before and after each tool run. `CANCELLED` never becomes `COMPLETED` and never receives new TOOL/ASSISTANT conversation messages.

## Conversation TOOL messages

When `nova.conversation.store-tool-messages=true`, a concise TOOL message may be appended after successful tool completion, only if the conversation is still ACTIVE under lock.

## Privacy

Audit metadata and INFO/WARN logs must not contain tool input/output payloads, prompts, or raw exceptions.

## Known limitations

- No third-party integrations, OAuth, webhooks, or arbitrary HTTP tools
- No streaming / WebSocket tool events
- Sequential tool execution only
- No real LLM provider (NoOp runtime)

## Compatibility with knowledge retrieval

Tool calling and RAG coexist. Platform API retrieves knowledge once before the
initial runtime turn, then continues tool orchestration as documented here.
Retrieval does not re-run after every tool call in this phase.

## Migrations

- `V14__tool_registry.sql`
- `V15__tool_permissions_seed.sql`
