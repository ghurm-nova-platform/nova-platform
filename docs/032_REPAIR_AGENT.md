# Repair Agent

Sprint 3 Phase 5 — Repair Agent collects failure signals from review, testing, and CI; analyzes root causes; and generates a **new** `PatchResult` after a prior patch pipeline fails. It never overwrites prior patches, applies diffs, commits, pushes, merges, approves, deploys, or triggers CI.

## Purpose

Given an orchestration `taskId`, the Repair Agent:

1. Requires a prior **PatchResult** for the same task (immutable history)
2. Collects failure inputs from Review findings, Testing plans, CI observation, compile/test logs, static analysis, formatting, dependency, and coverage signals
3. Fingerprints inputs to deduplicate identical repair attempts (`input_fingerprint`)
4. Analyzes root causes and proposes repair actions (advisory metadata)
5. Calls Agent Runtime / Model Gateway **outside** any DB write transaction
6. Validates and persists a **new** `PatchResult` via `PatchStorageService.appendResult` (never deletes or overwrites prior patch rows used by Patch Agent)
7. Persists `repair_operations`, `repair_inputs`, `repair_actions`, and `repair_results`

## Safety boundary

Must **not**:

- overwrite or delete prior `PatchResult` rows
- apply patches or execute git
- commit, push, merge, or approve pull requests
- trigger, rerun, or cancel CI workflows
- deploy to any environment
- store credentials or secrets in repair tables or API responses

Portal safety statement: **"Repair Agent proposes fixes only. It never merges code."**

## Patch policy — NEW PatchResult

- Each successful repair creates a **new** `patch_results` row linked via `repair_operations.new_patch_result_id` and `repair_results.patch_result_id`.
- The prior patch remains immutable (`prior_patch_result_id`).
- `attempt_number` increments per distinct input fingerprint for the task.
- Operators inspect the new patch on `/patch` using the returned `patchResultId`; Git Integration and PR agents consume patches through the normal pipeline.

## APIs

| Method | Path | Permission |
|--------|------|------------|
| POST | `/api/repair/run` | `REPAIR_RUN` |
| GET | `/api/repair/{taskId}` | `REPAIR_READ` |
| GET | `/api/repair/{taskId}/history` | `REPAIR_READ` |

Request body for run:

```json
{
  "taskId": "11111111-1111-1111-1111-111111111025"
}
```

Response DTO (`RepairOperation`) includes: `status`, `reason`, `inputs`, `actions`, `repairedFiles`, `confidence`, `patchResultId`, `priorPatchResultId`, `attemptNumber`, `timeline`, `errorCode` — **no secrets**.

## Configuration

| Property | Description |
|----------|-------------|
| `nova.repair.enabled` | Master switch (default `false` in main `application.yml`) |
| `nova.repair.max-attempts` | Upper bound on repair attempts per task fingerprint |
| `nova.repair.min-confidence` | Minimum model confidence to mark repair succeeded |

## Status model

### Operation status (`RepairStatus`)

```text
PENDING → COLLECTING → ANALYZING → GENERATING_PATCH → VALIDATING → SUCCEEDED | FAILED
```

- **PENDING** — operation row created
- **COLLECTING** — loading review, testing, CI, and other failure inputs
- **ANALYZING** — ranking inputs and building repair prompt context
- **GENERATING_PATCH** — model gateway invocation for new Unified Diff
- **VALIDATING** — Patch Agent validation rules on generated diff
- **SUCCEEDED** — new `PatchResult` persisted and linked
- **FAILED** — repair could not complete (missing prior patch, validation failure, model error, etc.)

## Domain objects

- `RepairOperation`, `RepairInput`, `RepairAction`, `RepairResult`, `RepairedFile`
- Flyway **V42**: `repair_operations`, `repair_inputs`, `repair_actions`, `repair_results`
- Permissions seeded: `REPAIR_RUN`, `REPAIR_READ`

## Pipeline

```text
Coding → Review → Testing → Patch → Git → Pull Request → CI Observation
                                    ↓ (failures)
                              Repair Agent → NEW PatchResult → (re-enter Git/PR/CI manually)
```

Repair is advisory and generative only. Re-applying git integration or opening a new PR after a repair patch remains a separate human-gated step.

## Security

- RBAC: `REPAIR_RUN`, `REPAIR_READ`; `ORG_ADMIN` bypass
- No credentials, tokens, or vault references in DTOs or portal models
- Input collection reads only persisted Nova agent outputs (review findings, testing results, CI observation summaries)
- Model invocation uses existing Model Gateway allowlists and audit trails

## Error codes

`REPAIR_DISABLED`, `REPAIR_NO_PRIOR_PATCH`, `REPAIR_NO_FAILURE_INPUTS`, `REPAIR_DUPLICATE_FINGERPRINT`, `REPAIR_VALIDATION_FAILED`, `REPAIR_MODEL_ERROR`, `REPAIR_MAX_ATTEMPTS`, `REPAIR_NOT_FOUND`, …

## Known limitations

- Does not automatically re-run Git Integration, PR, or CI after generating a repair patch
- Confidence scores are model-estimated; low-confidence repairs may still persist as `FAILED`
- Input sources depend on upstream agents having run for the task (Review, Testing, CI Observation)
- Does not execute tests or compile code to verify fixes — validation is Unified Diff structure only
- Multiple repair attempts per task are keyed by input fingerprint; identical failures dedupe to one operation

## Portal

Route `/repair` — task ID form, Run / Load latest / Load history, status badge, reason, failure inputs, affected files, proposed actions, `patchResultId`, `priorPatchResultId`, confidence, timeline, repair history. No credential fields.

## Related

- ADR-0021
- Patch Agent (ADR-0017, `028`)
- CI Observation Agent (ADR-0020, `031`)
- Review Agent (`026`), Testing Agent (`027`)
