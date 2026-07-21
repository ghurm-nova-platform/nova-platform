# Enterprise Identity

Sprint 6 Phase 4 — org-scoped enterprise identity layer that becomes the sole authentication entry point for Nova Platform. Identity wraps the existing JWT foundation (ADR-0002) without replacing access-token claims or portal interceptors. External providers (SAML, OIDC, LDAP), session management, MFA enrollment, login audit, and optional SCIM provisioning are exposed via REST and the portal `/identity` route.

## Boundary

```text
Browser / SCIM clients
  → Platform API
      → Identity*Controller (/api/identity/*)
      → ScimController (/api/scim/v2) [optional]
      → AuthenticationService + domain services (users, groups, roles, providers, sessions, tokens)
      → Existing JWT issuance (wraps /api/auth/* for portal compatibility)
      → Audit Center (AuditSource.IDENTITY)
      → PostgreSQL (V58 identity tables)
```

The browser continues to store JWT access/refresh tokens in `sessionStorage` and attach `Authorization: Bearer` via the existing interceptor. Identity does not expose Agent Runtime credentials or bypass Platform API.

## Architecture

Enterprise Identity sits above the Sprint 1 password/JWT stack:

1. **Authentication entry** — all sign-in flows (local password, SAML, OIDC, LDAP) resolve to the same JWT claim contract (`sub`, `userId`, `organizationId`, `roles`, permissions)
2. **Provider registry** — org-scoped identity providers with metadata and status; no provider secrets in portal responses
3. **Session registry** — tracks active refresh-token sessions with IP, user agent, auth method, and revoke support
4. **Login history** — append-only audit of success/failure/MFA-required outcomes
5. **MFA** — TOTP and WebAuthn enrollment; SMS OTP deferred if needed
6. **SCIM 2.0** — optional read-only user listing for provisioned identities (`SCIM_PROVISION`)

Identity is the **sole auth entry** for new enterprise flows; existing `/api/auth/*` endpoints remain for backward compatibility during migration (see ADR-0035).

## Auth methods

| Method | Description |
|--------|-------------|
| `LOCAL` | Email/password via BCrypt; existing demo seed user |
| `SAML` | Enterprise SAML 2.0 IdP federation (metadata-driven) |
| `OIDC` | OpenID Connect authorization code flow |
| `LDAP` | Directory bind against configured LDAP server |

Provider type values: `LOCAL`, `SAML`, `OIDC`, `LDAP`.

## MFA

| Method | Status |
|--------|--------|
| `TOTP` | Supported — authenticator app enrollment with QR URI |
| `WEBAUTHN` | Supported — security key / platform authenticator |
| `SMS` | Deferred — optional future phase if carrier integration required |

Enrollment status values: `NOT_ENROLLED`, `PENDING`, `ENROLLED`, `DISABLED`.

Org-level `mfaRequired` flag in config forces MFA before token issuance when enabled.

## SCIM

Minimal SCIM 2.0 surface for provisioned user visibility:

| Method | Path | Permission |
|--------|------|------------|
| GET | `/api/scim/v2/Users` | `SCIM_PROVISION` |

Full SCIM write (POST/PATCH/DELETE) and group provisioning are out of scope for this phase. Portal shows SCIM users only when `SCIM_PROVISION` is granted and the API returns data.

## Sessions

Sessions map to hashed refresh-token records with lifecycle metadata:

- Status: `ACTIVE`, `REVOKED`, `EXPIRED`
- Operators with `IDENTITY_SESSION_ADMIN` may revoke sessions via `DELETE /api/identity/sessions/{id}` or `DELETE /api/identity/sessions/revoke-all`
- `sessionMaxConcurrent` in config caps simultaneous active sessions per user

## JWT integration

Identity wraps existing JWT issuance — it does **not** introduce a new token format:

| Claim | Description |
|-------|-------------|
| `sub` / `userId` | User UUID |
| `organizationId` | Organization UUID |
| `roles` | Role codes (e.g. `ORG_ADMIN`) |
| Permissions | Resolved server-side; not embedded in JWT by default |

Access and refresh TTLs are configurable via identity config (`jwtAccessTtlSeconds`, `jwtRefreshTtlSeconds`) and align with existing auth properties.

## Password policies

Org-scoped password policy exposed in config:

| Field | Description |
|-------|-------------|
| `passwordMinLength` | Minimum password length |
| `passwordRequireUppercase` | Require uppercase letter |
| `passwordRequireLowercase` | Require lowercase letter |
| `passwordRequireDigit` | Require digit |
| `passwordRequireSpecial` | Require special character |
| `passwordMaxAgeDays` | Maximum password age (0 = no expiry) |

