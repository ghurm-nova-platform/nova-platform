# ADR-0022: Approval Gate evaluates eligibility only

- Status: Accepted
- Date: 2026-07-19

## Context

After Coding → Review → Testing → Patch → Git → Pull Request → CI (and optional Repair), Nova needs a governed checkpoint before any future Merge Agent can act. Operators require configurable organizational policies, durable evidence binding, human approval workflows, and fail-closed automation — without granting the gate the ability to mutate repositories, CI, or deployments.

## Decision

1. Implement Approval Gate on Platform API with RBAC (`APPROVAL_GATE_RUN`, `APPROVAL_GATE_READ`, `APPROVAL_GATE_APPROVE`, `APPROVAL_GATE_REJECT`) and feature flag `nova.approval-gate.enabled`.
2. Persist versioned approval policies and normalized rules (Flyway V43).
3. Collect evidence exclusively from trusted storage services (`findLatest` by task/org) — never from request-supplied evidence IDs.
4. Evaluate mandatory automated rules via `ApprovalPolicyEvaluator` / `ApprovalRule` abstraction.
5. Compute SHA-256 evidence and decision fingerprints with deterministic field ordering (no secrets).
6. Record append-only decisions, requirements, evidence references, human actions, and timeline events.
7. Bind human approvals to exact evidence fingerprints; enforce distinct approvers and author-prohibition when configured.
8. Invalidate or supersede stale decisions when patch, commit, PR head, CI, or policy state changes.
9. Expose governance APIs and Portal UI; state explicitly that merge and deploy are never performed here.
10. Do not merge, GitHub-approve, push, modify patches/commits, rerun CI, or deploy from this component.

## Consequences

- Operators can see why a task is blocked, awaiting human approval, or eligible for future merge processing.
- Merge Agent (future) can consume `APPROVED` + `eligibleForMerge` + fingerprints as a hard prerequisite.
- Fail-closed behavior prevents silent approval when evidence is missing or stale.
- Human actions are auditable and immutable; withdrawal requires a new action row.
- GitHub-native reviewer state is not synchronized in this phase — Nova internal approvals only.
- Policy changes require new versions; active policies are not edited in place.
