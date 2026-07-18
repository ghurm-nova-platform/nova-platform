# ADR-0014: Coding Agent generates artifacts only

- Status: Accepted
- Date: 2026-07-18

## Context

Nova needs a first production execution agent that can turn an orchestration coding task into source files. Repository mutation (commit, push, merge) is intentionally deferred to later phases. The agent must not gain shell, git, docker, browser, MCP, or filesystem write capabilities through this phase.

## Decision

1. Implement Coding Agent as a Platform API module that calls the existing Agent Runtime / Model Gateway for structured JSON artifacts only.
2. Persist `generated_artifacts` (V35) with SHA-256 digests and RBAC (`CODING_GENERATE`, `CODING_READ`).
3. Validate language, artifact type, relative paths, emptiness, duplicates, and binary content with stable `CODING_*` codes.
4. Keep external model invocation outside database write transactions; store artifacts through `ArtifactStorageService`.
5. Do not commit, push, merge, execute shell, or write the local filesystem from this agent.

## Consequences

- Operators can preview, search, copy, and download generated artifacts in the portal.
- Future Git commit / PR phases can consume stored artifacts without changing the generation boundary.
- Invalid model output fails closed with stable API error codes.
- Security surface stays limited to durable artifact storage, not repository mutation.
