# Knowledge Bases and Retrieval-Augmented Generation

Sprint 1 Phase 8 — secure, provider-neutral knowledge bases with Platform API–owned ingestion, embedding, vector storage, and retrieval.

## Boundary

```text
Browser
  → Platform API
      → Knowledge Base Service
      → Document Validation
      → Text Extraction
      → Chunking
      → EmbeddingProvider (allowlisted)
      → VectorStore (database-backed in this phase)
      → RetrievalService
      → ExecutionService / AgentRuntimeClient (grounding context)
```

The browser never generates embeddings, queries the vector store, submits trusted chunk IDs, injects retrieved context, calls Agent Runtime, or reads internal storage paths.

## Knowledge-base lifecycle

| Status | Meaning |
|--------|---------|
| `DRAFT` | Configurable; not used for retrieval |
| `ACTIVE` | Eligible for agent assignment and retrieval |
| `ARCHIVED` | Soft-deleted; assignments disabled; excluded from retrieval |

Activation requires a server-allowlisted embedding provider and valid chunk/retrieval configuration. Archived knowledge bases cannot reactivate in this phase.

## Document lifecycle

| Status | Meaning |
|--------|---------|
| `UPLOADED` | Accepted upload, not yet processed |
| `PROCESSING` | Extraction / chunking / embedding in progress |
| `READY` | Chunks and embeddings available for retrieval |
| `FAILED` | Safe ingestion error code stored; never left in `PROCESSING` after handled failure |
| `ARCHIVED` | Soft-deleted; excluded from retrieval; records preserved |

Processing in this phase is synchronous and bounded. Reprocess deletes and recreates chunks/embeddings transactionally where possible and is idempotent.

## Supported document types

| Type | MIME | Notes |
|------|------|-------|
| TEXT | `text/plain` | Required |
| MARKDOWN | `text/markdown`, `text/x-markdown` | Required |
| PDF | `application/pdf` | Deferred (`nova.knowledge.pdf-enabled=false`); rejected until enabled |

Rejected: HTML, XML, Office, images, audio/video, executables, archives, unknown MIME, MIME/extension mismatches, empty documents.

Maximum extracted text: 1,000,000 characters (configurable).

## Extraction restrictions

- Normalize line endings; strip null characters; reject binary-looking text.
- No macros, scripts, link fetching, includes, or embedded resource execution.
- Never use original file names as filesystem paths.
- Never expose internal storage paths.
- Never log extracted text at INFO/WARN.

## Chunking strategy

`ParagraphAwareTextChunker` is deterministic:

1. Prefer paragraph boundaries.
2. Fall back to sentence, then character boundaries.
3. Respect `chunkSize` and `chunkOverlap`.
4. Stable chunk indexes and content hashes.
5. No empty or duplicate trailing chunks.
6. Enforce `nova.knowledge.max-chunks-per-document`.

## Embedding-provider abstraction

`EmbeddingProvider` is Spring-managed and allowlisted via `EmbeddingProviderRegistry`.

- Database stores `provider_key` + `model`, never Java class names.
- Duplicate provider keys fail startup.
- No reflection-based or arbitrary HTTP providers in this phase.

### DeterministicLocalEmbeddingProvider

| Field | Value |
|-------|-------|
| Key | `DETERMINISTIC_LOCAL` |
| Model | `deterministic-v1` |
| Dimensions | 64 |

Suitable for tests and local development only. Not production semantic quality. Same input → same finite normalized vector.

## Vector-store abstraction

`KnowledgeVectorStore` with `DatabaseKnowledgeVectorStore` for this phase:

- Tenant + project + knowledge-base scoped candidates only.
- Cosine similarity in application code.
- Exclude archived knowledge bases, archived/non-READY documents.
- Bound candidates (`nova.knowledge.max-vector-candidates`).
- Stable sort: score descending, then chunk ID ascending.
- Raw embedding vectors are never returned by normal REST APIs.

## Retrieval lifecycle

1. Load enabled agent knowledge assignments.
2. Keep only `ACTIVE` knowledge bases.
3. Embed the query with each assigned provider/model.
4. Search assigned stores with effective topK / minimum score.
5. Deduplicate identical chunk hashes.
6. Enforce maximum retrieved characters.
7. Build citation labels `[K1]`, `[K2]`, …
8. Persist privacy-safe retrieval audit (best-effort; failure does not fail retrieval).
9. Return structured chunks to execution (not as conversation messages).

