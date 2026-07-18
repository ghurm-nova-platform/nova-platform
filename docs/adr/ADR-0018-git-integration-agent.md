# ADR-0018: Git Integration Agent applies patches on isolated branches only

- Status: Accepted
- Date: 2026-07-18

## Context

After Patch Agent produces a validated Unified Diff, Nova needs a governed way to materialize that patch in Git without granting merge/push privileges or arbitrary shell access.

## Decision

1. Implement Git Integration Agent as a Platform API module that loads an approved PatchResult, creates `ai/task-{taskId}`, applies the patch with JGit, commits once, and persists metadata (V39).
2. Enforce RBAC with `GIT_RUN` / `GIT_READ`.
3. Reject unapproved/invalid patches, existing branches, apply/commit failures, and inconsistent repositories with stable `GIT_*` codes.
4. Never merge, push (including to main/develop), force-push, delete branches, or execute arbitrary shell commands.
5. Use deterministic branch names and commit messages; never reuse an existing working branch.

## Consequences

- Operators can inspect branch/commit/patch hashes in the portal before any later PR/merge agent runs.
- Remote push, PR creation, merge, and conflict resolution remain future work.
- Local workspace repositories under `nova.git.workspace-root` are project-scoped and initialized only when allowed by configuration.
