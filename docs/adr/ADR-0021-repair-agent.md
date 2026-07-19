# ADR-0021: Repair Agent creates new PatchResults only

- Status: Accepted
- Date: 2026-07-19

## Context

After Coding → Review → Testing → Patch → Git → Pull Request → CI, failures often require another patch cycle. Overwriting prior `PatchResult` rows would break audit history and confuse Git Integration, which references a specific patch snapshot. Nova needs a governed repair agent that proposes fixes from persisted failure signals without mutating repositories, merging, or re-triggering CI.

## Decision

1. Implement Repair Agent on Platform API with RBAC (`REPAIR_RUN`, `REPAIR_READ`) and feature flag `nova.repair.enabled`.
2. Require an existing `PatchResult` for the task; link repairs via `prior_patch_result_id`.
3. Collect failure inputs from Review, Testing, CI Observation, and other allowlisted source types; dedupe attempts with `input_fingerprint`.
4. Persist normalized repair operations, inputs, actions, and results (V42).
5. On success, create a **new** `PatchResult` through `PatchStorageService.appendResult` — never delete or overwrite prior patch rows.
6. Validate generated diffs with existing Patch validation rules before marking `SUCCEEDED`.
7. Do not apply patches, execute git, commit, push, merge, approve PRs, deploy, or trigger/rerun CI from this agent.

## Consequences

- Operators can iterate on failed patch pipelines while retaining immutable patch history.
- Portal shows repair status, inputs, affected files, confidence, and linked `patchResultId` for follow-up on `/patch`.
- Failed repairs persist `FAILED` with stable `REPAIR_*` codes; successful repairs still require manual re-entry into Git/PR/CI flows.
- Repair attempts bounded by fingerprint deduplication and configurable `max-attempts`.
- Future auto-orchestration of re-git/re-PR after repair remains out of scope for this ADR.
