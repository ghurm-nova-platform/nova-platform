# ADR-0030: Deployment Execution Engine

- Status: Accepted
- Date: 2026-07-21

## Context

Deployment Observation (ADR-0025) tracks external deployment state without executing deploys. Sprint 5 Phase 1 introduces a separate execution engine that performs controlled deployments through pluggable providers while preserving release immutability and enterprise audit requirements.

## Decision

1. Implement `ai.nova.platform.deploymentexecution` (distinct from agent `ai.nova.platform.execution`).
2. Persist executions, steps, logs, artifacts, results, validations, and events (Flyway V52).
3. Validate releases (PUBLISHED), policies, rollback plans, environments, manifest hash, content fingerprint, and per-environment concurrency before queueing.
4. Enforce one active execution per organization+environment with a PostgreSQL partial unique index (V53); map constraint races to `EXECUTION_CONCURRENCY_BLOCKED`.
5. Claim `QUEUED→STARTING` atomically, return 202, and run prepare/deploy/verify on a bounded managed executor with short per-transition transactions (no provider I/O inside open DB transactions).
6. Support cooperative cancellation via `cancel_requested` so cancel can win against COMPLETED.
7. Support provider registry with runnable LOCAL/REST providers and dry-run K8s/ArgoCD/Helm adapters.
8. Expose REST API under `/api/deployment-executions` with `EXECUTION_RUN` / `EXECUTION_READ` RBAC and `nova.execution.*` configuration.
9. Publish audit events with `AuditSource.DEPLOYMENT_EXECUTION` and actions QUEUE/START/VERIFY/COMPLETE/FAIL/CANCEL.
10. Portal route `/deployment-execution` after Deployments with explicit safety statement.

## Consequences

- Operators can execute validated deployments without mutating release records.
- Observation and execution remain separate bounded contexts.
- Cluster integrations can replace adapter stubs in later phases without changing validation or audit contracts.
- No automatic rollback or progressive delivery strategies in Phase 1.
