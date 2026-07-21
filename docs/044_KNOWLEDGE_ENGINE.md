# Knowledge and Memory Engine

Sprint 6 Phase 2 — org-scoped structured knowledge documents with keyword search, document relations, import/export, access logging, and agent memory surfacing. This engine is separate from the provider-neutral RAG stack in [018_KNOWLEDGE_BASE_AND_RAG.md](018_KNOWLEDGE_BASE_AND_RAG.md); it does not use embeddings, vector stores, or semantic retrieval.

## Boundary

```text
Browser / Agents
  → Platform API
      → KnowledgeService (CRUD, lifecycle, relations)
      → KnowledgeSearchService (keyword + chunk text search)
      → KnowledgeMemoryService (relevant document surfacing)
      → KnowledgeImportExportService (markdown, JSON, PDF)
      → KnowledgeIndexService (fixed-size text chunks for search only)
      → Audit Center (AuditSource.KNOWLEDGE)
```

The browser never writes directly to storage, generates embeddings, or queries a vector database. Chunking exists solely to support substring search over large documents.

## Capabilities

1. **Structured documents** with title, summary, content, format, type, category, visibility, version, and tags
2. **Document lifecycle** — `DRAFT`, `ACTIVE`, `ARCHIVED`, `DELETED` (soft delete)
3. **Visibility** — `PRIVATE`, `PROJECT`, `ORGANIZATION`, `PUBLIC` with org-scoped enforcement
4. **Keyword search** over title, summary, content, tags, and chunk text with optional filters
5. **Memory surfacing** — returns recent active documents by knowledge type for agent context
6. **Document relations** to other documents or external refs (ADR, PR, release, deployment, project, decision)
7. **Import** — ADR, markdown, runbook, readme, and other formats with inferred metadata
8. **Export** — markdown, JSON, and PDF
9. **Access logging** — per-document read events
10. **Audit integration** — create, update, archive, delete, and access events via Audit Center

## Document lifecycle

| Status | Meaning |
|--------|---------|
| `DRAFT` | Reserved; new documents are created as `ACTIVE` in this phase |
| `ACTIVE` | Visible for read, search, and memory surfacing |
| `ARCHIVED` | Read-only; excluded from memory; cannot be modified |
| `DELETED` | Soft-deleted; excluded from list and search |

Archive and restore require `KNOWLEDGE_ADMIN`. Delete sets status to `DELETED` without removing rows.

## Knowledge types

`PROJECT`, `CODE`, `DOCUMENTATION`, `ADR`, `PULL_REQUEST`, `RELEASE`, `DEPLOYMENT`, `PIPELINE`, `TEST`, `BUG`, `FIX`, `DECISION`, `BEST_PRACTICE`, `RUNBOOK`, `API`

## Categories

`Architecture`, `Backend`, `Frontend`, `Database`, `Infrastructure`, `Security`, `Testing`, `Deployment`, `Operations`, `AI`, `General`

## Content formats

`MARKDOWN`, `PLAIN_TEXT`, `HTML`, `CODE`, `JSON`, `YAML`, `SQL`, `XML`

## Visibility rules

| Visibility | Read access |
|------------|-------------|
| `PRIVATE` | Author and `ORG_ADMIN` only |
| `PROJECT` | Org members when `projectId` is set |
| `ORGANIZATION` | All org members |
| `PUBLIC` | All org members (same as organization in this phase) |

Private documents can only be modified by the author, `ORG_ADMIN`, or users with `KNOWLEDGE_ADMIN`.

## API

| Method | Path | Permission |
|--------|------|------------|
| GET | `/api/knowledge/config` | `KNOWLEDGE_READ` |
| GET | `/api/knowledge` | `KNOWLEDGE_READ` |
| GET | `/api/knowledge/search` | `KNOWLEDGE_READ` |
| GET | `/api/knowledge/memory` | `KNOWLEDGE_READ` |
| GET | `/api/knowledge/categories` | `KNOWLEDGE_READ` |
| GET | `/api/knowledge/tags` | `KNOWLEDGE_READ` |
| GET | `/api/knowledge/project/{projectId}` | `KNOWLEDGE_READ` |
| GET | `/api/knowledge/{id}` | `KNOWLEDGE_READ` |
| GET | `/api/knowledge/{id}/export` | `KNOWLEDGE_READ` |
| GET | `/api/knowledge/{id}/relations` | `KNOWLEDGE_READ` |
| POST | `/api/knowledge` | `KNOWLEDGE_WRITE` |
| POST | `/api/knowledge/import` | `KNOWLEDGE_WRITE` |
| POST | `/api/knowledge/{id}/archive` | `KNOWLEDGE_ADMIN` |
| POST | `/api/knowledge/{id}/restore` | `KNOWLEDGE_ADMIN` |
| POST | `/api/knowledge/{id}/relate` | `KNOWLEDGE_WRITE` |
| PUT | `/api/knowledge/{id}` | `KNOWLEDGE_WRITE` |
| DELETE | `/api/knowledge/{id}` | `KNOWLEDGE_ADMIN` |

