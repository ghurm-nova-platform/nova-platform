# Agent Execution Engine

Sprint 1 Phase 5 — first execution layer for Nova Platform.

## Boundary

```text
Browser → Platform API (/api/projects/{projectId}/agents/{agentId}/execute)
             → AgentRuntimeClient.execute(...)
                 → NoOpAgentRuntimeClient (this phase)
```

The browser never calls Agent Runtime and never receives provider secrets.
No real AI provider integration is included in this phase.

## Execution flow

```text
Load Agent
  → Verify ACTIVE
  → Resolve published (or agent-pinned SUPERSEDED) prompt version
  → Load variables
  → Substitute {{variables}} via PromptVariableParser
  → Persist execution + SYSTEM/USER messages
  → Call AgentRuntimeClient.execute
  → Persist ASSISTANT message, tokens, latency, metrics
```

Rules:

- Never execute a `DRAFT` prompt version.
- Reject archived prompts.
- Reject missing required variables (`MISSING_VARIABLE`).
- `conversationId` is accepted and stored. When provided with `clientRequestId`,
  Platform API assembles bounded conversation context (see
  [`016_CONVERSATION_MEMORY.md`](016_CONVERSATION_MEMORY.md)).
- Stateless execution (`conversationId` null) remains supported.
- Assigned ACTIVE tools are orchestrated by Platform API
  ([`017_TOOL_REGISTRY_AND_CALLING.md`](017_TOOL_REGISTRY_AND_CALLING.md)).

## Endpoints

| Method | Path | Permission |
|--------|------|------------|
| `POST` | `/api/projects/{projectId}/agents/{agentId}/execute` | `AGENT_EXECUTE` |
| `GET` | `/api/projects/{projectId}/executions` | `EXECUTION_READ` |
| `GET` | `/api/projects/{projectId}/executions/{id}` | `EXECUTION_READ` |
| `POST` | `/api/projects/{projectId}/executions/{id}/cancel` | `EXECUTION_CANCEL` |

List query params: `agentId`, `status`, `page`, `size`, `sort`.

### Execute request

```json
{
  "input": { "message": "Help with billing" },
  "variables": { "customer_name": "Alex", "topic": "billing" },
  "conversationId": null
}
```

### Execute response

```json
{
  "executionId": "...",
  "status": "COMPLETED",
  "response": "...",
  "latencyMs": 120,
  "tokens": { "input": 10, "output": 25, "total": 35 },
  "renderedPrompt": "..."
}
```

## Statuses

`PENDING` → `RUNNING` → `COMPLETED` | `FAILED` | `CANCELLED`

## RBAC

| Permission | ORG_ADMIN | PROJECT_ADMIN | USER / ORG_MEMBER |
|------------|-----------|---------------|-------------------|
| `AGENT_EXECUTE` | ✓ | ✓ | ✓ |
| `EXECUTION_READ` | ✓ | ✓ | ✓ |
| `EXECUTION_CANCEL` | ✓ | ✓ | |

## Runtime

`NoOpAgentRuntimeClient`:

- Deterministic fake assistant text
- Artificial latency 100–300 ms
- Fake token counts derived from word counts
- No outbound HTTP

Replaceable later with OpenAI / Azure / Anthropic / Gemini / Ollama adapters behind the same interface.

## Portal

Project-scoped playground:

`/projects/:projectId/agents/:agentId/playground`

Features: input, variables, run, status, latency, tokens, prompt preview, response viewer, execution history, cancel.

## Error codes

| Code | Typical HTTP |
|------|--------------|
| `AGENT_NOT_ACTIVE` | 409 |
| `PROMPT_NOT_PUBLISHED` | 409 |
| `MISSING_VARIABLE` | 400 |
| `INVALID_INPUT` | 400 |
| `EXECUTION_FAILED` | 500/409 |
| `EXECUTION_CANCELLED` | 409 |
| `EXECUTION_NOT_FOUND` | 404 |
| `AGENT_NOT_FOUND` | 404 |
| `PROJECT_NOT_FOUND` | 404 |
| `FORBIDDEN` | 403 |

## Known limitations

- No real LLM providers
- No conversation memory / multi-turn context assembly
- No streaming responses
- Cancel is cooperative: RUNNING is committed before the runtime call; completion
  will not overwrite `CANCELLED`
- Execution messages currently store full rendered system prompt and user text.
  Before connecting a real provider, define retention, deletion, redaction,
  who may read transcripts, and a per-project option to disable content storage.

## Error persistence

On runtime failure, Platform API stores only:

- `error_message` = `Execution failed` (safe user-facing text)
- metric `error_code` = `EXECUTION_FAILED`
- metric `correlation_id` when present

Raw provider/runtime exception text is never persisted or returned.

## Follow-up

- Real provider adapters (server-side secrets only)
- Streaming timeline over Platform API
- Conversation memory store
- Cost / usage aggregation from `execution_metrics`
- Execution transcript retention / redaction / project-level content storage toggle