Policies apply to `LOCAL` provider password changes and new local accounts.

## REST API

All endpoints share base path `/api/identity` unless noted.

### Authentication (public unless noted)

| Method | Path | Auth | Notes |
|--------|------|------|-------|
| POST | `/login` | Public | Email/password (+ optional MFA code) |
| POST | `/logout` | Public | Body: `{ "refreshToken" }` |
| POST | `/logout-all` | Bearer | Revoke all sessions for current user |
| POST | `/refresh-token` | Public | Body: `{ "refreshToken" }` |
| POST | `/validate-token` | Bearer optional | Body: `{ "accessToken" }` |
| POST | `/change-password` | Bearer | Current + new password |
| POST | `/forgot-password` | Public | Generic success (token logged server-side) |
| POST | `/reset-password` | Public | Body: `{ "token", "newPassword" }` |
| POST | `/enroll-mfa`, `/mfa/enroll` | Bearer | TOTP enrollment |
| POST | `/verify-mfa`, `/mfa/verify` | Bearer / public | Confirm MFA enrollment |
| POST | `/disable-mfa` | Bearer | Disable MFA for current user |

### Providers

| Method | Path | Permission |
|--------|------|------------|
| GET/POST | `/providers` | `IDENTITY_READ` / `IDENTITY_PROVIDER_ADMIN` |
| GET/PUT/DELETE | `/providers/{id}` | `IDENTITY_READ` / `IDENTITY_PROVIDER_ADMIN` |
| POST | `/providers/{id}/test` | `IDENTITY_PROVIDER_ADMIN` |
| POST | `/providers/{id}/sync` | `IDENTITY_PROVIDER_ADMIN` |

### Users

| Method | Path | Permission |
|--------|------|------------|
| GET/POST | `/users` | `IDENTITY_USER_ADMIN` |
| GET/PUT/DELETE | `/users/{id}` | `IDENTITY_USER_ADMIN` |
| POST | `/users/{id}/enable\|disable\|unlock\|reset-password` | `IDENTITY_USER_ADMIN` |

### Groups, roles, permissions

Full CRUD at `/groups`, `/roles`, `/permissions` with `IDENTITY_GROUP_ADMIN`, `IDENTITY_ROLE_ADMIN`, `IDENTITY_PERMISSION_ADMIN` respectively. Groups also support `POST /groups/{id}/sync`.

### Session endpoints

| Method | Path | Permission |
|--------|------|------------|
| GET | `/sessions`, `/sessions/{id}` | `IDENTITY_READ` |
| DELETE | `/sessions/{id}`, `/sessions/revoke-all` | `IDENTITY_SESSION_ADMIN` |

### API tokens & service accounts

| Method | Path | Permission |
|--------|------|------------|
| GET/POST | `/api-tokens` | `IDENTITY_ADMIN` |
| DELETE | `/api-tokens/{id}` | `IDENTITY_ADMIN` |
| POST | `/api-tokens/{id}/revoke` | `IDENTITY_ADMIN` |
| GET/POST | `/service-accounts` | `IDENTITY_ADMIN` |
| PUT/DELETE | `/service-accounts/{id}` | `IDENTITY_ADMIN` |

### Dashboard, audit, export

| Method | Path | Permission |
|--------|------|------------|
| GET | `/config`, `/summary`, `/dashboard` | `IDENTITY_READ` |
| GET | `/login-history`, `/security-events` | `IDENTITY_AUDIT_READ` |
| GET/POST | `/export/{users\|groups\|roles\|permissions\|login-history}?format=csv\|json\|xlsx` | `IDENTITY_READ` |

### SCIM endpoints

| Method | Path | Permission |
|--------|------|------------|
| GET | `/api/scim/v2/Users` | `SCIM_PROVISION` |

## Permissions

| Permission | Purpose |
|------------|---------|
| `IDENTITY_READ` | View config, providers, sessions, dashboard, summary |
| `IDENTITY_ADMIN` | Full identity administration; API tokens and service accounts |
| `IDENTITY_PROVIDER_ADMIN` | Configure and sync identity providers |
| `IDENTITY_SESSION_ADMIN` | Revoke sessions |
| `IDENTITY_USER_ADMIN` | Manage identity users and admin password reset |
| `IDENTITY_GROUP_ADMIN` | Manage groups and group sync |
| `IDENTITY_ROLE_ADMIN` | Manage identity roles |
| `IDENTITY_PERMISSION_ADMIN` | Manage identity permission definitions |
| `IDENTITY_AUDIT_READ` | View login history and security events |
| `IDENTITY_MFA_MANAGE` | Enroll, verify, and disable MFA |
| `SCIM_PROVISION` | List SCIM-provisioned users |

