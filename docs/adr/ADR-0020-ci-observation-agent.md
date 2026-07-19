# ADR-0020: Read-only CI observation via provider abstraction

- Status: Accepted
- Date: 2026-07-19

## Context

After the Pull Request Agent publishes a branch and opens a reviewable PR, operators need visibility into CI workflow health without granting Nova the ability to trigger reruns, approve merges, or deploy. CI providers differ (GitHub Actions first; GitLab later), so observation must sit behind a provider abstraction similar to other Nova agents.

## Decision

1. Implement CI Observation Agent on Platform API with RBAC (`CI_RUN`, `CI_READ`) and feature flag `nova.ci.enabled`.
2. Require SUCCEEDED Pull Request operation output; link observations to `pull_request_operation_id`.
3. Fetch workflow runs through a provider interface; GitHub Actions is the first production adapter.
4. Resolve credentials from `NOVA_GITHUB_TOKEN` / `nova.ci.github-token` at runtime only; never store tokens in `ci_*` tables or API DTOs.
5. Persist normalized workflow → job → step hierarchy (V41). Use `LOCAL` in-memory provider in tests.
6. Compute overall CI health, failure summary, and retry recommendation as read-only advisory output.
7. Do not trigger, rerun, cancel, approve, merge, or deploy from this agent.

## Consequences

- Operators can inspect CI status from the portal after PR creation while merge and deploy decisions stay human-controlled.
- Failed observations persist `FAILED` with stable `CI_*` codes; successful observations may still report `overallStatus=FAILED` when CI itself failed.
- Production requires GitHub token configuration for live Actions data; tests use `provider=LOCAL` without real credentials.
- Future GitLab support extends the provider interface without changing portal contracts.
