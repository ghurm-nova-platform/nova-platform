# ADR-0018: Git Integration Agent applies patches on isolated operation workspaces

- Status: Accepted
- Date: 2026-07-18
- Updated: 2026-07-18 (workspace isolation + no synthetic seeds)

## Context

After Patch Agent produces a validated Unified Diff, Nova needs a governed way to materialize that patch in Git without granting merge/push privileges or arbitrary shell access. Concurrent operations for the same project must not share a mutable working tree.

## Decision

1. Implement Git Integration Agent as a Platform API module that loads an approved PatchResult, clones the project **source** into a dedicated `operations/{operationId}/repo` workspace, creates `ai/task-{taskId}`, applies the patch with JGit, commits once, and persists metadata (V39).
2. Enforce RBAC with `GIT_RUN` / `GIT_READ`.
3. Reject missing source (`GIT_REPO_MISSING`), missing baseRef (`GIT_INVALID_BASE`), unapproved/invalid patches, existing branches, apply/commit failures, and inconsistent repositories with stable `GIT_*` codes.
4. Never fall back from configured `baseRef` to HEAD. Never synthetic-initialize a repository during a normal API request (`nova.git.allow-init-repository=false`).
5. Never merge, push (including to main/develop), force-push, delete branches, or execute arbitrary shell commands.
6. Use deterministic branch names and commit messages; never reuse an existing working branch.
7. On failure: persist `FAILED` + `error_code`, skip success branch/commit rows, and preserve the isolated workspace for diagnosis by default.

## Consequences

- Operators can inspect branch/commit/patch hashes and operation workspace paths in the portal before any later PR/merge agent runs.
- Remote push, PR creation, merge, and conflict resolution remain future work.
- Project source under `workspaceRoot/{org}/{project}/source` is the immutable clone origin; task mutations occur only under `operations/{operationId}/repo`.
