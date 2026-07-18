# Git Integration Agent

Sprint 3 Phase 2 — Git Integration Agent applies validated Patch Agent output onto an **isolated per-operation workspace**, creates one commit, and returns metadata.

It does **not** merge, push to main/develop, delete branches, resolve conflicts, review code, or run CI/CD.

## Purpose

Given an orchestration `taskId`, the agent:

1. Loads the latest approved `PatchResult` (`status=VALID`, validation passed)
2. Re-validates the Unified Diff and computes the patch hash
3. Requires an existing project **source** repository and configured `baseRef` (no synthetic init, no HEAD fallback)
4. Allocates a unique `operationId` and persists `PENDING`
5. Clones the source into `operations/{operationId}/repo` via JGit
6. Creates isolated branch `ai/task-{taskId}` inside that workspace only (never reused)
7. Applies the patch via JGit (no shell)
8. Creates one deterministic commit and verifies HEAD / branch / parent
9. Persists `SUCCEEDED` with `git_branches` / `git_commits`, or `FAILED` with a stable `GIT_*` error code
10. Returns `GitOperation`

## Workspace layout

```text
workspaceRoot/
  {organizationId}/
    {projectId}/
      source/                          # immutable clone source (never checked out for tasks)
      operations/
        {operationId}/
          repo/                        # isolated working tree for one Git run
```

## Failure cleanup policy

On branch creation, patch apply, or commit failure:

- Mark the operation `FAILED` and persist `error_code`
- Do **not** write `git_branches` / `git_commits` success rows
- Preserve the isolated operation workspace for diagnosis by default (`nova.git.cleanup-failed-workspaces=false`)
- Never leave the shared **source** repository on a changed branch (source is never mutated for task branches)

## APIs

| Method | Path | Permission |
|--------|------|------------|
| POST | `/api/git/run` | `GIT_RUN` |
| GET | `/api/git/{taskId}` | `GIT_READ` |

## Domain objects

- `GitOperation` (includes `errorCode`), `GitBranch`, `GitCommit`, `GitApplyResult`, `GitValidation`
- `GitStatus` (`PENDING`, `SUCCEEDED`, `FAILED`)
- Flyway **V39**: `git_operations`, `git_branches`, `git_commits`

## Branch / commit

- Branch: `ai/task-{taskId}`
- Commit message: `AI: Apply approved patch for Task #{taskId}`

## Validation error codes

`GIT_REPO_MISSING`, `GIT_INVALID_BASE`, `GIT_PATCH_NOT_APPROVED`, `GIT_PATCH_VALIDATION_FAILED`, `GIT_BRANCH_EXISTS`, `GIT_APPLY_FAILED`, `GIT_COMMIT_FAILED`, `GIT_REPO_INCONSISTENT`, `GIT_INVALID_BRANCH`, …

## Configuration

| Property | Default | Notes |
|----------|---------|-------|
| `nova.git.allow-init-repository` | `false` | Production must not synthetic-init on API requests |
| `nova.git.cleanup-failed-workspaces` | `false` | Preserve failed ops for diagnosis |
| `nova.git.base-ref` | `main` | Must exist in source; no fallback to HEAD |

Test fixtures may create source repositories via `TestGitSourceFixture` only.

## Security boundary

Allowlisted JGit operations only:

- clone source → operation workspace
- create isolated branch from base ref
- apply patch
- stage + commit

Must **not**: merge, push, force push, delete branch, checkout task branches in the shared source tree, modify main/develop in place, or run arbitrary shell.

## Portal

Route `/git` — branch, commit hash, patch hash, status badge, execution timeline, copy branch/commit.

## Related

- ADR-0018
- Patch Agent (`028`, ADR-0017)
- Testing / Review / Coding agents
