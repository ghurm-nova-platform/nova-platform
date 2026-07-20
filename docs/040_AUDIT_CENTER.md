# Enterprise Audit Center

Sprint 4 Phase 6 — Enterprise Audit Center provides an authoritative append-only audit trail. It never modifies business data. Public APIs are read-only; internal services publish events through `AuditPublisher`.

## Purpose

1. Capture immutable audit events across releases, deployments, rollbacks, environments, policies, security, and REST API access
2. Support search, entity history, and correlation by `correlationId`, `requestId`, and `sessionId`
3. Remain separate from domain `*_audit_log` and `*_events` tables

## Safety boundary

Must **not**:

- modify business records
- expose public write APIs
- store secret values or password fields in `details_json`

Portal safety statement:

> Enterprise Audit Center is append-only and read-only in the portal. It never modifies business data.

## Permissions

| Code | Use |
|------|-----|
| `AUDIT_READ` | GET APIs and portal |
| `AUDIT_WRITE` | Internal `AuditPublisher` only |

Granted to ORG_ADMIN, PROJECT_ADMIN, USER, and system roles (V50 seed pattern).

## APIs (read-only)

| Method | Path | Permission |
|--------|------|------------|
| GET | `/api/audit` | `AUDIT_READ` |
| GET | `/api/audit/{id}` | `AUDIT_READ` |
| GET | `/api/audit/history?entityType=&entityId=` | `AUDIT_READ` |
| GET | `/api/audit/search` | `AUDIT_READ` |

No POST/PUT/DELETE on `/api/audit`.

## Idempotency

Duplicate `(organization_id, event_fingerprint)` returns the existing event (silent idempotent return).

## Configuration (`nova.audit`)

| Property | Default |
|----------|---------|
| `enabled` | `true` |
| `immutable` | `true` |
| `retain-history` | `true` |
| `capture-rest-api` | `true` |
| `capture-security-events` | `true` |

## Database (V50)

- `audit_events` — primary immutable events
- `audit_entities` — entity reference registry
- `audit_sessions` — session tracking
- `audit_correlation` — correlation/request/session links
- `audit_indexes` — denormalized search keys

## Error codes

`AUDIT_DISABLED`, `AUDIT_NOT_FOUND`, `AUDIT_IMMUTABLE`, `AUDIT_DUPLICATE_EVENT`, `AUDIT_INVALID_QUERY`, `AUDIT_SEARCH_FAILED`

## Publishers wired in Phase 6

- Auth login/logout (`capture-security-events`)
- REST API access filter (`capture-rest-api`)
- Environment create/update/status transitions
- Release policy create
- Deployment observation create
- Rollback plan create

See [ADR-0029](adr/ADR-0029-audit-center.md).
