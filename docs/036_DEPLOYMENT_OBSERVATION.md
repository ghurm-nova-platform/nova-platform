# Deployment Observation

Sprint 4 Phase 2 — Deployment Observation monitors deployments across environments. It does not deploy software. It only observes, validates, records, and tracks deployment state for Releases created by Release Manager.

## Purpose

1. Record deployment observations linked to a Release
2. Track status, health, duration, provider, and environment
3. Verify observed outcomes against release manifest metadata
4. Persist append-only events, health snapshots, and artifact references

## Safety boundary

Must **not**:

- deploy software
- restart services
- rollback systems
- modify releases or manifests
- modify environments
- store secrets

Portal safety statement:

> Deployment Observation monitors deployments across environments. It does not deploy software.

## Environments

`DEVELOPMENT`, `TESTING`, `QA`, `STAGING`, `PRODUCTION`, `CUSTOM`

## Deployment status

`PENDING`, `STARTING`, `RUNNING`, `VERIFYING`, `SUCCEEDED`, `FAILED`, `CANCELLED`, `UNKNOWN`

## Health levels

`HEALTHY`, `WARNING`, `DEGRADED`, `FAILED`, `UNKNOWN`

## APIs

| Method | Path | Permission |
|--------|------|------------|
| POST | `/api/deployments/observe` | `DEPLOYMENT_RUN` |
| POST | `/api/deployments/{id}/verify` | `DEPLOYMENT_RUN` |
| GET | `/api/deployments` | `DEPLOYMENT_READ` |
| GET | `/api/deployments/{id}` | `DEPLOYMENT_READ` |
| GET | `/api/deployments/{id}/history` | `DEPLOYMENT_READ` |
| GET | `/api/deployments/environments` | `DEPLOYMENT_READ` |

## Idempotency

Repeated observe requests with the same deployment identity hash (release + environment + external key + provider + startedAt) return the existing Deployment.

## Configuration (`nova.deployment`)

| Property | Default |
|----------|---------|
| `enabled` | `true` |
| `observe-only` | `true` |
| `allow-external-events` | `true` |
| `retain-history` | `true` |

## Database (V46)

- `deployment_operations`
- `deployment_environments`
- `deployment_events`
- `deployment_health`
- `deployment_artifacts`

## Error codes

`DEPLOYMENT_DISABLED`, `DEPLOYMENT_ALREADY_EXISTS`, `DEPLOYMENT_NOT_FOUND`, `DEPLOYMENT_INVALID_STATUS`, `DEPLOYMENT_RELEASE_NOT_FOUND`, `DEPLOYMENT_VERIFICATION_FAILED`, `DEPLOYMENT_ENVIRONMENT_UNKNOWN`

## Known limitations

Observe only — no deployment execution, rollback, orchestration, environment promotion, Kubernetes, or ArgoCD integration. Execution comes in a future sprint.

## Related

- [ADR-0025](adr/ADR-0025-deployment-observation.md)
- [Release Manager](035_RELEASE_MANAGER.md)
