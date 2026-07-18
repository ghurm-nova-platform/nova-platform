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
- Artifact storage remains latest-only per task (V35); multi-generation history is an intentional later evolution, not a missing V35 requirement.

## Future Evolution

Keep the current V35 design exactly as-is. Coding Agent is intentionally **stateless at the artifact level**: each task retains only its latest generation so storage, validation, retrieval, and orchestration stay simple and deterministic.

When Review Agent, retry policies, and Git integration are introduced (Sprint 3+), evolve the model by adding `generation_id` (UUID) so one generate call’s files form a group.

Expected schema evolution:

- Add `generation_id UUID` to `generated_artifacts`
- Index `(task_id, generation_id)`
- Replace `uq_generated_artifacts_task_path` with `uq_generated_artifacts_generation_path` (path unique within a generation)

Goals: preserve and compare generations, AI review across generations, choose the best generation before patches, retry history, and human approval workflows.

Sprint backlog item: **Artifact Versioning / Multi-Generation Storage** (see `docs/025_SPRINT_3_BACKLOG.md`). No migration, API, or code changes in this phase.
