# ADR-0029: Enterprise Audit Center

## Status

Accepted

## Context

Nova Platform already has domain-specific `*_audit_log` and `*_events` tables for individual modules. Sprint 4 Phase 6 requires a centralized, append-only audit trail that:

- never mutates business data
- exposes read-only APIs to operators
- supports correlation across services
- survives business transaction rollbacks when events are published from failing operations

## Decision

Introduce package `ai.nova.platform.audit` with:

1. Flyway migration `V50__audit_center.sql` creating `audit_events`, `audit_entities`, `audit_sessions`, `audit_correlation`, and `audit_indexes`
2. Flyway migration script `V51__audit_database_immutability.sql` (under `db/scripts`, applied by Java migration `V51__audit_database_immutability`) extending CHECK constraints and enforcing append-only triggers on events/correlation/indexes (PostgreSQL PL/pgSQL; H2 Java trigger in tests)
3. Internal append-only publisher (`AuditPublisher` with `REQUIRES_NEW`) and fingerprint idempotency with canonical JSON details
4. Read-only controller `/api/audit` guarded by `AUDIT_READ`
5. Optional REST capture filter and auth security event wiring behind `nova.audit` flags
6. Publish hooks from Environment, Policy, Deployment Observation, Rollback, Auth, Orchestration, Planner, all pipeline agents, Approval Gate, Merge Agent, and Release Manager

Domain audit tables remain unchanged. Mutable exceptions: `audit_sessions.ended_at`, `audit_entities.display_label`.

## Consequences

- Positive: unified searchable audit history without overloading domain event tables
- Positive: idempotent publisher avoids duplicate rows under retries
- Negative: additional storage and indexing overhead; publishers must avoid secrets in `details_json`
- Operational: disable via `nova.audit.enabled=false`; REST capture via `capture-rest-api=false`
- Operational: audit publish failures are swallowed; monitor publisher warnings
- Operational: V51 immutability triggers reject direct SQL mutation of event rows (`AUDIT_IMMUTABLE`)

## Alternatives considered

- Reusing existing `*_events` tables — rejected; mixed semantics and no cross-domain correlation
- Public write API — rejected; violates read-only audit boundary
