# Pull Request Agent

Sprint 3 Phase 3 — Pull Request Agent publishes successful Git Integration branches to a configured remote and creates reviewable pull requests. It never approves, merges, force-pushes, or mutates protected branches.

## Purpose

Given an orchestration `taskId`, the Pull Request Agent:

1. Requires a **SUCCEEDED** Git Integration operation for the same task
2. Validates the isolated git workspace (clean tree, branch `ai/task-{taskId}`, HEAD matches recorded commit, patch hash alignment)
3. Resolves project repository configuration (`project_repository_configs`)
4. Pushes the source branch with an explicit refspec only: `refs/heads/{branch}:refs/heads/{branch}` (no force, no tags, no push-all)
5. Creates or reuses an open pull request via the configured provider
6. Persists `pull_request_operations`, `remote_pushes`, and `pull_request_records`

## Safety boundary

Must **not**:

- approve or merge pull requests
- force-push
- push to protected base branches directly (only opens PR targeting allowlisted base refs)
- store credentials in the database or API responses

Portal safety statement: **"This agent creates Pull Requests but never approves or merges them."**

## APIs

| Method | Path | Permission |
|--------|------|------------|
| POST | `/api/pull-requests/run` | `PR_RUN` |
| GET | `/api/pull-requests/{taskId}` | `PR_READ` |
| GET | `/api/pull-requests/{taskId}/history` | `PR_READ` |

## Configuration

| Property | Description |
|----------|-------------|
| `nova.pull-request.enabled` | Master switch (default `false` in main `application.yml`) |
| `nova.pull-request.provider` | `GITHUB` (default) or `LOCAL` (in-memory provider for tests) |
| `nova.pull-request.github-token` | From `${NOVA_GITHUB_TOKEN:}` — never persisted in PR tables |
| `nova.pull-request.target-base-ref` | Default target branch when project config omits override |
| `nova.pull-request.allowed-base-refs` | Allowlisted PR base branches |
| `nova.pull-request.allowed-repository-hosts` | Allowlisted remote hosts (file remotes skip host checks) |
| `nova.pull-request.allow-force-push` | Must remain `false` in production policy |

## Domain objects

- `PullRequestOperation`, `RemotePushResult`, `PullRequestRecord`, `TimelineEvent`
- `PullRequestStatus`: `PENDING` → `VALIDATING` → `PUSHING` → `PUSHED` → `CREATING_PR` → `SUCCEEDED` | `FAILED`
- Flyway **V40**: `project_repository_configs`, `pull_request_operations`, `remote_pushes`, `pull_request_records`

## Error codes

`PR_DISABLED`, `PR_GIT_OPERATION_NOT_FOUND`, `PR_GIT_OPERATION_NOT_SUCCEEDED`, `PR_GIT_WORKSPACE_DIRTY`, `PR_REMOTE_BRANCH_CONFLICT`, `PR_PUSH_FAILED`, `PR_CREATE_FAILED`, `PR_CREDENTIALS_MISSING`, `PR_REMOTE_NOT_CONFIGURED`, …

## Known limitations

- GitLab provider enum exists in schema; GitHub adapter is the production implementation
- `LOCAL` provider and `file://` bare remotes are for automated tests only
- Idempotency: one successful PR operation per git operation; reruns return the existing SUCCEEDED row
- Existing open PR with matching head SHA skips push when remote branch already matches

## Portal

Route `/pull-requests` — status badge (all lifecycle states), provider, repository, branches, commit hashes, PR number/title, external link (`target=_blank` `rel=noopener noreferrer`), timeline, error code, copy actions. No credential fields.

## Related

- ADR-0019
- Git Integration Agent (ADR-0018)
- Patch Agent (`028`, ADR-0017)
