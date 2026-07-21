# ADR-0035: Enterprise Identity

## Status

Accepted

## Context

Sprint 6 Phase 3 delivered the Automated PR Review Engine (ADR-0034). Enterprise customers require a unified identity layer — external IdP federation, session visibility, MFA, login audit, and optional SCIM — without replacing the JWT foundation established in ADR-0002.

The existing Authentication API (`/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`, `/api/auth/me`) issues HS256 JWT access tokens with `userId`, `organizationId`, and `roles`. The portal stores tokens in `sessionStorage` and attaches `Authorization: Bearer` via an interceptor. Roadmap item "Enterprise SSO and LDAP" (post-beta) is advanced into Sprint 6 Phase 4 as a foundational identity module.

Requirements:

- Identity as the **sole authentication entry** for new enterprise sign-in flows
- **Wrap existing JWT** — same claim contract and portal interceptor; no breaking token format change
- Provider registry for `LOCAL`, `SAML`, `OIDC`, `OAUTH2`, `LDAP`, `ACTIVE_DIRECTORY`
- Session registry with admin revoke and concurrent session limits
- Login history and security-event audit trail
- MFA enrollment (TOTP + recovery codes); SMS / WebAuthn / passkeys deferred
- Optional SCIM 2.0 user/group create and list
- Full REST under `/api/identity`, portal multi-page `/identity`, Audit Center integration
- Granular permissions: `IDENTITY_READ`, `IDENTITY_ADMIN`, `IDENTITY_PROVIDER_ADMIN`, `IDENTITY_SESSION_ADMIN`, `IDENTITY_USER_ADMIN`, `IDENTITY_GROUP_ADMIN`, `IDENTITY_ROLE_ADMIN`, `IDENTITY_PERMISSION_ADMIN`, `IDENTITY_AUDIT_READ`, `IDENTITY_MFA_MANAGE`, `SCIM_PROVISION`

## Decision

Implement `ai.nova.platform.identity` on Platform API as the **enterprise identity layer**:

1. **Schema** — Flyway V58 creates identity users/providers/sessions/login history/MFA/groups/roles/permissions/API tokens/service accounts/refresh tokens; extends audit constraints for `IDENTITY`
2. **Controllers** — Auth, users, groups, roles, permissions, providers, sessions, API tokens, service accounts, dashboard/export under `/api/identity`; SCIM under `/api/scim/v2`
3. **Services** — Authentication, authorization, sessions, refresh tokens, MFA, sync hooks, export, Micrometer metrics
4. **JWT wrap** — successful authentication issues tokens via existing JWT infrastructure (ADR-0002)
5. **Password policies** — org-scoped rules enforced on local password flows
6. **Portal** — Angular `/identity` child routes with `identityPermissionGuard`
7. **Audit** — `AuditSource.IDENTITY`, `AuditEntityType.IDENTITY`
8. **OpenAPI** — `docs/openapi/identity-openapi.yaml`

Legacy `/api/auth/*` endpoints remain during migration and delegate to Identity when `nova.identity.enabled=true`.

Out of scope for this phase: WebAuthn/passkeys/FIDO2/biometrics, risk-based/adaptive auth, IGA certification campaigns, secrets vault products, cloud IAM connectors (Azure MI / AWS IAM / GCP IAM).

## Consequences

### Positive

- Single enterprise identity entry point without breaking existing JWT portal integration
- Provider-agnostic auth model with unified session and audit trail
- Operators gain session visibility, revoke, RBAC admin, and export tooling
- TOTP MFA and password policy for local accounts
- SCIM create/list surface for provisioned identities
- Clear upgrade path from password-only auth (ADR-0002) to enterprise federation

### Negative

- Dual auth paths (`/api/auth/*` and Identity) during migration period
- Live LDAP/AD/OIDC/OAuth2/SAML backends require environment-specific secrets and crypto validation beyond config-gated adapters
- SMS OTP absence may block some enterprise MFA policies until a later phase

### Alternatives considered

- **Replace JWT with opaque session cookies** — rejected; breaks existing portal interceptor and ADR-0002 contract
- **Third-party identity broker only (Auth0/Okta embed)** — rejected for self-hosted control-plane requirement
- **Extend `/api/auth/*` without Identity module** — rejected; conflates login endpoints with provider/session/MFA administration
- **WebAuthn in Phase 4** — deferred with other biometric/passkey work
- **Full SCIM PATCH/DELETE in Phase 4** — deferred; create/list sufficient for current provisioning flows

## References

- [Enterprise Identity](../046_ENTERPRISE_IDENTITY.md)
- [Identity OpenAPI](../openapi/identity-openapi.yaml)
- [Authentication API](../009_AUTH_API.md)
- [ADR-0002: JWT authentication foundation](ADR-0002-jwt-auth-foundation.md)
- [ADR-0034: Automated Pull Request Review Engine](ADR-0034-pr-review-engine.md)
