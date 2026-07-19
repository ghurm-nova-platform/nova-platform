# ADR-0026: Rollback Manager is planning-only

- Status: Accepted
- Date: 2026-07-19

## Context

After Release Manager and Deployment Observation, operators need governed rollback planning before any future execution engine exists. Nova must not conflate planning/validation with performing rollbacks.

## Decision

1. Implement Rollback Manager on Platform API with RBAC (`ROLLBACK_RUN`, `ROLLBACK_READ`) and `nova.rollback.*` (`execution-enabled=false` by default).
2. Persist operations, plans, targets, events, and validations (Flyway V47).
3. Reference Releases and Deployments by ID; snapshot versions/environment for the immutable plan without mutating source records.
4. Enforce idempotent create via SHA-256 plan hash over canonical JSON.
5. Validate target existence/publication, deployment presence, environment match, release lineage, manifest integrity, and version compatibility.
6. Freeze plans as immutable after READY.
7. Expose Portal `/rollbacks` UI with list, status, versions, strategy, validation badges, plan hash, timeline, and history.
8. Never execute rollback, deploy, mutate releases/deployments/manifests, delete history, or store secrets.

## Consequences

- Rollback Manager becomes the audit trail for planned rollbacks before execution automation exists.
- Downstream execution services (future) can consume READY plans without Platform API performing rollbacks.
- Validation failures remain auditable without mutating release or deployment records.
