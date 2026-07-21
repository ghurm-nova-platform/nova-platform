# Enterprise Audit Center

Sprint 4 Phase 6 — Enterprise Audit Center provides an authoritative append-only audit trail. It never modifies business data. Public APIs are read-only; internal services publish events through `AuditPublisher`.

## Purpose

1. Capture immutable audit events across releases, deployments, rollbacks, environments, policies, security, REST API access, and Enterprise Identity (`AuditSource.IDENTITY`)
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

Fingerprint inputs are canonicalized: details maps are sorted recursively (`TreeMap` + `ORDER_MAP_ENTRIES_BY_KEYS`) before SHA-256. Idempotency applies when organization, actor, entity, action, result, severity, source, correlation/request IDs, and canonical details match. Different request IDs or materially different details produce distinct fingerprints.

## Failure behavior

`AuditPublisher` uses `REQUIRES_NEW` and **swallows** publish failures (logs a warning, never throws). Domain services wrap `AuditRecordingSupport.recordDomainEvent` in try/catch as defense in depth. Audit outages must not change business outcomes.

## Configuration (`nova.audit`)

| Property | Default |
|----------|---------|
| `enabled` | `true` |
| `immutable` | `true` |
| `retain-history` | `true` |
| `capture-rest-api` | `true` |
| `capture-security-events` | `true` |

## Database (V50 + V51)

- `audit_events` — primary immutable events
- `audit_entities` — entity reference registry (display labels may update)
- `audit_sessions` — session tracking (`ended_at` may update on logout)
- `audit_correlation` — correlation/request/session links
- `audit_indexes` — denormalized search keys

**V51** (`db/scripts/V51__audit_database_immutability.sql`, applied by Flyway Java migration `V51__audit_database_immutability`) extends CHECK constraints for lifecycle actions (`START`, `COMPLETE`, `FAIL`, `PREPARE`, `READY`, `PUBLISH`) and subsystem sources. Database-level immutability triggers block `UPDATE`/`DELETE` on `audit_events`, `audit_correlation`, and `audit_indexes` (PostgreSQL `reject_audit_mutation()` from the SQL script; H2 Java trigger equivalent in tests). `INSERT` remains allowed. `audit_sessions.ended_at` and `audit_entities.display_label` remain intentionally mutable.

## Error codes

`AUDIT_DISABLED`, `AUDIT_NOT_FOUND`, `AUDIT_IMMUTABLE`, `AUDIT_DUPLICATE_EVENT`, `AUDIT_INVALID_QUERY`, `AUDIT_SEARCH_FAILED`

## Publishers wired in Phase 6+

**Core platform**

- Auth login/logout (`capture-security-events`)
- REST API access filter (`capture-rest-api`)
- Environment create/update/status transitions
- Release policy create
- Deployment observation create
- Rollback plan create

**Orchestration & agents (Sprint 4 audit coverage)**

- Orchestration runs: create, update, ready, start, cancel, archive (`ORCHESTRATION`)
- Orchestration tasks: create, update (`ORCHESTRATION` / entity `TASK`)
- Planner plan/import (`PLANNER`)
- Coding generate (`CODING`)
- Review run (`REVIEW`)
- Testing run (`TESTING`)
- Patch run (`PATCH`)
- Git integration run (`GIT_INTEGRATION`)
- Pull request run (`PULL_REQUEST`)
- CI observation run (`CI_OBSERVATION`)
- Repair run (`REPAIR`)
- Approval gate validate/approve/reject (`APPROVAL_GATE`)
- Merge agent validate/merge (`MERGE_AGENT`)
- Release manager create/prepare/publish/fail (`RELEASE_MANAGER`)

Scheduler/worker paths without an authenticated user skip domain audit (no-op on null user).

See [ADR-0029](adr/ADR-0029-audit-center.md).
