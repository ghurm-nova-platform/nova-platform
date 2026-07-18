# ADR-0016: Testing Agent generates plans only

- Status: Accepted
- Date: 2026-07-18

## Context

After Coding Agent produces artifacts (and optionally Review Agent findings), Nova needs a governed way to produce structured test plans and cases before any future execution engines run. This phase must not execute tests or mutate repositories.

## Decision

1. Implement Testing Agent as a Platform API module that loads generated artifacts (and optional review findings) and calls Agent Runtime / Model Gateway for structured JSON test assets only.
2. Persist `testing_results`, `generated_tests`, `generated_test_cases`, and `testing_reviewed_artifacts` (V37) with RBAC (`TESTING_RUN`, `TESTING_READ`).
3. Validate test type, priority, title, description, and coverage estimate with stable `TESTING_*` codes.
4. Keep external model invocation outside database write transactions; store latest testing result per task via `TestingStorageService`.
5. Do not run Maven/Gradle/npm/Docker/shell, execute code, modify repositories, or integrate CI from this agent.

## Consequences

- Operators can inspect coverage estimates, suites, and cases in the portal before any execution phase.
- Test execution engines and CI integration remain future work and can consume persisted testing assets without changing this generation boundary.
- Invalid model output fails closed with stable API error codes.
- Testing storage is latest-only per task (same intentional simplicity as Coding Agent V35 and Review Agent V36).
