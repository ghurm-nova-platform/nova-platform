# ADR-0019: Pull Request Agent creates PRs only

- Status: Accepted
- Date: 2026-07-19

## Context

After Git Integration produces an isolated branch and commit, Nova needs a governed way to publish that branch to a configured remote and open a reviewable pull request without crossing into merge/approval automation.

## Decision

1. Implement Pull Request Agent on Platform API with RBAC (`PR_RUN`, `PR_READ`) and feature flag `nova.pull-request.enabled`.
2. Require SUCCEEDED Git Integration output; validate workspace cleanliness, branch naming, HEAD commit, and patch hash before any remote operation.
3. Push exactly one branch via JGit with explicit refspec `refs/heads/{branch}:refs/heads/{branch}`; never force-push, never push tags, never push-all.
4. Resolve credentials from `NOVA_GITHUB_TOKEN` / `nova.pull-request.github-token` at runtime only; never store tokens in `pull_request_*` tables or API DTOs.
5. Persist operations, remote push audit rows, and PR records (V40). Use `LOCAL` in-memory provider and `file://` bare remotes in tests.
6. Do not approve, merge, close, or auto-merge pull requests from this agent.

## Consequences

- Operators can publish agent branches and obtain PR links from the portal while merge decisions stay human-controlled.
- Failed pushes or provider errors persist `FAILED` with stable `PR_*` codes; successful pushes remain auditable even when PR creation fails afterward.
- Production requires GitHub token configuration; tests use `provider=LOCAL` without real credentials.
