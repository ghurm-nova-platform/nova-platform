# ADR-0003: Project-scoped agent lifecycle on Platform API

## Status

Accepted

## Context

Sprint 1 Phase 3 requires managing agent definitions owned by projects while preserving
Browser → Platform API → Agent Runtime boundaries and JWT tenancy.

## Decision

- Store agent definitions in Platform API PostgreSQL with Flyway migrations.
- Scope every query by JWT `organizationId` and path `projectId`.
- Authorize with permission codes (`AGENT_READ`, `AGENT_CREATE`, `AGENT_UPDATE`,
  `AGENT_ACTIVATE`, `AGENT_ARCHIVE`) embedded in JWT from role-permission grants.
- Soft-archive via status `ARCHIVED` instead of hard delete for active agents.
- Use Hibernate `@Version` optimistic locking.
- Keep Agent Runtime sync behind `AgentRuntimeClient` with a no-op implementation until
  runtime sync is enabled; DB commits do not depend on runtime availability.
- Do not store provider secrets on agents.

## Consequences

- Portal must use project-scoped routes under `/projects/:projectId/agents`.
- Future execution workflows can reuse the same agent IDs through Platform API.
- Enabling runtime sync is a follow-up and must remain server-to-server only.
