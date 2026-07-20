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
2. Internal append-only publisher (`AuditPublisher` with `REQUIRES_NEW`) and fingerprint idempotency
3. Read-only controller `/api/audit` guarded by `AUDIT_READ`
4. Optional REST capture filter and auth security event wiring behind `nova.audit` flags
5. Thin publish hooks from Environment, Policy, Deployment Observation, Rollback, and Auth services

Domain audit tables remain unchanged.

## Consequences

- Positive: unified searchable audit history without overloading domain event tables
- Positive: idempotent publisher avoids duplicate rows under retries
- Negative: additional storage and indexing overhead; publishers must avoid secrets in `details_json`
- Operational: disable via `nova.audit.enabled=false`; REST capture via `capture-rest-api=false`

## Alternatives considered

- Reusing existing `*_events` tables — rejected; mixed semantics and no cross-domain correlation
- Public write API — rejected; violates read-only audit boundary
