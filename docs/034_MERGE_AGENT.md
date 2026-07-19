# Merge Agent

Sprint 3 Phase 7 — Merge Agent is the sole Nova component permitted to merge a Pull Request into a protected target branch. It runs only after Approval Gate records a valid `APPROVED` decision with `eligibleForMerge=true` and matching evidence fingerprints. It never bypasses Approval Gate, deploys, reruns CI, or stores secrets.

## Purpose

Given an orchestration `taskId`, Merge Agent:

1. Loads the latest valid **Approval Gate** decision for the task
2. Validates preconditions (decision state, fingerprints, PR head, patch/commit alignment)
3. Selects merge method from organization/project configuration (`MERGE`, `SQUASH`, or `REBASE`)
4. Invokes the configured **provider** (GitHub initially) to merge the associated Pull Request
5. Verifies post-merge repository state
6. Persists append-only merge operations, validations, results, and timeline events

## Preconditions

Merge Agent refuses to run unless all of the following hold:

- Approval Gate is enabled and a decision exists for the task
- Decision value is `APPROVED` with `eligibleForMerge=true`
- Decision is not stale, expired, superseded, or invalidated
- Evidence and decision fingerprints match persisted Approval Gate rows
- Expected patch hash, commit hash, and PR head SHA align with trusted storage
- Pull Request operation exists and PR is mergeable per provider state
- No prior successful merge for the same `(organization, task, approval_decision_id)` tuple

Request bodies supply only `taskId` — never approval IDs, fingerprints, or provider tokens from the client.

## Safety boundary

Must **not**:

- merge without a valid Approval Gate decision
- bypass or weaken Approval Gate validation
- approve a GitHub Pull Request separately from merge (merge implies provider merge only)
- push unrelated commits, force-push, or delete branches outside merge scope
- deploy to any environment
- rerun, cancel, or trigger CI workflows
- modify patches, commits, or approval rows
- store credentials or secrets in merge tables or API responses

Portal safety statement:

> Merge Agent performs repository merge only after successful Approval Gate validation.

## Architectural boundary

```text
Review → Testing → Patch → Git → Pull Request → CI → Repair (optional)
                              ↓
                       Approval Gate (APPROVED + eligibleForMerge)
                              ↓
                         Merge Agent
                              ↓
                    merged commit on target branch
```

Merge Agent consumes Approval Gate output; it does not regenerate upstream evidence.

## Operation status

| Status | Meaning |
|--------|---------|
| `PENDING` | Operation created |
| `VALIDATING` | Preconditions and fingerprint checks running |
| `MERGING` | Provider merge in progress |
| `VERIFYING` | Post-merge verification |
| `SUCCEEDED` | Merge completed and verified |
| `FAILED` | Validation or provider merge failed |

## Merge methods

| Method | Description |
|--------|-------------|
| `MERGE` | Standard merge commit |
| `SQUASH` | Squash merge into target branch |
| `REBASE` | Rebase merge (when supported by provider and policy) |

## Validation

Persisted in `merge_validations` with results `PASSED`, `FAILED`, `SKIPPED`, or `ERROR`. Typical checks:

- Approval decision approved and eligible
- Evidence and decision fingerprint match
- Patch, commit, and PR head SHA alignment
- PR open and mergeable
- CI success when required by policy (observed state, not rerun)
- Idempotency — duplicate merge for same decision rejected

## Provider integration

- Uses the same provider abstraction as Pull Request Agent (GitHub REST initially)
- Provider credentials resolved via Provider Secret Vault at runtime — never returned in APIs
- Records provider name and safe provider message in `merge_results`
- External PR URLs may be returned when already public; tokens never are

## Idempotency

- Unique constraint on `(organization_id, task_id, approval_decision_id)` prevents duplicate merges for the same approval
- Re-running merge for an already-merged decision returns the persisted successful operation when fingerprints still match
- Validation failures persist failed operations; operators fix upstream state and re-run Approval Gate before retrying merge

## APIs

| Method | Path | Permission |
|--------|------|------------|
| POST | `/api/merge/run` | `MERGE_RUN` |
| GET | `/api/merge/{taskId}` | `MERGE_READ` |
| GET | `/api/merge/{taskId}/history` | `MERGE_READ` |

Run request:

```json
{
  "taskId": "11111111-1111-1111-1111-111111111025"
}
```

Response DTO (`MergeOperation`) includes: `status`, `mergeMethod`, `approvalDecisionId`, `eligibleForMerge`, fingerprints, `validations`, `mergedCommit`, `timeline`, `errorCode` — **no secrets**.

## Configuration

Prefix: `nova.merge`

| Property | Description |
|----------|-------------|
| `enabled` | Master switch (default `false` in main `application.yml`) |
| `default-merge-method` | Fallback when project policy omits value |
| `verify-after-merge` | Re-fetch provider state after merge |
| `fail-closed` | Any validation failure aborts merge |
| `allow-already-merged` | Treat provider already-merged PR as success when hashes match |

## Permissions

Seeded in V44:

- `MERGE_RUN`, `MERGE_READ`

## Database (V44)

- `merge_operations` — operation metadata, fingerprints, expected hashes, status
- `merge_validations` — precondition check results
- `merge_results` — merged commit, PR URL, provider, merged-at timestamp
- `merge_events` — append-only timeline

## Error codes

`MERGE_DISABLED`, `MERGE_APPROVAL_REQUIRED`, `MERGE_APPROVAL_EXPIRED`, `MERGE_APPROVAL_INVALIDATED`, `MERGE_APPROVAL_SUPERSEDED`, `MERGE_PATCH_MISMATCH`, `MERGE_COMMIT_MISMATCH`, `MERGE_PR_CHANGED`, `MERGE_CI_CHANGED`, `MERGE_BRANCH_PROTECTED`, `MERGE_ALREADY_COMPLETED`, `MERGE_PROVIDER_FAILED`, `MERGE_VERIFICATION_FAILED`, `MERGE_REMOTE_STATE_MISMATCH`, `MERGE_REMOTE_HEAD_MISMATCH`, `MERGE_COMMIT_NOT_VERIFIED`, `MERGE_OUTCOME_AMBIGUOUS`

## Post-merge remote verification

After `MergeProvider.merge` returns, Merge Agent always re-fetches the Pull Request and requires:

- remote state is merged
- repository owner/name and PR number match
- remote head SHA matches the approved pre-merge head
- a real merge commit SHA is present — never substitute the PR head SHA
- `VERIFY_PASSED` only after successful remote verification
- verification failure never yields `SUCCEEDED`

Ambiguous provider responses are resolved by remote refetch without a duplicate merge.

## Known limitations

- GitHub provider only in initial phase
- No auto-merge scheduling or batch merge queues
- No deployment or release tagging after merge
- No branch protection rule management
- Rebase merge depends on provider and repository settings
- Does not synchronize GitHub reviewer approval state — relies on Nova Approval Gate only
- SHA-256 fingerprints provide integrity correlation, not cryptographic non-repudiation
- Squash commit message customization is limited to provider defaults

## Portal

Route `/merge` — task ID, Run Merge / Load latest / Load history, eligibility badge, validation summary (passed/failed), approval decision id, masked fingerprints with copy, merge method, merged commit, timeline, history.

## Related

- ADR-0023
- Approval Gate (`033`, ADR-0022)
- Pull Request Agent (`030`, ADR-0019)
