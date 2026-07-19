# ADR-0024: Release Manager owns immutable release lifecycle

- Status: Accepted
- Date: 2026-07-19

## Context

After Merge Agent lands code on the target branch, Nova needs an authoritative, auditable release record that deployment services can consume. Operators need semantic versioning, frozen manifests, and strict separation from deployment — without allowing release tooling to mutate merges, commits, or approvals.

## Decision

1. Implement Release Manager on Platform API with RBAC (`RELEASE_RUN`, `RELEASE_READ`) and feature flag `nova.release.enabled`.
2. Persist release operations, versions, contents, artifact references, and events (Flyway V45).
3. Support SEMVER with MAJOR/MINOR/PATCH automatic increment and optional explicit versions.
4. Generate an immutable SHA-256 release manifest covering metadata, commits, PRs, merges, approvals, and artifact refs; freeze at READY.
5. Enforce idempotent create via content fingerprint uniqueness per organization/project.
6. Expose create / prepare / publish / list / get / history APIs and Portal `/releases` UI.
7. Do not deploy, rollback, store secrets, or mutate commits/merges/approvals/manifests after READY.

## Consequences

- Release Manager becomes the source of truth for release identity before Deployment (PR #60).
- Manifest immutability after READY enables trustworthy downstream promotion.
- Identical create payloads reuse the existing release, avoiding duplicate version numbers for the same content.
- Artifact rows store references only; binary artifact storage remains out of scope.
