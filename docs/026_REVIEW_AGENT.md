# Review Agent

Sprint 2 Phase 4 — Review Agent evaluates generated artifacts and returns structured findings. It never edits artifacts, generates patches, executes shell, or mutates git.

## Purpose

Given an orchestration `taskId`, the Review Agent:

1. Loads the task and latest generated artifacts (`ArtifactStorageService`)
2. Builds a review prompt (`ReviewPromptBuilder`) with objective, artifacts, conventions, architecture/security rules, and schema
3. Calls existing `AgentRuntimeClient` / Model Gateway **outside** any DB write transaction
4. Parses JSON into score, approval, and findings
5. Validates severity, category, title, recommendation, and score (`REVIEW_*` codes)
6. Persists `review_results`, `review_findings`, and `reviewed_artifacts` (latest-only per task)

## APIs

| Method | Path | Permission |
|--------|------|------------|
| POST | `/api/review/run` | `REVIEW_RUN` |
| GET | `/api/review/{taskId}` | `REVIEW_READ` |

## Domain objects

- `ReviewResult`, `ReviewFinding`, `ArtifactReview`, `ReviewRecommendation`
- `ReviewSeverity`, `ReviewCategory`
- Flyway **V36**: `review_results`, `review_findings`, `reviewed_artifacts`

## Categories

Correctness, Architecture, Maintainability, Readability, Security, Performance, Concurrency, Validation, Error Handling, Documentation, Naming, Testing, Best Practices

## Severity

`INFO`, `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`

## Validation error codes

`REVIEW_UNKNOWN_SEVERITY`, `REVIEW_UNKNOWN_CATEGORY`, `REVIEW_MISSING_TITLE`, `REVIEW_MISSING_RECOMMENDATION`, `REVIEW_INVALID_SCORE`, `REVIEW_INVALID_JSON`, `REVIEW_NO_ARTIFACTS`, `REVIEW_TASK_NOT_FOUND`, `REVIEW_NOT_FOUND`, …

## Quality score

Integer **0–100**. Portal colors: 90+ green, 70–89 yellow, below 70 red.

## Portal

Route `/review` — score badge, approval badge, severity counts, finding list with severity/category/artifact/search filters, expandable details.

## Related

- ADR-0015
- Coding Agent (`024`, ADR-0014)
- Orchestration foundation (`022`, ADR-0012)
