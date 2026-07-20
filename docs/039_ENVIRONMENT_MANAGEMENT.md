# Environment Management

Sprint 4 Phase 5 — Environment Management registers org/project-scoped deployment targets and metadata. It extends the global `deployment_environments` catalog (V46) without replacing seeded rows. It never deploys software, stores secret values, or modifies releases or deployments.

## Purpose

1. Create and manage project-scoped environment definitions
2. Track labels, variable metadata (names/descriptions only), status lifecycle, events, and immutable history
3. Preserve the global environment catalog used by Deployment Observation and Rollback Manager

## Safety boundary

Must **not**:

- deploy or rollback software
- execute scripts
- store secret values
- modify releases or deployment operations

Portal safety statement:

> Environment Management registers deployment targets and metadata. It does not deploy software, store secrets, or modify releases.

## Environment status

`ACTIVE`, `DISABLED`, `MAINTENANCE`, `ARCHIVED`

The legacy `active` boolean stays in sync: `ACTIVE` and `MAINTENANCE` => `active=true`; `DISABLED` and `ARCHIVED` => `active=false`.

## Environment types

`DEVELOPMENT`, `TESTING` (API alias `TEST`), `QA`, `STAGING`, `PRODUCTION`, `CUSTOM`, `DR`

## APIs

| Method | Path | Permission |
|--------|------|------------|
| POST | `/api/environments` | `ENVIRONMENT_RUN` |
| PUT | `/api/environments/{id}` | `ENVIRONMENT_RUN` |
| POST | `/api/environments/{id}/enable` | `ENVIRONMENT_RUN` |
| POST | `/api/environments/{id}/disable` | `ENVIRONMENT_RUN` |
| POST | `/api/environments/{id}/archive` | `ENVIRONMENT_RUN` |
| GET | `/api/environments?projectId=` | `ENVIRONMENT_READ` |
| GET | `/api/environments/{id}` | `ENVIRONMENT_READ` |
| GET | `/api/environments/{id}/history` | `ENVIRONMENT_READ` |

## Idempotency

Repeated create requests with the same `(organization, project, name)` return the existing environment and append an `IDEMPOTENT_RETURN` event.

## Configuration (`nova.environment`)

| Property | Default |
|----------|---------|
| `enabled` | `true` |
| `allow-delete` | `false` |
| `retain-history` | `true` |
| `allow-multiple-production` | `false` |

## Database (V49)

Altered `deployment_environments` (org/project columns, status, metadata) plus:

- `environment_labels`
- `environment_variables_metadata`
- `environment_events`
- `environment_history`

## Error codes

`ENVIRONMENT_DISABLED`, `ENVIRONMENT_ALREADY_EXISTS`, `ENVIRONMENT_NOT_FOUND`, `ENVIRONMENT_INVALID_STATUS`, `ENVIRONMENT_INVALID_CONFIGURATION`, `ENVIRONMENT_DUPLICATE_NAME`, `ENVIRONMENT_DUPLICATE_TYPE`, `ENVIRONMENT_METADATA_INVALID`

## Architecture notes

- Global catalog rows (`organization_id` / `project_id` NULL) remain read-only for Deployment Observation (`findByActiveTrueOrderBySortOrderAsc`, `findByCodeIgnoreCase`).
- Project-scoped rows are managed exclusively through `/api/environments`.
- Status transitions are validated in the service layer; archived is terminal.

See [ADR-0028](adr/ADR-0028-environment-management.md).
