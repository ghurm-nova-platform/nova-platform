# ADR-0015: Review Agent evaluates artifacts only

- Status: Accepted
- Date: 2026-07-18

## Context

After Coding Agent produces generated artifacts, Nova needs a governed quality gate before future patch/commit phases. The reviewer must not rewrite files, create patches, or touch repositories in this phase.

## Decision

1. Implement Review Agent as a Platform API module that loads existing generated artifacts and calls Agent Runtime / Model Gateway for structured JSON findings only.
2. Persist `review_results`, `review_findings`, and `reviewed_artifacts` (V36) with RBAC (`REVIEW_RUN`, `REVIEW_READ`).
3. Validate severity, category, title, recommendation, and score with stable `REVIEW_*` codes.
4. Keep external model invocation outside database write transactions; store latest review per task via `ReviewStorageService`.
5. Do not modify artifacts, generate patches, execute shell, or edit git from this agent.

## Consequences

- Operators can inspect score, approval, and findings in the portal before any repository mutation phase.
- Patch generation and commit flows remain future work and can consume review outputs without changing this evaluation boundary.
- Invalid model output fails closed with stable API error codes.
- Review storage is latest-only per task (same intentional simplicity as Coding Agent V35 artifact replacement).
