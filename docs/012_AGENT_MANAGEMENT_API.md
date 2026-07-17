# Agent Management API

Sprint 1 Phase 3 project-scoped agent definitions managed by Platform API.

## Boundary

```text
Browser → Platform API (/api/projects/{projectId}/agents)
             → Agent Runtime (server-to-server, no-op in this phase)
```

The browser never calls Agent Runtime and never receives runtime credentials.

## Ownership and tenancy

- Agents belong to a project.
- `organizationId` is always taken from the authenticated JWT, never from the request body.
- Lookups require `projectId` + `organizationId` + `agentId`.
- Cross-tenant access returns `AGENT_NOT_FOUND` / `PROJECT_NOT_FOUND`.

## Endpoints

| Method | Path | Permission |
|--------|------|------------|
| `GET` | `/api/projects/{projectId}/agents` | `AGENT_READ` |
| `GET` | `/api/projects/{projectId}/agents/{agentId}` | `AGENT_READ` |
| `POST` | `/api/projects/{projectId}/agents` | `AGENT_CREATE` |
| `PUT` | `/api/projects/{projectId}/agents/{agentId}` | `AGENT_UPDATE` |
| `PATCH` | `/api/projects/{projectId}/agents/{agentId}/status` | `AGENT_ACTIVATE` / `AGENT_ARCHIVE` |
| `DELETE` | `/api/projects/{projectId}/agents/{agentId}` | `AGENT_ARCHIVE` (archives) |

Query params for list: `search`, `status`, `page`, `size`, `sort`.

## Lifecycle

`DRAFT` → `ACTIVE` ↔ `PAUSED` → `ARCHIVED`

`DELETE` archives; active agents are not physically deleted.

## Model configuration

Provider allowlist (server config): `OPENAI`, `ANTHROPIC`, `GOOGLE`, `AZURE_OPENAI`, `LOCAL`.

No provider API keys are stored on agents.

## Optimistic locking

Update and status change require the current `version`. Conflicts return `OPTIMISTIC_LOCK_CONFLICT`.
