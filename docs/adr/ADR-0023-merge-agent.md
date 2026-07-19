# ADR-0023: Merge Agent merges only after Approval Gate

- Status: Accepted
- Date: 2026-07-19

## Context

After the Sprint 3 agent pipeline produces patches, commits, pull requests, CI evidence, optional repairs, and an Approval Gate decision, Nova requires a single governed component that may merge code. Operators need durable validation, provider-neutral merge execution, post-merge verification, and strict separation from deployment — without allowing merge to bypass human and automated governance.

## Decision

1. Implement Merge Agent on Platform API with RBAC (`MERGE_RUN`, `MERGE_READ`) and feature flag `nova.merge.enabled`.
2. Persist merge operations, validations, results, and events (Flyway V44).
3. Require a valid `APPROVED` Approval Gate decision with `eligibleForMerge=true` and matching evidence/decision fingerprints — loaded from trusted storage, never from request-supplied IDs.
4. Validate patch hash, commit hash, PR head SHA, and PR mergeability before invoking the provider.
5. Support merge methods `MERGE`, `SQUASH`, and `REBASE` via the existing provider abstraction.
6. Enforce idempotency with unique `(organization_id, task_id, approval_decision_id)` and return persisted success when already merged.
7. Verify post-merge provider state when configured; record merged commit in `merge_results`.
8. Expose governance APIs and Portal UI; state explicitly that merge runs only after Approval Gate validation.
9. Do not deploy, rerun CI, modify approval rows, or store provider secrets in merge tables or responses.

## Consequences

- Merge is the only automated write that lands code on the target branch; all prior agents remain read-only or branch-isolated.
- Approval Gate remains the mandatory governance checkpoint; Merge Agent cannot weaken it.
- Failed validations produce auditable failed operations; operators re-run Approval Gate after fixing upstream drift.
- Provider credentials stay in the secret vault; merge APIs expose only public metadata and merged commit hashes.
- GitHub is the initial provider; other VCS providers require adapter work.
- Deployment, release automation, and branch protection management remain out of scope.
