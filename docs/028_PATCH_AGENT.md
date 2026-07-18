# Patch Agent

Sprint 3 Phase 1 — Patch Agent generates Git-compatible Unified Diff patches from approved Coding Agent artifacts. It never applies patches, executes git/shell, commits, pushes, merges, or modifies repositories.

## Purpose

Given an orchestration `taskId`, the Patch Agent:

1. Loads Coding artifacts (`ArtifactStorageService`)
2. Loads latest Review result and requires `approved=true`
3. Loads latest Testing result when present (optional context)
4. Builds a patch prompt (`PatchPromptBuilder`) with task, artifacts, review, testing, conventions, and Unified Diff schema
5. Calls existing `AgentRuntimeClient` / Model Gateway **outside** any DB write transaction
6. Parses JSON into summary, statistics, patch text, and status
7. Validates Unified Diff structure (`PATCH_*` codes)
8. Persists `patch_results`, `generated_patches`, and `patch_artifacts` (latest-only per task)

## APIs

| Method | Path | Permission |
|--------|------|------------|
| POST | `/api/patch/run` | `PATCH_RUN` |
| GET | `/api/patch/{taskId}` | `PATCH_READ` |

## Domain objects

- `PatchResult`, `PatchFile`, `PatchSummary`, `PatchValidation`, `PatchStatistics`
- `PatchStatus` (`VALID`, `INVALID`)
- Flyway **V38**: `patch_results`, `generated_patches`, `patch_artifacts`

## Validation error codes

`PATCH_INVALID_DIFF`, `PATCH_MISSING_HEADERS`, `PATCH_MALFORMED_HUNK`, `PATCH_INVALID_PATH`, `PATCH_EMPTY`, `PATCH_INVALID_JSON`, `PATCH_NO_ARTIFACTS`, `PATCH_NO_REVIEW`, `PATCH_NOT_APPROVED`, `PATCH_NOT_FOUND`, …

## Statistics

Files changed, insertions, deletions, patch size (bytes).

## Security boundary

Must **not**:

- execute git
- apply patch
- commit / push / merge
- execute shell
- modify the repository

Git Integration is deferred to a later phase.

## Portal

Route `/patch` — summary, statistics, changed files, Unified Diff viewer, download `.patch`, validation badge.

## Related

- ADR-0017
- Testing Agent (`027`, ADR-0016)
- Review Agent (`026`, ADR-0015)
- Coding Agent (`024`, ADR-0014)
