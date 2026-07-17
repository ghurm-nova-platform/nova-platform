# Prompt Management API

Sprint 1 Phase 4 project-scoped reusable prompts managed by Platform API.

## Boundary

```text
Browser → Platform API (/api/projects/{projectId}/prompts)
             → (no LLM execution in this phase)
```

The browser never calls Agent Runtime and never receives AI provider secrets.
Preview performs deterministic `{{variable}}` substitution only.

## Ownership and tenancy

- Prompts belong to a project and an organization.
- `organizationId`, `createdBy`, `updatedBy`, and audit fields are taken from the
  authenticated JWT / server clock — never from the request body.
- Lookups require `projectId` + `organizationId` + `promptId`.
- Cross-tenant access returns `PROMPT_NOT_FOUND` / `PROJECT_NOT_FOUND`.

## Endpoints

| Method | Path | Permission |
|--------|------|------------|
| `GET` | `/api/projects/{projectId}/prompts` | `PROMPT_READ` |
| `GET` | `/api/projects/{projectId}/prompts/{promptId}` | `PROMPT_READ` |
| `POST` | `/api/projects/{projectId}/prompts` | `PROMPT_CREATE` |
| `PUT` | `/api/projects/{projectId}/prompts/{promptId}` | `PROMPT_UPDATE` |
| `DELETE` | `/api/projects/{projectId}/prompts/{promptId}` | `PROMPT_ARCHIVE` (archives) |
| `GET` | `/api/projects/{projectId}/prompts/{promptId}/versions` | `PROMPT_READ` |
| `GET` | `/api/projects/{projectId}/prompts/{promptId}/versions/{versionId}` | `PROMPT_READ` |
| `POST` | `/api/projects/{projectId}/prompts/{promptId}/versions` | `PROMPT_UPDATE` |
| `PUT` | `/api/projects/{projectId}/prompts/{promptId}/versions/{versionId}` | `PROMPT_UPDATE` |
| `POST` | `/api/projects/{projectId}/prompts/{promptId}/versions/{versionId}/publish` | `PROMPT_PUBLISH` |
| `POST` | `/api/projects/{projectId}/prompts/{promptId}/rollback` | `PROMPT_PUBLISH` |
| `POST` | `/api/projects/{projectId}/prompts/{promptId}/compare` | `PROMPT_COMPARE` |
| `POST` | `/api/projects/{projectId}/prompts/validate` | `PROMPT_READ` |
| `POST` | `/api/projects/{projectId}/prompts/preview` | `PROMPT_PREVIEW` |

List query params: `search`, `status`, `type`, `tag`, `page`, `size`, `sort`.

Compare body: `{ "leftVersionId", "rightVersionId" }`.

Rollback body: `{ "sourceVersionId", "reason?" }`.

## RBAC matrix

| Permission | ORG_ADMIN | PROJECT_ADMIN | USER / ORG_MEMBER |
|------------|-----------|---------------|-------------------|
| `PROMPT_READ` | ✓ | ✓ | ✓ |
| `PROMPT_CREATE` | ✓ | ✓ | |
| `PROMPT_UPDATE` | ✓ | ✓ | |
| `PROMPT_PUBLISH` | ✓ | ✓ | |
| `PROMPT_ARCHIVE` | ✓ | ✓ | |
| `PROMPT_COMPARE` | ✓ | ✓ | ✓ |
| `PROMPT_PREVIEW` | ✓ | ✓ | ✓ |

Security is enforced server-side. Frontend checks are UX only.

## Variable syntax

```text
{{variable_name}}
```

- Names: `^[a-zA-Z_][a-zA-Z0-9_]*$`
- Whitespace inside braces is trimmed
- Malformed placeholders are rejected
- No eval, no executable templates, no LLM calls

## Error codes

| Code | Typical HTTP |
|------|--------------|
| `PROMPT_NOT_FOUND` | 404 |
| `PROMPT_VERSION_NOT_FOUND` | 404 |
| `PROMPT_NAME_EXISTS` | 409 |
| `PROMPT_VERSION_IMMUTABLE` | 409 |
| `INVALID_PROMPT_STATUS` | 400/409 |
| `INVALID_PROMPT_TYPE` | 400 |
| `INVALID_PROMPT_VARIABLE` | 400 |
| `PROMPT_VALIDATION_FAILED` | 400 |
| `PROMPT_ALREADY_PUBLISHED` | 409 |
| `NO_DRAFT_VERSION` | 409 |
| `PROMPT_IN_USE` | 409 |
| `OPTIMISTIC_LOCK_CONFLICT` | 409 |
| `PROJECT_NOT_FOUND` | 404 |
| `FORBIDDEN` | 403 |
| `VALIDATION_ERROR` | 400 |

## Agent relationship

Agents may optionally reference `promptId` + `promptVersionId` when the version is
`PUBLISHED` and belongs to the same organization and project.

`systemPrompt` remains required on agents in this phase and will be deprecated later.
See [014_PROMPT_VERSIONING.md](014_PROMPT_VERSIONING.md) and [ADR-0004](adr/ADR-0004-prompt-versioning.md).

## Security

- Never log full prompt content by default
- Never log preview variable values
- Never store provider secrets in prompts
- Never store prompt content in browser `localStorage` / `sessionStorage`
- Never store raw prompt content in `prompt_audit_log`; version updates record
  metadata only (`versionNumber`, content hash, length, `changeSummary`)
- `validate` and `preview` require the path `projectId` to belong to the caller's organization
- Configurable max content length: `nova.prompts.max-content-length`

## Known limitations

- No real LLM execution or provider integration
- Monaco Editor deferred; Material textarea used in Portal
- Compare uses a simple line-based LCS diff
- Global tag catalog is not implemented (per-prompt tags only)
