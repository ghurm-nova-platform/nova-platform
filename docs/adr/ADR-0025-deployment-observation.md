# ADR-0025: Deployment Observation is observe-only

- Status: Accepted
- Date: 2026-07-19

## Context

After Release Manager publishes immutable release records, operators need durable visibility into deployment attempts across environments. Execution engines may integrate later; Nova must not conflate observation with deployment control.

## Decision

1. Implement Deployment Observation on Platform API with RBAC (`DEPLOYMENT_RUN`, `DEPLOYMENT_READ`) and `nova.deployment.*` (observe-only by default).
2. Persist operations, environments, events, health snapshots, and artifact references (Flyway V46).
3. Require a READY or PUBLISHED Release; record provider, environment, version, manifest hash, duration, and health.
4. Enforce idempotent observe via deployment hash uniqueness.
5. Provide verify to validate observed state against release metadata without deploying.
6. Expose Portal `/deployments` UI with list, environments, health badges, timeline, and history.
7. Never deploy, restart, rollback, mutate releases/manifests/environments, or store secrets.

## Consequences

- Deployment Observation becomes the audit trail for deployment attempts before execution automation exists.
- Downstream execution services (future) can emit observations without Platform API performing deploys.
- Health and verification failures remain auditable without mutating release records.
