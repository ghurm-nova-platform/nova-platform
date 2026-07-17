# Conversation Memory and Message History

Sprint 1 Phase 6 — durable project-scoped conversations with bounded context assembly for agent execution.

## Boundary

```text
Browser → Platform API
             → Conversation Service
             → Execution Service
             → AgentRuntimeClient
                 → NoOpAgentRuntimeClient
```

The browser never calls Agent Runtime. Conversation history is assembled only by Platform API.

## Conversation vs execution

| Store | Purpose |
|-------|---------|
| `conversation_messages` | Durable multi-turn history for a conversation |
| `execution_messages` | Immutable per-execution snapshot (system + user + assistant for that run) |

By default, system prompts are **not** duplicated into conversation history
(`nova.conversation.store-system-message=false`).

## APIs

Base: `/api/projects/{projectId}/conversations`

| Method | Path | Permission |
|--------|------|------------|
| `POST` | `/` | `CONVERSATION_CREATE` |
| `GET` | `/` | `CONVERSATION_READ` |
| `GET` | `/{id}` | `CONVERSATION_READ` |
| `PUT` | `/{id}` | `CONVERSATION_UPDATE` |
| `DELETE` | `/{id}` | `CONVERSATION_ARCHIVE` (soft archive) |
| `POST` | `/{id}/restore` | `CONVERSATION_ARCHIVE` |
| `GET` | `/{id}/messages` | `CONVERSATION_MESSAGE_READ` |
| `POST` | `/{id}/messages` | `CONVERSATION_MESSAGE_CREATE` |

Execute integration:

`POST /api/projects/{projectId}/agents/{agentId}/execute`

```json
{
  "input": { "message": "Hello" },
  "variables": {},
  "conversationId": "uuid-or-null",
  "clientRequestId": "uuid-required-when-conversationId-set"
}
```

## Memory selection algorithm

Config (`nova.conversation`):

- `max-context-messages` (default 20)
- `max-context-characters` (default 30000)
- `max-message-characters` (default 10000)

Algorithm:

1. Load recent conversation messages descending by sequence.
2. Select until message or character limit is reached (never split a message).
3. Always include the current user message.
4. Return ascending sequence for the runtime.
5. Emit `MEMORY_TRUNCATED` audit with safe metadata only (counts, no content)
   via a separate write transaction (`REQUIRES_NEW`), so assembly can stay
   `@Transactional(readOnly = true)`.

## Execution integration

- `conversationId` null → existing stateless execution.
- `conversationId` set → validate ACTIVE conversation for same org/project/agent,
  assemble memory, then in one short write transaction:
  reserve `clientRequestId`, create RUNNING execution, append USER message.
  Duplicates return the reserved execution and never create a second execution row.
  Run runtime afterward; append ASSISTANT only on `COMPLETED` and only while the
  conversation is still ACTIVE under lock (archived conversations skip ASSISTANT).
- `clientRequestId` provides idempotency per conversation via
  `conversation_execution_requests`.

## Cancellation

PR #38 lifecycle is preserved:

- RUNNING committed before runtime.
- Completion/failure will not overwrite `CANCELLED`.
- Cancelled runs do not add ASSISTANT conversation messages.
- Successful tool results may append TOOL conversation messages when
  `nova.conversation.store-tool-messages=true` (see tool calling docs).
- Retrieved knowledge chunks are never appended as conversation messages.
  Citations may appear in the assistant response and execute metadata
  (see [`018_KNOWLEDGE_BASE_AND_RAG.md`](018_KNOWLEDGE_BASE_AND_RAG.md)).

## RBAC

| Permission | ORG_ADMIN | PROJECT_ADMIN | USER / ORG_MEMBER |
|------------|-----------|---------------|-------------------|
| `CONVERSATION_READ` | ✓ | ✓ | ✓ |
| `CONVERSATION_CREATE` | ✓ | ✓ | ✓ |
| `CONVERSATION_UPDATE` | ✓ | ✓ | |
| `CONVERSATION_ARCHIVE` | ✓ | ✓ | |
| `CONVERSATION_MESSAGE_READ` | ✓ | ✓ | ✓ |
| `CONVERSATION_MESSAGE_CREATE` | ✓ | ✓ | ✓ |

## Privacy

- No raw message content in audit metadata or INFO/WARN logs.
- Tenant/project/agent isolation on every query.
- Cross-tenant access returns 404.
- Content length limits enforced server-side.

## Portal

- `/projects/:projectId/conversations`
- `/projects/:projectId/conversations/new`
- `/projects/:projectId/conversations/:conversationId`
- `/projects/:projectId/agents/:agentId/conversations`
- Agent playground: Stateless vs Conversation modes

## Known limitations

- No embeddings / vector memory / RAG
- No AI summarization
- No WebSocket streaming of conversation events
- Transcript retention/redaction policy still a follow-up for real providers

## Follow-up

- Semantic / long-term memory
- Optional per-project disable of content storage
- Retention and redaction policies
- Streaming conversation updates
