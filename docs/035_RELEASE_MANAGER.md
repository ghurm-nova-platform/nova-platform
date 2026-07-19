# Release Manager

Sprint 4 Phase 1 — Release Manager is the authoritative source for software release lifecycle after Merge Agent completes. It creates immutable Release records that Deployment services consume later. It never deploys, rolls back, or mutates commits, merges, or approval decisions.

## Purpose

Given project-scoped release content, Release Manager:

1. Creates a **DRAFT** release with versioning (SEMVER MAJOR/MINOR/PATCH)
2. Records included merge operations, approval decisions, pull requests, patches, and commits
3. Prepares an immutable **Release Manifest** (SHA-256) and transitions to **READY**
4. Publishes the release (**PUBLISHED**) for downstream deployment consumers
5. Persists append-only release operations, versions, contents, artifact references, and timeline events

## Safety boundary

Must **not**:

- deploy software to any environment
- rollback deployments
- modify commits, merges, or approval decisions
- modify the release manifest after `READY`
- store secrets
- delete releases when `nova.release.allow-delete=false`

Portal safety statement:

> Release Manager creates immutable release records after Merge Agent completes. It does not deploy software.

## Architectural boundary

```text
Merge Agent (SUCCEEDED)
        ↓
  Release Manager (create → prepare → publish)
        ↓
 immutable Release + Manifest
        ↓
 Deployment services (PR #60 — out of scope)
```

## Release status

| Status | Meaning |
|--------|---------|
| `DRAFT` | Release created; content frozen for fingerprinting |
| `PREPARING` | Manifest generation in progress |
| `READY` | Manifest frozen; metadata immutable |
| `PUBLISHED` | Released for deployment consumers |
| `ARCHIVED` | Historical archive |
| `FAILED` | Prepare/publish failed |

## Versioning

- Strategy: `SEMVER` (`nova.release.default-version-strategy`)
- Bumps: `MAJOR`, `MINOR`, `PATCH` with automatic increment from the latest project version
- Explicit `semanticVersion` may be supplied; conflicts raise `RELEASE_VERSION_CONFLICT`
- Release numbers are sequential per `(organization, project)`

## Manifest

Canonical JSON (key-ordered) hashed with SHA-256 includes:

- release metadata (id, number, name, version, org, project, creator, timestamps)
- merge / approval / PR / patch IDs
- commit SHAs
- artifact references (URI + hash labels only — no binary storage)

After `READY`, the manifest is immutable (`allow-edit-after-ready=false`).

## Idempotency

Repeated `POST /api/releases/create` with identical release content (same sorted IDs, commits, and artifact refs) returns the existing Release for that `(organization, project, content_fingerprint)`.

## APIs

| Method | Path | Permission |
|--------|------|------------|
| POST | `/api/releases/create` | `RELEASE_RUN` |
| POST | `/api/releases/{id}/prepare` | `RELEASE_RUN` |
| POST | `/api/releases/{id}/publish` | `RELEASE_RUN` |
| GET | `/api/releases` | `RELEASE_READ` |
| GET | `/api/releases/{id}` | `RELEASE_READ` |
| GET | `/api/releases/{id}/history` | `RELEASE_READ` |

## Configuration (`nova.release`)

| Property | Default | Meaning |
|----------|---------|---------|
| `enabled` | `true` | Feature flag |
| `allow-edit-after-ready` | `false` | Block manifest mutation after READY |
| `allow-delete` | `false` | No delete API |
| `default-version-strategy` | `SEMVER` | Versioning strategy |

## Permissions

| Code | Purpose |
|------|---------|
| `RELEASE_RUN` | Create / prepare / publish |
| `RELEASE_READ` | List / get / history |

## Database (V45)

- `release_operations`
- `release_versions`
- `release_contents`
- `release_artifacts`
- `release_events`

## Error codes

| Code | When |
|------|------|
| `RELEASE_DISABLED` | Feature flag off |
| `RELEASE_ALREADY_EXISTS` | Duplicate release identity (documented; identical content returns existing) |
| `RELEASE_ALREADY_READY` | Prepare called when already READY |
| `RELEASE_ALREADY_PUBLISHED` | Publish called when already PUBLISHED |
| `RELEASE_MANIFEST_CHANGED` | Frozen manifest hash mismatch |
| `RELEASE_INVALID_STATUS` | Illegal lifecycle transition |
| `RELEASE_VERSION_CONFLICT` | Semantic version already used or invalid |

## Known limitations

- No deployment
- No rollback
- No artifact binary storage (references only)
- No environment promotion

Deployment is handled in PR #60.

## Portal

Route `/releases` shows release list, status, semantic version, included PRs/commits, manifest hash, timeline, and history.

## Related

- [ADR-0024](adr/ADR-0024-release-manager.md)
- [Merge Agent](034_MERGE_AGENT.md)
