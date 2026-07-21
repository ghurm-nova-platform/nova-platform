# ADR-0035: Enterprise Identity

## Status

Accepted

## Context

Sprint 6 Phase 3 delivered the Automated PR Review Engine (ADR-0034). Enterprise customers require a unified identity layer — external IdP federation, session visibility, MFA, login audit, and optional SCIM — without replacing the JWT foundation established in ADR-0002.

The existing Authentication API (`/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`, `/api/auth/me`) issues HS256 JWT access tokens with `userId`, `organizationId`, and `roles`. The portal stores tokens in `sessionStorage` and attaches `Authorization: Bearer` via an interceptor. Roadmap item "Enterprise SSO and LDAP" (post-beta) is advanced into Sprint 6 Phase 4 as a foundational identity module.

Requirements:

- Identity as the **sole authentication entry** for new enterprise sign-in flows
- **Wrap existing JWT** — same claim contract and portal interceptor; no breaking token format change
- Provider registry for `LOCAL`, `SAML`, `OIDC`, `LDAP`
- Session registry with admin revoke
- Login history audit trail
- MFA enrollment (TOTP, WebAuthn); SMS OTP deferred if needed
- Optional SCIM 2.0 user listing for provisioned identities
- REST API, portal visibility at `/identity`, and Audit Center integration
- Permissions: `IDENTITY_READ`, `IDENTITY_ADMIN`, `IDENTITY_PROVIDER_MANAGE`, `IDENTITY_MFA_MANAGE`, `SCIM_PROVISION`

## Decision

Implement `ai.nova.platform.identity` on Platform API as the **enterprise identity layer**:

1. **Schema** — Flyway V58 creates `identity_providers`, `identity_sessions`, `identity_login_events`, `identity_mfa_enrollments`, and `identity_password_policies`; extends audit constraints for `IDENTITY`
2. **IdentityController** — REST under `/api/identity`; config, providers, sessions, login history, MFA
3. **ScimUserController** — read-only `GET /api/scim/v2/Users` for provisioned user listing
4. **IdentityService** — orchestrates provider resolution, session lifecycle, MFA enrollment, and login event recording
5. **JWT wrap** — successful authentication through any provider issues tokens via existing JWT infrastructure (ADR-0002); Identity does not define a new token format
6. **Password policies** — org-scoped rules exposed in config; enforced on local password flows
7. **Authorization** — `IDENTITY_READ`, `IDENTITY_ADMIN`, `IDENTITY_PROVIDER_MANAGE`, `IDENTITY_MFA_MANAGE`, `SCIM_PROVISION`; `ORG_ADMIN` bypass
8. **Portal** — Angular `/identity` route with Providers, Sessions, Login History, and MFA tabs
9. **Audit** — `AuditSource.IDENTITY`, `AuditEntityType.IDENTITY`

Legacy `/api/auth/*` endpoints remain during migration. New enterprise flows route through Identity.

Out of scope: full production SAML XML crypto (optional phased), SMS OTP (deferred), SCIM write/provisioning UI, Agent Runtime auth changes, WebSockets, message brokers.

## Consequences

### Positive

- Single enterprise identity entry point without breaking existing JWT portal integration
- Provider-agnostic auth (local, SAML, OIDC, LDAP) with unified session and audit model
- Operators gain session visibility and revoke capability
- MFA enrollment path for TOTP and WebAuthn
- SCIM read surface for IdP-provisioned user visibility
- Clear upgrade path from password-only auth (ADR-0002) to enterprise federation

### Negative

- Dual auth paths (`/api/auth/*` and Identity) during migration period
- SAML/OIDC/LDAP adapters require careful secret handling and metadata validation
- Full SAML signature verification may lag behind metadata-first configuration
- SMS OTP absence may block some enterprise MFA policies until a later phase
- SCIM limited to read listing; full provisioning deferred

### Alternatives considered

- **Replace JWT with opaque session cookies** — rejected; breaks existing portal interceptor and ADR-0002 contract
- **Third-party identity broker only (Auth0/Okta embed)** — rejected for self-hosted control-plane requirement
- **Extend `/api/auth/*` without Identity module** — rejected; conflates login endpoints with provider/session/MFA administration
- **SMS OTP in Phase 4** — deferred; TOTP and WebAuthn cover primary enterprise MFA needs
- **Full SCIM CRUD in Phase 4** — rejected; read-only listing sufficient for portal visibility

## References

- [Enterprise Identity](../046_ENTERPRISE_IDENTITY.md)
- [Authentication API](../009_AUTH_API.md)
- [ADR-0002: JWT authentication foundation](ADR-0002-jwt-auth-foundation.md)
- [ADR-0034: Automated Pull Request Review Engine](ADR-0034-pr-review-engine.md)
