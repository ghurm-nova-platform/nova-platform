# ADR-0002: JWT authentication foundation on Platform API

## Status

Accepted

## Context

Sprint 1 Phase 1 requires an authentication foundation so portal users can sign in
and every Platform API request can resolve `userId`, `organizationId`, and `roles`.
The browser must talk only to Platform API, never directly to Agent Runtime.

## Decision

- Use Spring Security with stateless JWT access tokens (HS256) issued by Platform API.
- Hash passwords with BCrypt.
- Persist organizations, users, roles, permissions, and refresh tokens in PostgreSQL
  via Flyway migrations and JPA entities.
- Model RBAC as User → Roles → Permissions.
- Store refresh tokens hashed (SHA-256) so logout/revocation is possible without
  storing raw refresh secrets.
- Angular portal stores access/refresh tokens in `sessionStorage` and attaches
  `Authorization: Bearer` via a JWT interceptor. Passwords are never stored.

## Consequences

- Protected endpoints require a valid access token.
- Enabling Dependency Graph / secrets management remains separate from this ADR.
- Future SSO/OIDC can replace password login while keeping the same JWT claim contract.
