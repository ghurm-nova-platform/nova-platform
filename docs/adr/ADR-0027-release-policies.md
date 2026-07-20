# ADR-0027: Release Policies evaluate without mutating upstream records

- Status: Accepted
- Date: 2026-07-20

## Context

After Release Manager, Deployment Observation, and Rollback Manager, organizations need governed rules that decide whether a Release may advance. Policy evaluation must remain separate from mutation and deployment.

## Decision

1. Implement Release Policies on Platform API with RBAC (`POLICY_RUN`, `POLICY_READ`) and `nova.policy.*`.
2. Persist policies, versions, evaluations, evidence, and events (Flyway V48).
3. Support typed policies evaluated deterministically against Releases using reference-based upstream lookups.
4. Enforce idempotent create via policy fingerprint and idempotent evaluate via evaluation hash without duplicating evidence.
5. Keep evaluations immutable after completion; evidence append-only.
6. Expose Portal `/policies` UI with list, status, priority, mode, decision badges, evidence, timeline, and history.
7. Never modify releases, deployments, rollbacks, merges, approvals, or store secrets.

## Consequences

- Release Policies become the decision audit trail before READY/PUBLISHED gates are enforced by callers.
- Upstream systems remain authoritative; policies do not duplicate living records.
- Future OPA/Rego/scripting providers can plug into the same evaluation storage model.