Retrieval runs once before the initial runtime turn. It does not re-run after every tool call.

## Agent assignments

`agent_knowledge_assignments` links agents to knowledge bases with optional `topK` / `minimumScore` overrides. Agent and knowledge base must share organization and project. Archived agents cannot receive assignments. Duplicate assign is idempotent. Soft disable on unassign.

## Runtime grounding contract

`ExecutionRequest` may include `RuntimeKnowledgeContext`:

- Citation metadata (label, names, indexes, scores)
- Bounded chunk content for grounding

Runtime cannot choose knowledge bases or request arbitrary document IDs. Only server-authorized chunks are sent. `NoOpAgentRuntimeClient` accepts optional knowledge context and exposes deterministic markers for tests.

## Citations

Execute responses include citation metadata separately (no chunk content unless a dedicated document-read endpoint with permission). Assistant text may reference `[K1]`-style labels. Retrieved chunks are never appended as conversation messages.

## Execution snapshot policy

May store: retrieval configuration, citation identifiers, bounded chunk content for reproducibility, scores, knowledge-base/document IDs.

Persisted in `execution_knowledge_snapshots` (Flyway V19) so tool-approval continuation restores the same bounded `RuntimeKnowledgeContext` without re-retrieval. Snapshots never include embeddings.

Must not store: embeddings, provider secrets, internal paths, unbounded document content.

## Conversation policy

- Do not append retrieved chunks as messages.
- Future turns retrieve again from active sources.
- Archived conversations must not receive assistant messages.
- Cancellation prevents final assistant append.

## Idempotency

- Document upload: `(knowledgeBaseId, contentHash)` for non-archived documents; concurrent duplicates must not create duplicate READY docs/chunks.
- Reprocess: no duplicate chunks/embeddings; failed rebuild must not expose partial replacements.
- Execution: existing `clientRequestId` guarantees unchanged; retrieval must not duplicate USER messages.

## Concurrency and cancellation

| Scenario | Behavior |
|----------|----------|
| Cancel before retrieval | Runtime not called |
| Cancel during retrieval | No final assistant append |
| Archive conversation during retrieval | No assistant append |
| Disable assignment / archive KB or document before retrieval | Excluded at recheck |
| Reprocess during retrieval | Old complete or new complete version only |
| Concurrent duplicate uploads | One canonical document; no duplicate chunks |
| Completion after cancel | Status stays `CANCELLED` |

## Tenant isolation and RBAC

Every repository query is organization- and project-scoped. Cross-tenant access returns 404. Backend authorization is authoritative.

| Permission | Typical grants |
|------------|----------------|
| `KNOWLEDGE_READ`, `KNOWLEDGE_DOCUMENT_READ`, `KNOWLEDGE_RETRIEVE` | USER / ORG_MEMBER |
| Create, update, activate, archive, upload, assign, reprocess, audit | ORG_ADMIN / PROJECT_ADMIN |

## Audit privacy

`knowledge_retrieval_audit` stores query hash, character counts, topK, candidate/returned counts, minimum score, duration, identifiers, and correlation ID.

Never store: raw query, retrieved chunk content, embeddings.

Document/chunk/query content must not appear in INFO/WARN logs.

## Content retention

Soft archive preserves documents, chunks, embeddings, and retrieval history. Physical delete is out of scope for this phase.

## Security restrictions

- No arbitrary URL crawling or fetching.
- No execution of uploaded files.
- No archive extraction.
- No secrets in Angular environments.
- MIME + content inspection; SHA-256 content hashing.
- Size, text, chunk, query, candidate, and retrieval character limits.

## Future work

- Production embedding providers (allowlisted)
- External vector databases
- Document connectors (Drive, SharePoint, etc.) — not in Sprint 1
- PDF text extraction when safely enabled
- Streaming ingestion, OCR, knowledge graphs

## Compatibility with model gateway

Retrieved context is passed through the AI Model Gateway request contract
([`019_AI_MODEL_GATEWAY.md`](019_AI_MODEL_GATEWAY.md)). Execution knowledge
snapshots (V19) survive tool-approval continuation independently of which
provider adapter executes the turn.
