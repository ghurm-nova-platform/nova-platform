# Authentication API

Sprint 1 Phase 1 authentication endpoints exposed by Platform API.

## Boundary

```text
Browser → Platform API (/api/auth/*) → (later) Agent Runtime server-to-server
```

The browser never calls Agent Runtime and never sends internal API keys.

## Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/auth/login` | Public | Email/password login; returns access + refresh tokens |
| `POST` | `/api/auth/refresh` | Public | Rotate refresh token; returns new token pair |
| `POST` | `/api/auth/logout` | Public (refresh body) | Revoke refresh token |
| `GET` | `/api/auth/me` | Bearer access token | Current user profile and roles |

## Access token claims

| Claim | Description |
|-------|-------------|
| `sub` | User id (UUID string) |
| `userId` | User id (UUID string) |
| `organizationId` | Organization id (UUID string) |
| `roles` | Array of role codes (for example `ORG_ADMIN`) |
| `iat` | Issued-at |
| `exp` | Expiration |

## Login request

```json
{
  "email": "admin@nova.local",
  "password": "ChangeMe123!"
}
```

## Login response

```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<opaque>",
  "tokenType": "Bearer",
  "expiresInSeconds": 900
}
```

## Local demo user

Seeded by Flyway for local development only:

- Email: `admin@nova.local`
- Password: `ChangeMe123!`
- Role: `ORG_ADMIN`

Change the password before any shared environment. Do not reuse the seed password outside local demos.

## Relationship to Enterprise Identity

When `nova.identity.enabled=true` (default), `/api/auth/login`, `/api/auth/refresh`, and `/api/auth/logout` delegate to the Enterprise Identity `AuthenticationService`. Prefer `/api/identity/*` for new enterprise flows (MFA, providers, sessions, RBAC admin). See [Enterprise Identity](046_ENTERPRISE_IDENTITY.md) and [OpenAPI](openapi/identity-openapi.yaml).