### Search query parameters

| Parameter | Description |
|-----------|-------------|
| `q` | Keyword matched against title, summary, content, and chunk text |
| `tag` | Filter by tag name |
| `category` | Filter by category |
| `projectId` | Filter by project |
| `authorId` | Filter by author |
| `visibility` | Filter by visibility |
| `knowledgeType` | Filter by knowledge type |
| `from` / `to` | ISO-8601 date range on `updatedAt` |

### Memory query parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `projectId` | — | Optional project scope |
| `types` | ADR, PR, BUG, FIX, DEPLOYMENT, DECISION, RUNBOOK | Knowledge types to include |
| `limit` | 20 | Max results (capped at 100) |

## Configuration

```yaml
nova:
  knowledge:
    engine:
      enabled: true
      cache:
        enabled: true
        ttl-seconds: 300
      chunk-size: 1000
      chunk-overlap: 100
```

| Property | Default | Description |
|----------|---------|-------------|
| `enabled` | `true` | Master switch; disabled returns `503 KNOWLEDGE_ENGINE_DISABLED` |
| `cache.enabled` | `true` | In-memory search result cache per org/user |
| `cache.ttl-seconds` | `300` | Search cache TTL |
| `chunk-size` | `1000` | Characters per search chunk |
| `chunk-overlap` | `100` | Overlap between consecutive chunks |

## Database

Flyway `V56__knowledge_engine.sql` creates:

- `knowledge_engine_documents`
- `knowledge_engine_chunks` (text offsets for keyword search; no embeddings)
- `knowledge_engine_tags`
- `knowledge_engine_document_tags`
- `knowledge_engine_relations`
- `knowledge_engine_attachments`
- `knowledge_engine_access_logs`

Permissions added: `KNOWLEDGE_WRITE`, `KNOWLEDGE_ADMIN`. `KNOWLEDGE_READ` is reused from the RAG permission seed (V18).

Audit constraints extended for `AuditSource.KNOWLEDGE` and `AuditEntityType.KNOWLEDGE`.

## Search and indexing

- `KnowledgeIndexService` splits document content into fixed-size overlapping chunks on create/update
- `KnowledgeSearchService` performs case-insensitive substring matching (not semantic similarity)
- Results are filtered by visibility and cached in-process with configurable TTL
- Archived and deleted documents are excluded from search and memory

## Import and export

**Import** (`POST /api/knowledge/import`): accepts content with optional `importFormat` (`adr`, `markdown`, `runbook`, `readme`, `json`, etc.) and infers `contentFormat` and `knowledgeType` when omitted.

**Export** (`GET /api/knowledge/{id}/export?format=`): supports `markdown` (default), `json`, and `pdf`.

## Audit

Document lifecycle events publish to Audit Center with `AuditSource.KNOWLEDGE` and `AuditEntityType.KNOWLEDGE`. Access events are recorded on document read.

## Constraints

- No embeddings, vector stores, or semantic/RAG retrieval in this engine
- No agent assignment or grounding context injection (use RAG stack in 018)
- No file upload pipeline; content is submitted as text via API
- No WebSockets, Kafka, RabbitMQ, or Redis
- Search cache is in-process only; no cross-instance invalidation
- Attachments table exists for future use; no upload API in this phase
- Portal UI deferred; API-only in Sprint 6 Phase 2

## Relationship to RAG

| Concern | Knowledge Engine (044) | Knowledge Bases / RAG (018) |
|---------|------------------------|----------------------------|
| Purpose | Structured project memory, ADRs, runbooks, relations | Agent grounding via semantic retrieval |
| Storage | `knowledge_engine_*` tables | `knowledge_bases`, `knowledge_embeddings`, etc. |
| Search | Keyword + chunk text | Embedding similarity |
| Agent integration | Memory surfacing endpoint | Pre-turn retrieval snapshot |

See [ADR-0033](adr/ADR-0033-knowledge-engine.md).
