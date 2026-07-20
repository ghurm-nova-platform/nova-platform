# ADR-0028: Environment Management extends the global catalog without deployment

- Status: Accepted
- Date: 2026-07-20

## Context

Deployment Observation (V46) seeded a global `deployment_environments` catalog referenced by `deployment_operations` and `rollback_operations`. Organizations also need project-scoped environment metadata (labels, variable definitions, ownership, status) without breaking existing FKs or deployment observation queries.

## Decision

1. Alter `deployment_environments` in Flyway V49 — do not recreate the table or re-seed UUIDs.
2. Add org/project-scoped management columns, `status`, and metadata fields; keep `active` synchronized with status for legacy queries.
3. Replace global `code` uniqueness with partial indexes: global catalog rows keep unique `code`; project rows use unique `(organization_id, project_id, name)` and optional unique PRODUCTION per project.
4. Implement Platform API package `ai.nova.platform.environment` with RBAC (`ENVIRONMENT_RUN`, `ENVIRONMENT_READ`) and `nova.environment.*` configuration.
5. Persist labels, variable metadata (never values), append-only events, and immutable history snapshots.
6. Expose Portal `/environments` UI mirroring Release Policies patterns.
7. Never deploy, rollback, execute scripts, store secrets, or modify releases/deployments.

## Consequences

- Deployment Observation and Rollback Manager continue resolving global environments by code without migration of existing operations.
- Project environments become the durable registry for target metadata ahead of future deployment integrations.
- Hard delete remains disabled (`allow-delete=false`); archived environments are terminal.