`ORG_ADMIN` bypasses permission checks.

## Database (V58)

Flyway `V58__enterprise_identity.sql` creates:

- `identity_users` — linked platform users with lockout, failed-login count, password expiry, reset tokens
- `identity_providers` — org-scoped provider registry (type, name, status, metadata URLs, default flag)
- `identity_sessions` — active session records linked to users and refresh tokens
- `identity_login_history` — append-only login history (outcome, IP, user agent)
- `identity_mfa_factors`, `identity_recovery_codes`, `identity_password_history`
- `identity_groups`, `identity_roles`, `identity_permissions` and assignment join tables
- `identity_api_tokens`, `identity_service_accounts`, `identity_refresh_tokens`

Audit constraints extended for `AuditSource.IDENTITY` and `AuditEntityType.IDENTITY`.

Permissions seeded: `IDENTITY_READ`, `IDENTITY_ADMIN`, `IDENTITY_PROVIDER_ADMIN`, `IDENTITY_SESSION_ADMIN`, `IDENTITY_USER_ADMIN`, `IDENTITY_GROUP_ADMIN`, `IDENTITY_ROLE_ADMIN`, `IDENTITY_PERMISSION_ADMIN`, `IDENTITY_AUDIT_READ`, `IDENTITY_MFA_MANAGE`, `SCIM_PROVISION`.

## Portal

Angular feature `/identity` with child routes:

| Route | Page |
|-------|------|
| `/identity/dashboard` | Active users, sessions, failed logins, locked accounts, MFA adoption, providers, recent logins, security alerts |
| `/identity/providers` | LDAP / AD / OIDC / OAuth2 / SAML / SCIM provider CRUD, test, sync |
| `/identity/users` | Create/edit, enable/disable, unlock, reset password, roles/groups |
| `/identity/groups` | CRUD, assign users/roles, LDAP/AD sync |
| `/identity/roles` | CRUD, assign permissions, clone |
| `/identity/permissions` | CRUD, categories, search/filter |
| `/identity/sessions` | List/revoke sessions |
| `/identity/api-tokens` | Issue and revoke personal access tokens |
| `/identity/service-accounts` | Service account lifecycle |
| `/identity/audit` | Deep-link into Audit Center (`AuditSource.IDENTITY`) |
| `/identity/security-events` | Login/MFA/session/token/permission security timeline |
| `/identity/configuration` | Feature flags and policy summary |

Requires `IDENTITY_READ` to view. Admin actions gated by granular `IDENTITY_*` permissions. Plain English strings (no TranslatePipe).

## Configuration

```yaml
nova:
  identity:
    enabled: true
    session-idle-timeout: PT30M
    session-absolute-timeout: P1D
    max-concurrent-sessions: 5
    jwt:
      enabled: true
      expiration: PT15M
    refresh:
      expiration: P7D
    password:
      enabled: true
      min-length: 12
      require-uppercase: true
      require-lowercase: true
      require-digit: true
      require-special: true
      max-age-days: 90
    mfa:
      enabled: true
    ldap:
      enabled: false
    ad:
      enabled: false
    oidc:
      enabled: false
    oauth:
      enabled: false
    saml:
      enabled: false
    scim:
      enabled: false
    sync:
      enabled: false
    session:
      timeout: PT30M
```

## Audit

Identity lifecycle events (login, logout, session revoke, MFA enroll/verify, provider changes) publish to Audit Center with `AuditSource.IDENTITY` and `AuditEntityType.IDENTITY`.

## Constraints and non-goals

- **Wrap existing JWT** — no new token format; same portal interceptor contract (ADR-0002)
- **Identity as sole auth entry** — new enterprise flows route through Identity; legacy `/api/auth/*` retained during migration
- **No Agent Runtime auth changes** — browser never talks to Agent Runtime directly
- **Full production SAML crypto optional** — metadata-driven configuration first; full XML signature validation may be phased
- **SMS OTP deferred** — TOTP and WebAuthn prioritized; SMS added only if required
- **SCIM read-only in portal** — no SCIM write UI in this phase
- **No WebSockets, Kafka, RabbitMQ, or Redis**
- Provider secrets stored hashed/encrypted server-side; never returned in API responses

## Relationship to Authentication API

| Concern | Enterprise Identity (046) | Auth API (009) |
|---------|---------------------------|----------------|
| Scope | Providers, sessions, MFA, SCIM, policies | Login, refresh, logout, `/me` |
| Token format | Wraps same JWT claims | Issues JWT access tokens |
| Portal | `/identity` admin UI | `/login` sign-in page |

See [ADR-0035](adr/ADR-0035-enterprise-identity.md) and [Authentication API](009_AUTH_API.md).
