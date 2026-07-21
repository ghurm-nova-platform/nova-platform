# Enterprise Identity

Sprint 6 Phase 4 — org-scoped enterprise identity layer that becomes the sole authentication entry point for Nova Platform. Identity wraps the existing JWT foundation (ADR-0002) without replacing access-token claims or portal interceptors. External providers (SAML, OIDC, LDAP), session management, MFA enrollment, login audit, and optional SCIM provisioning are exposed via REST and the portal `/identity` route.

## Boundary

```text
Browser / SCIM clients
  → Platform API
      → IdentityController (/api/identity/*)
      → ScimUserController (/api/scim/v2/Users) [optional]
      → IdentityService (config, providers, sessions, login history, MFA)
      → Existing JWT issuance (wraps /api/auth/login refresh flow)
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
- Operators with `IDENTITY_ADMIN` may revoke non-current sessions via `POST /api/identity/sessions/{id}/revoke`
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

| Method | Path | Permission |
|--------|------|------------|
| GET | `/api/identity/config` | `IDENTITY_READ` |
| GET | `/api/identity/providers` | `IDENTITY_READ` |
| GET | `/api/identity/sessions` | `IDENTITY_READ` |
| POST | `/api/identity/sessions/{id}/revoke` | `IDENTITY_ADMIN` |
| GET | `/api/identity/login-history` | `IDENTITY_READ` |
| GET | `/api/identity/mfa/status` | `IDENTITY_READ` |
| POST | `/api/identity/mfa/enroll` | `IDENTITY_MFA_MANAGE` |
| POST | `/api/identity/mfa/verify-enrollment` | `IDENTITY_MFA_MANAGE` |
| GET | `/api/scim/v2/Users` | `SCIM_PROVISION` |

### MFA enroll request

```json
{
  "method": "TOTP"
}
```

### MFA verify enrollment request

```json
{
  "enrollmentToken": "<opaque>",
  "code": "123456"
}
```

## Permissions

| Permission | Purpose |
|------------|---------|
| `IDENTITY_READ` | View config, providers, sessions, login history, MFA status |
| `IDENTITY_ADMIN` | Revoke sessions; full identity administration |
| `IDENTITY_PROVIDER_MANAGE` | Configure identity providers |
| `IDENTITY_MFA_MANAGE` | Enroll and verify MFA for users |
| `SCIM_PROVISION` | List SCIM-provisioned users |

`ORG_ADMIN` bypasses permission checks.

## Database (V58)

Flyway `V58__enterprise_identity.sql` creates:

- `identity_providers` — org-scoped provider registry (type, name, status, metadata URLs, default flag)
- `identity_sessions` — active session records linked to users and refresh tokens
- `identity_login_events` — append-only login history (outcome, auth method, IP, user agent)
- `identity_mfa_enrollments` — MFA method enrollment state per user
- `identity_password_policies` — org password policy overrides

Audit constraints extended for `AuditSource.IDENTITY` and `AuditEntityType.IDENTITY`.

Permissions seeded: `IDENTITY_READ`, `IDENTITY_ADMIN`, `IDENTITY_PROVIDER_MANAGE`, `IDENTITY_MFA_MANAGE`, `SCIM_PROVISION`.

## Portal

Angular route `/identity` — Material tabs:

| Tab | Content |
|-----|---------|
| Providers | Registered SAML/OIDC/LDAP/local providers |
| Sessions | Active sessions with revoke (admin) |
| Login History | Success/failure audit trail |
| MFA | Enrollment status and TOTP/WebAuthn enrollment flow |

Requires `IDENTITY_READ` to view. Provider management, session revoke, MFA enrollment, and SCIM listing gated by respective permissions. Plain English strings (no TranslatePipe).

## Configuration

```yaml
nova:
  identity:
    enabled: true
    jwt-access-ttl-seconds: 900
    jwt-refresh-ttl-seconds: 604800
    session-max-concurrent: 5
    mfa-required: false
    scim-enabled: false
    saml-enabled: true
    oidc-enabled: true
    ldap-enabled: false
    password:
      min-length: 12
      require-uppercase: true
      require-lowercase: true
      require-digit: true
      require-special: true
      max-age-days: 90
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
