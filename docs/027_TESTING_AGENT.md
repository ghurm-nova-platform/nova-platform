# Testing Agent

Sprint 2 Phase 5 — Testing Agent generates structured test plans and test cases for Coding Agent artifacts. It does **not** execute tests, run build tools, modify repositories, or integrate CI.

## Purpose

Given an orchestration `taskId`, the Testing Agent:

1. Loads the task and latest generated artifacts (`ArtifactStorageService`)
2. Optionally loads latest Review findings (`ReviewStorageService`) when present
3. Builds a testing prompt (`TestingPromptBuilder`) with objective, artifacts, findings, language/framework/architecture, strategy, and JSON schema
4. Calls existing `AgentRuntimeClient` / Model Gateway **outside** any DB write transaction
5. Parses JSON into coverage estimate and generated tests / cases
6. Validates type, priority, title, description, and coverage (`TESTING_*` codes)
7. Persists `testing_results`, `generated_tests`, `generated_test_cases`, and `testing_reviewed_artifacts` (latest-only per task)

## APIs

| Method | Path | Permission |
|--------|------|------------|
| POST | `/api/testing/run` | `TESTING_RUN` |
| GET | `/api/testing/{taskId}` | `TESTING_READ` |

## Domain objects

- `TestingResult`, `GeneratedTest`, `TestCase`, `TestSuite`, `CoverageEstimate`
- `TestPriority`, `TestType`
- Flyway **V37**: `testing_results`, `generated_tests`, `generated_test_cases`, `testing_reviewed_artifacts`

## Test types

`UNIT`, `INTEGRATION`, `API`, `UI`, `DATABASE`, `SECURITY`, `PERFORMANCE`, `EDGE_CASE`, `NEGATIVE`

## Priority

`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`

## Validation error codes

`TESTING_UNKNOWN_TYPE`, `TESTING_UNKNOWN_PRIORITY`, `TESTING_INVALID_COVERAGE`, `TESTING_INVALID_JSON`, `TESTING_MISSING_TITLE`, `TESTING_MISSING_DESCRIPTION`, `TESTING_NO_ARTIFACTS`, `TESTING_TASK_NOT_FOUND`, `TESTING_NOT_FOUND`, …

## Coverage estimate

Integer **0–100** (estimate only — not measured execution coverage). Portal colors: 90+ green, 70–89 yellow, below 70 red.

## No execution

The Testing Agent must **not**:

- run Maven, Gradle, npm, Docker, or shell
- execute generated or existing code
- modify the repository or open PRs
- integrate CI runners

Execution engines are deferred to a future sprint.

## Portal

Route `/testing` — coverage gauge, generated suites, cases, priority badges, type filters, search, expandable details.

## Related

- ADR-0016
- Review Agent (`026`, ADR-0015)
- Coding Agent (`024`, ADR-0014)
- Orchestration foundation (`022`, ADR-0012)
