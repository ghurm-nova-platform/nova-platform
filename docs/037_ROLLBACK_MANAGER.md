# Rollback Manager

Sprint 4 Phase 3 — Rollback Manager plans, validates, and tracks rollback operations. It does not execute rollback. Execution will be implemented in a future sprint.

## Purpose

1. Create rollback plans linked to Releases and Deployments
2. Validate rollback eligibility (target, environment, lineage, manifest, version)
3. Persist append-only events and validation checks
4. Freeze an immutable SHA-256 plan hash after READY

## Safety boundary

Must **not**:

- execute rollback
- deploy software
- modify releases or deployments
- modify manifests
- delete rollback history
- store secrets

Portal safety statement:

> Rollback Manager plans and validates rollbacks. It does not execute rollback.

## Rollback status

`DRAFT`, `VALIDATING`, `READY`, `EXECUTING`, `SUCCEEDED`, `FAILED`, `CANCELLED`

## Strategies

`PREVIOUS_RELEASE`, `PREVIOUS_STABLE`, `SPECIFIC_RELEASE`, `HOTFIX_ONLY`, `CUSTOM`

## APIs

| Method | Path | Permission |
|--------|------|------------|
| POST | `/api/rollbacks/create` | `ROLLBACK_RUN` |
| POST | `/api/rollbacks/{id}/validate` | `ROLLBACK_RUN` |
| GET | `/api/rollbacks` | `ROLLBACK_READ` |
| GET | `/api/rollbacks/{id}` | `ROLLBACK_READ` |
| GET | `/api/rollbacks/{id}/history` | `ROLLBACK_READ` |

## Idempotency

Repeated create requests with identical plan inputs return the existing Rollback Plan (SHA-256 over canonical JSON).

## Configuration (`nova.rollback`)

| Property | Default |
|----------|---------|
| `enabled` | `true` |
| `execution-enabled` | `false` |
| `allow-plan-edit` | `false` |
| `retain-history` | `true` |

## Database (V47)

- `rollback_operations`
- `rollback_plans`
- `rollback_targets`
- `rollback_events`
- `rollback_validations`

## Error codes

`ROLLBACK_DISABLED`, `ROLLBACK_ALREADY_EXISTS`, `ROLLBACK_INVALID_STATUS`, `ROLLBACK_RELEASE_NOT_FOUND`, `ROLLBACK_DEPLOYMENT_NOT_FOUND`, `ROLLBACK_TARGET_NOT_FOUND`, `ROLLBACK_VALIDATION_FAILED`, `ROLLBACK_MANIFEST_MISMATCH`, `ROLLBACK_ENVIRONMENT_MISMATCH`, `ROLLBACK_VERSION_INCOMPATIBLE`

## Architecture notes

- Consumes Release Manager and Deployment Observation by reference (no duplicated living release/deployment records)
- Plans become immutable after READY
- Plan hash is SHA-256 over canonical JSON

## Known limitations

Planning only — no rollback execution, Kubernetes/ArgoCD/Helm rollback, or automatic rollback. Execution comes in a future sprint.

## Related

- [ADR-0026](adr/ADR-0026-rollback-manager.md)
- [Deployment Observation](036_DEPLOYMENT_OBSERVATION.md)
- [Release Manager](035_RELEASE_MANAGER.md)
