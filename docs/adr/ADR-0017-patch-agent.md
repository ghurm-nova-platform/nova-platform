# ADR-0017: Patch Agent generates diffs only

- Status: Accepted
- Date: 2026-07-18

## Context

After Coding, Review, and Testing agents produce artifacts and evaluations, Nova needs a governed way to produce Git-compatible Unified Diff patches before any future Git Integration applies them. This phase must not mutate repositories or invoke git.

## Decision

1. Implement Patch Agent as a Platform API module that loads approved Coding artifacts plus latest Review (required `approved=true`) and optional Testing context, then calls Agent Runtime / Model Gateway for JSON containing Unified Diff text only.
2. Persist `patch_results`, `generated_patches`, and `patch_artifacts` (V38) with RBAC (`PATCH_RUN`, `PATCH_READ`).
3. Validate Unified Diff structure (headers, hunks, paths, emptiness) and JSON with stable `PATCH_*` codes; store computed statistics from the parsed diff.
4. Keep external model invocation outside database write transactions; store latest patch per task via `PatchStorageService`.
5. Do not execute git, apply patches, commit, push, merge, run shell, or modify repositories from this agent.

## Consequences

- Operators can inspect and download validated patches in the portal before any Git Integration phase.
- Apply/commit/push flows remain future work and can consume persisted patch text without changing this generation boundary.
- Invalid model output fails closed with stable API error codes.
- Patch storage is latest-only per task (same intentional simplicity as Coding/Review/Testing agents).
