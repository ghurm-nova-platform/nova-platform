# Deployment Execution Engine

Sprint 5 Phase 1 — controlled deployment execution via pluggable providers. Never mutates Release entities.

## Purpose

1. Validate published releases, policies, rollback plans, environments, and manifest integrity before queueing
2. Queue idempotent deployment executions (`execution_fingerprint`)
3. Run provider lifecycle: prepare → deploy → verify
4. Persist steps, logs, artifacts, results, validations, and timeline events
5. Publish enterprise audit events (`DEPLOYMENT_EXECUTION` source)

## Safety boundary

Must **not**:

- mutate releases or manifests
- auto-rollback
- blue/green or canary strategies
- call real Kubernetes/ArgoCD/Helm clusters (adapters are dry-run stubs in Phase 1)

Portal safety statement:

> Deployment Execution Engine performs controlled deployments via pluggable providers. No automatic rollback, blue/green, or canary.

## Providers

| Code | Phase 1 behavior |
|------|------------------|
| `LOCAL` | Simulated instant success with in-memory artifacts |
| `REST` | POST to configured URL; dry-run when URL empty |
| `KUBERNETES` | Adapter dry-run stub |
| `ARGOCD` | Adapter dry-run stub |
| `HELM` | Adapter dry-run stub |

## Execution status

`READY`, `QUEUED`, `STARTING`, `DEPLOYING`, `VERIFYING`, `COMPLETED`, `FAILED`, `CANCELLED`

## APIs

| Method | Path | Permission |
|--------|------|------------|
| POST | `/api/deployment-executions/create` | `EXECUTION_RUN` |
| POST | `/api/deployment-executions/{id}/start` | `EXECUTION_RUN` |
| POST | `/api/deployment-executions/{id}/cancel` | `EXECUTION_RUN` |
| GET | `/api/deployment-executions` | `EXECUTION_READ` |
| GET | `/api/deployment-executions/{id}` | `EXECUTION_READ` |
| GET | `/api/deployment-executions/{id}/history` | `EXECUTION_READ` |
| GET | `/api/deployment-executions/{id}/logs` | `EXECUTION_READ` |

## Validation (pre-queue)

1. Release exists, org match, status `PUBLISHED`
2. ACTIVE policy evaluations `PASSED` when policies exist
3. READY rollback plan for release
4. Environment managed and `ACTIVE`
5. Manifest hash matches recomputed manifest
6. Content fingerprint matches release record
7. No active execution on same environment (`READY`, `QUEUED`, `STARTING`, `DEPLOYING`, `VERIFYING`)

Database enforcement (V53): partial unique index `uq_deploy_exec_one_active_environment` on `(organization_id, environment_id)` for those active statuses. Concurrent creates that race the application check receive HTTP 409 `EXECUTION_CONCURRENCY_BLOCKED` (no DB exception details).

## Execution dispatch

`POST /api/deployment-executions/{id}/start` returns **202 Accepted**:

1. Atomically claims `QUEUED → STARTING` (duplicate claim → `EXECUTION_ALREADY_STARTED`)
2. Dispatches prepare/deploy/verify to a bounded managed executor (`nova.execution.worker-count` / `queue-capacity`)
3. Each status transition uses a short `REQUIRES_NEW` transaction; provider I/O never runs inside an open DB transaction

## Cancellation

Cancel marks `cancel_requested` atomically, calls `provider.cancel` (cooperative), and finalizes `CANCELLED` when cancel wins the race against `COMPLETED`. Provider cancellation may be cooperative.

## Configuration (`nova.execution`)

| Property | Default |
|----------|---------|
| `enabled` | `true` |
| `provider` | `LOCAL` |
| `retry-count` | `0` |
| `verification-timeout-seconds` | `60` |
| `execution-timeout-seconds` | `300` |
| `worker-count` | `4` |
| `queue-capacity` | `32` |
| `allow-cancel` | `true` |
| `rest-base-url` | `""` |

Note: agent execution uses package `ai.nova.platform.execution` and does not consume `nova.execution` config.

## Database (V52 + V53)

- `deployment_executions` (+ `cancel_requested`, `active_environment_slot` in V53)
- `deployment_execution_steps`
- `deployment_execution_logs`
- `deployment_execution_results`
- `deployment_execution_artifacts`
- `deployment_execution_validations`
- `deployment_execution_events`
- V53 concurrency: PostgreSQL partial unique index `uq_deploy_exec_one_active_environment` on `(organization_id, environment_id) WHERE status IN (READY,QUEUED,STARTING,DEPLOYING,VERIFYING)`. H2 tests use the same index name on `(organization_id, active_environment_slot)` (nullable slot cleared on terminal status).

Permissions: `EXECUTION_RUN` (new). `EXECUTION_READ` reuses the agent execution read permission seeded in V11.

Audit CHECK extensions: actions `QUEUE`, `VERIFY`, `CANCEL`; source `DEPLOYMENT_EXECUTION`.

## Error codes

`EXECUTION_DISABLED`, `DEPLOYMENT_EXECUTION_NOT_FOUND`, `EXECUTION_INVALID_STATUS`, `EXECUTION_ALREADY_STARTED`, `EXECUTION_RELEASE_NOT_FOUND`, `EXECUTION_VALIDATION_FAILED`, `EXECUTION_POLICY_FAILED`, `EXECUTION_ROLLBACK_PLAN_MISSING`, `EXECUTION_ENVIRONMENT_NOT_FOUND`, `EXECUTION_CONCURRENCY_BLOCKED`, `EXECUTION_FAILED`, `EXECUTION_CANCEL_DISABLED`, `EXECUTION_QUEUE_FULL`, `EXECUTION_TIMEOUT`, `EXECUTION_VERIFY_TIMEOUT`

## Known limitations

No automatic rollback, no production deployment strategies, no blue/green/canary, Kubernetes/ArgoCD/Helm adapters only.

## Related

- [036_DEPLOYMENT_OBSERVATION.md](036_DEPLOYMENT_OBSERVATION.md)
- [037_ROLLBACK_MANAGER.md](037_ROLLBACK_MANAGER.md)
- [ADR-0030](adr/ADR-0030-deployment-execution.md)
