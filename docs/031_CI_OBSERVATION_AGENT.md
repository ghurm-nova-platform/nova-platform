# CI Observation Agent

Sprint 3 Phase 4 — CI Observation Agent fetches and summarizes CI workflow runs for successful Pull Request operations. It never reruns workflows, approves pull requests, merges, or deploys.

## Purpose

Given an orchestration `taskId`, the CI Observation Agent:

1. Requires a **SUCCEEDED** Pull Request operation for the same task
2. Resolves repository and PR context from the linked pull request operation
3. Fetches workflow runs from the configured CI provider (GitHub Actions first)
4. Normalizes workflow → job → step hierarchy with durations and failure reasons
5. Computes **overall CI health** (`SUCCESS`, `FAILED`, `CANCELLED`, `TIMED_OUT`, `IN_PROGRESS`, `UNKNOWN`)
6. Generates a human-readable **failure summary** and **retry recommendation** when applicable
7. Persists `ci_observation_operations`, `ci_workflow_runs`, `ci_jobs`, and `ci_steps`

## Safety boundary

Must **not**:

- trigger, rerun, cancel, or approve CI workflows
- merge or approve pull requests
- deploy to any environment
- store credentials in the database or API responses

Portal safety statement: **"This agent observes CI only. It never reruns, approves, merges, or deploys."**

## APIs

| Method | Path | Permission |
|--------|------|------------|
| POST | `/api/ci/run` | `CI_RUN` |
| GET | `/api/ci/{taskId}` | `CI_READ` |
| GET | `/api/ci/{taskId}/history` | `CI_READ` |

Request body for run:

```json
{
  "taskId": "11111111-1111-1111-1111-111111111024"
}
```

## Configuration

| Property | Description |
|----------|-------------|
| `nova.ci.enabled` | Master switch (default `false` in main `application.yml`) |
| `nova.ci.provider` | `GITHUB` (default) or `LOCAL` (in-memory provider for tests) |
| `nova.ci.github-token` | From `${NOVA_GITHUB_TOKEN:}` — never persisted in CI tables |
| `nova.ci.poll-interval-ms` | Interval while waiting for in-progress runs |
| `nova.ci.max-poll-attempts` | Upper bound on polling iterations |

## Status model

### Operation status (`CiObservationStatus`)

```text
PENDING → FETCHING → PROCESSING → SUCCEEDED | FAILED
```

- **PENDING** — operation row created, not yet fetching provider data
- **FETCHING** — calling provider APIs for workflow runs
- **PROCESSING** — normalizing runs, computing overall health and summaries
- **SUCCEEDED** — observation completed (overall CI may still be FAILED)
- **FAILED** — agent could not complete observation (provider error, missing PR, etc.)

### Overall CI health (`CiOverallStatus`)

```text
SUCCESS | FAILED | CANCELLED | TIMED_OUT | IN_PROGRESS | UNKNOWN
```

Derived from the latest relevant workflow runs for the PR branch/commit. `IN_PROGRESS` when any monitored run is still active. `UNKNOWN` when provider data is incomplete or ambiguous.

## Domain objects

- `CiObservationOperation`, `CiWorkflowRun`, `CiJob`, `CiStep`
- Flyway **V41**: `ci_observation_operations`, `ci_workflow_runs`, `ci_jobs`, `ci_steps`
- Permissions seeded: `CI_RUN`, `CI_READ`

## Error codes

`CI_DISABLED`, `CI_PR_OPERATION_NOT_FOUND`, `CI_PR_OPERATION_NOT_SUCCEEDED`, `CI_PROVIDER_ERROR`, `CI_CREDENTIALS_MISSING`, `CI_NO_WORKFLOWS_FOUND`, `CI_POLL_TIMEOUT`, …

## Known limitations

- GitLab provider enum exists in schema; GitHub Actions adapter is the production implementation
- `LOCAL` provider is for automated tests only (synthetic workflow data)
- Does not wait indefinitely for long-running workflows; polling bounded by `max-poll-attempts`
- Retry recommendations are advisory text only — no automatic reruns
- Only observes CI linked to Nova PR operations; ad-hoc branch CI outside the agent pipeline is out of scope

## Portal

Route `/ci` — operation status badge, overall CI badge, provider, repository, branches, commit, PR number, workflow cards with GitHub Actions links (`target=_blank` `rel=noopener noreferrer`), job cards, step timeline, durations, failed jobs/steps, failure summary, retry recommendation. No credential fields.

## Related

- ADR-0020
- Pull Request Agent (ADR-0019, `030`)
- Git Integration Agent (ADR-0018)
