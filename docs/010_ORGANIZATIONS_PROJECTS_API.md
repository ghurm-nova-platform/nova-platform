# Organizations and Projects API

Sprint 1 Phase 2 APIs for organization and project management on Platform API.

## Boundary

```text
Browser → Platform API (/api/organizations, /api/projects) → Agent Runtime (server-to-server only)
```

## Organizations

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| `GET` | `/api/organizations` | Authenticated | List/search (paginated). Non-admins see their org only |
| `GET` | `/api/organizations/{id}` | Authenticated | Get by id (membership enforced) |
| `POST` | `/api/organizations` | `ORG_ADMIN` | Create organization |
| `PUT` | `/api/organizations/{id}` | `ORG_ADMIN` | Update organization |
| `DELETE` | `/api/organizations/{id}` | `ORG_ADMIN` | Delete organization |

Query params: `search`, `page`, `size`, `sort`.

Organization names and slugs are unique. Audit fields `createdBy` / `updatedBy` come from the JWT user.

## Projects

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| `GET` | `/api/projects` | Authenticated | List/search projects in the caller's organization |
| `GET` | `/api/projects/{id}` | Authenticated | Get project in caller's organization |
| `POST` | `/api/projects` | `PROJECT_ADMIN` (or `ORG_ADMIN`) | Create project |
| `PUT` | `/api/projects/{id}` | `PROJECT_ADMIN` (or `ORG_ADMIN`) | Update project |
| `DELETE` | `/api/projects/{id}` | `PROJECT_ADMIN` (or `ORG_ADMIN`) | Archive project (`status=ARCHIVED`) |

Project names are unique within an organization.

Statuses: `ACTIVE`, `DRAFT`, `ARCHIVED`  
Visibility: `PRIVATE`, `INTERNAL`, `PUBLIC`

## Example create project

```json
{
  "name": "Portal Redesign",
  "description": "UI refresh",
  "status": "ACTIVE",
  "visibility": "PRIVATE"
}
```
