# ADR-0004: Immutable published prompt versions

## Status

Accepted

## Context

Sprint 1 Phase 4 requires reusable, project-scoped prompts with versioning,
variables, audit, and RBAC while preserving Browser → Platform API boundaries
and avoiding real LLM execution in this phase.

## Decision

- Store prompts, versions, variables, tags, and audit logs in Platform API
  PostgreSQL via Flyway (`V7`–`V9`).
- Scope every query by JWT `organizationId` and path `projectId`.
- Authorize with permission codes (`PROMPT_READ`, `PROMPT_CREATE`, `PROMPT_UPDATE`,
  `PROMPT_PUBLISH`, `PROMPT_ARCHIVE`, `PROMPT_COMPARE`, `PROMPT_PREVIEW`).
- Treat published versions as immutable; edits create new draft versions.
- Implement rollback as copy-forward into a new draft (no history rewrite).
- Support `{{variable}}` parsing and deterministic preview substitution only.
- Allow agents optional `prompt_id` / `prompt_version_id` references to published
  versions; keep `system_prompt` for backward compatibility until a later phase.
- Soft-archive prompts instead of hard delete.

## Consequences

- Portal uses project-scoped routes under `/projects/:projectId/prompts`.
- Future LLM execution must remain Platform API → Agent Runtime only.
- Monaco Editor and provider execution are explicit follow-ups.
- Agent `systemPrompt` deprecation requires a coordinated migration later.
