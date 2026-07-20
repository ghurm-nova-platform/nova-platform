# Release Policies

Sprint 4 Phase 4 — Release Policies define organizational rules that determine whether a Release may advance through its lifecycle. Policies are evaluated before a Release moves to READY or PUBLISHED. They never perform deployments and never modify Releases. They only evaluate and produce decisions.

## Purpose

1. Create, enable, and disable release policies
2. Evaluate policies against a Release using upstream references
3. Persist immutable evaluation decisions and append-only evidence
4. Track policy versions, events, and history

## Safety boundary

Must **not**:

- modify releases
- modify deployments
- modify rollback plans
- modify merge operations
- modify approvals
- store secrets

Portal safety statement:

> Release Policies evaluate whether a Release may advance. They do not modify releases or deploy software.

## Policy status

`ACTIVE`, `DISABLED`, `ARCHIVED`

## Policy decision

`PASSED`, `FAILED`, `WARNING`, `SKIPPED`, `ERROR`

## Policy types

`MINIMUM_APPROVALS`, `CI_REQUIRED`, `NO_FAILED_CHECKS`, `SIGNED_COMMITS_REQUIRED`, `SEMANTIC_VERSION_REQUIRED`, `MANIFEST_INTEGRITY`, `RELEASE_NOTES_REQUIRED`, `DEPLOYMENT_OBSERVATION_EXISTS`, `ROLLBACK_PLAN_EXISTS`, `CUSTOM_EXPRESSION`

## Evaluation modes

`ALL_REQUIRED`, `FIRST_FAILURE`, `BEST_EFFORT`

## APIs

| Method | Path | Permission |
|--------|------|------------|
| POST | `/api/policies` | `POLICY_RUN` |
| POST | `/api/policies/{id}/evaluate` | `POLICY_RUN` |
| POST | `/api/policies/{id}/enable` | `POLICY_RUN` |
| POST | `/api/policies/{id}/disable` | `POLICY_RUN` |
| GET | `/api/policies` | `POLICY_READ` |
| GET | `/api/policies/{id}` | `POLICY_READ` |
| GET | `/api/policies/{id}/history` | `POLICY_READ` |

## Idempotency

Repeated create requests with identical policy fingerprints return the existing policy. Repeated evaluations with identical inputs return the existing decision and do not duplicate evidence.

## Configuration (`nova.policy`)

| Property | Default |
|----------|---------|
| `enabled` | `true` |
| `allow-custom-policies` | `true` |
| `retain-history` | `true` |

## Database (V48)

- `release_policies`
- `policy_versions`
- `policy_evaluations`
- `policy_evidence`
- `policy_events`

## Error codes

`POLICY_DISABLED`, `POLICY_ALREADY_EXISTS`, `POLICY_NOT_FOUND`, `POLICY_INVALID_CONFIGURATION`, `POLICY_EVALUATION_FAILED`, `POLICY_EVIDENCE_MISSING`, `POLICY_ENGINE_ERROR`

## Architecture notes

- Consumes Release Manager, Merge Agent, Approval Gate, CI Observation, Deployment Observation, and Rollback Manager by reference
- Evaluations are immutable after completion; evidence is append-only
- `CUSTOM_EXPRESSION` supports deterministic `requireStatus` configuration only (no scripting language)

## Known limitations

No scripting language, external policy providers, OPA/Rego, or organization inheritance. These come in future sprints.

## Related

- [ADR-0027](adr/ADR-0027-release-policies.md)
- [Rollback Manager](037_ROLLBACK_MANAGER.md)
- [Release Manager](035_RELEASE_MANAGER.md)
