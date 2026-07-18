# ADR-0008: Provider-Neutral RAG

## Status

Accepted (Sprint 1 Phase 8)

## Context

Nova needs Retrieval-Augmented Generation for agents, but binding Platform API to a single cloud embedding vendor or vector database would lock the architecture and increase risk. The browser must never talk to embedding or vector providers. Uploaded documents must not be executed or used to construct filesystem paths.

## Decision

1. Own knowledge-base, document, chunk, embedding, assignment, and retrieval-audit data in Platform API (Flyway V16–V18).
2. Use Spring-managed allowlisted `EmbeddingProvider` beans resolved by `provider_key` (never class names from the database).
3. Use a `KnowledgeVectorStore` interface with a database-backed cosine-similarity implementation for this phase.
4. Ship `DeterministicLocalEmbeddingProvider` for tests and local development only.
5. Perform retrieval once before the initial Agent Runtime turn; pass bounded `RuntimeKnowledgeContext` into `AgentRuntimeClient`.
6. Keep audit privacy-safe: hashes and counts only; never store query text, chunk content, or embeddings in audit.
7. Soft-archive knowledge bases and documents; exclude archived/non-READY content from retrieval.
8. Defer PDF extraction (`pdf-enabled=false`) until a safe pure-Java path is proven.

## Consequences

- Strong tenant isolation and a clear upgrade path to production embeddings/vector DBs.
- Application-side similarity search is bounded and not production-scale.
- Deterministic local embeddings are not semantic quality for production RAG.
- Tool calling (ADR-0007) and knowledge retrieval coexist: retrieval does not re-run after every tool call.
