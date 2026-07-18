# Git Integration Agent

Sprint 3 Phase 2 — Git Integration Agent applies validated Patch Agent output onto an isolated working branch, creates one commit, and returns metadata.

It does **not** merge, push to main/develop, delete branches, resolve conflicts, review code, or run CI/CD.

## Purpose

Given an orchestration `taskId`, the agent:

1. Loads the latest approved `PatchResult` (`status=VALID`, validation passed)
2. Re-validates the Unified Diff
3. Creates isolated branch `ai/task-{taskId}` (never reused)
4. Applies the patch via JGit (no shell)
5. Verifies repository consistency
6. Creates one deterministic commit
7. Persists `git_operations`, `git_branches`, `git_commits`
8. Returns `GitOperation`

## APIs

| Method | Path | Permission |
|--------|------|------------|
| POST | `/api/git/run` | `GIT_RUN` |
| GET | `/api/git/{taskId}` | `GIT_READ` |

## Domain objects

- `GitOperation`, `GitBranch`, `GitCommit`, `GitApplyResult`, `GitValidation`
- `GitStatus` (`PENDING`, `SUCCEEDED`, `FAILED`)
- Flyway **V39**: `git_operations`, `git_branches`, `git_commits`

## Branch / commit

- Branch: `ai/task-{taskId}`
- Commit message: `AI: Apply approved patch for Task #{taskId}`

## Validation error codes

`GIT_PATCH_NOT_APPROVED`, `GIT_PATCH_VALIDATION_FAILED`, `GIT_BRANCH_EXISTS`, `GIT_APPLY_FAILED`, `GIT_COMMIT_FAILED`, `GIT_REPO_INCONSISTENT`, `GIT_INVALID_BRANCH`, …

## Security boundary

Allowlisted JGit operations only:

- create isolated branch from base ref
- apply patch
- stage + commit

Must **not**: merge, push, force push, delete branch, checkout unrelated branches for mutation, modify main/develop in place, or run arbitrary shell.

## Portal

Route `/git` — branch, commit hash, patch hash, status badge, execution timeline, copy branch/commit.

## Related

- ADR-0018
- Patch Agent (`028`, ADR-0017)
- Testing / Review / Coding agents
