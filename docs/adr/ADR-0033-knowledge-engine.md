# ADR-0033: Knowledge and Memory Engine

## Status

Accepted

## Context

Nova Platform already has a provider-neutral RAG stack (ADR-0008) for agent grounding: knowledge bases, document ingestion, embeddings, vector similarity search, and retrieval snapshots injected before Agent Runtime turns. Sprint 6 Phase 1 delivered multi-agent collaboration (ADR-0032), which explicitly deferred knowledge and memory.

Operators and agents also need durable, structured project knowledge — ADRs, runbooks, decisions, bug/fix records, and cross-document relations — without coupling that data to embedding providers or vector infrastructure. Requirements:

- Org-scoped structured documents with types, categories, tags, and visibility
- Keyword search and memory surfacing for agents
- Import/export (markdown, JSON, PDF)
- Document relations to ADRs, PRs, releases, deployments, and external refs
- Audit integration via existing Audit Center
- Reuse existing `KNOWLEDGE_READ` permission; add write/admin permissions
- No embeddings, vector stores, semantic search, or RAG retrieval in this phase

## Decision

Implement `ai.nova.platform.knowledge.engine` on Platform API as a **parallel engine** alongside the existing RAG stack (`ai.nova.platform.knowledge`):

1. **Separate schema** — Flyway V56 creates `knowledge_engine_*` tables; no changes to `knowledge_bases`, `knowledge_embeddings`, or agent assignment tables
2. **KnowledgeService** — document CRUD, lifecycle (archive/restore/soft-delete), tags, relations, access logging
3. **KnowledgeIndexService** — fixed-size text chunks for keyword search only (no embeddings)
4. **KnowledgeSearchService** — substring matching with in-memory TTL cache
5. **KnowledgeMemoryService** — surfaces recent active documents by knowledge type for agent context
6. **KnowledgeImportExportService** — import with format inference; export to markdown, JSON, PDF
7. **KnowledgeVisibilityService** — org-scoped visibility enforcement
8. **KnowledgeEngineAuthorizationService** — `KNOWLEDGE_READ` (existing), `KNOWLEDGE_WRITE`, `KNOWLEDGE_ADMIN`
9. **KnowledgeController** — REST API under `/api/knowledge`
10. **Audit** — `AuditSource.KNOWLEDGE`, `AuditEntityType.KNOWLEDGE`

The RAG stack remains unchanged and continues to serve agent grounding. The Knowledge Engine serves structured memory, documentation, and keyword discovery.

## Consequences

### Positive

- Structured project knowledge without embedding provider dependency
- Clear separation of concerns: memory/docs vs. semantic retrieval
- Reuses existing read permission; minimal permission surface
- Full audit trail for document lifecycle
- Upgrade path: future phases can bridge engines or add optional semantic indexing without replacing either stack

### Negative

- Two knowledge subsystems coexist; operators must understand which to use
- Keyword search does not match semantic relevance of RAG retrieval
- In-process search cache does not invalidate across Platform API instances
- No portal UI in this phase; API-only
- Attachments schema exists but upload API is deferred

### Alternatives considered

- **Extend RAG knowledge bases** — rejected; couples structured memory to embeddings and agent assignment model
- **Replace RAG with keyword search** — rejected; agents need semantic retrieval for grounding
- **Single unified knowledge table** — rejected; different lifecycles, search models, and agent integration paths
- **Vector search in Knowledge Engine** — rejected (explicit constraint for this phase)

## References

- [Knowledge and Memory Engine](../044_KNOWLEDGE_ENGINE.md)
- [Knowledge Bases and RAG](../018_KNOWLEDGE_BASE_AND_RAG.md)
- [ADR-0008: Provider-Neutral RAG](ADR-0008-provider-neutral-rag.md)
- [ADR-0032: Multi-Agent Collaboration](ADR-0032-multi-agent-collaboration.md)
