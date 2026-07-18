# ADR-0010: Provider Secret Vault and Production Adapters

## Status

Accepted (Sprint 1 Phase 10)

## Context

Phase 9 introduced a provider-neutral model gateway with credential **references** resolved from allowlisted environment variables and a deterministic local adapter only. Production use needs encrypted secret storage, OpenAI / Azure OpenAI invocation, and SSRF-safe endpoints without regressing gateway transaction boundaries, cancellation, RAG, or tool calling. The browser must never hold or re-read stored API keys.

## Decision

1. Persist provider secrets as AES-256-GCM ciphertext in `provider_secrets` (Flyway V23), keyed by `NOVA_SECRET_MASTER_KEY` (Base64 32-byte master key).
2. Expose create / rotate / revoke / metadata APIs; accept plaintext only on create and rotate; never return stored secrets again.
3. Allow credential references `vault:provider-secret:<uuid>` and keep `env:NOVA_PROVIDER_*` for ops and local setups.
4. Ship Spring-managed `OPENAI` and `AZURE_OPENAI` adapters with server-built allowlisted hosts only (`api.openai.com`, `{resource}.openai.azure.com`).
5. Add connection metadata and connection-test APIs (V24–V25) that store safe status/error codes only.
6. Map provider HTTP failures to existing gateway error codes and transient/permanent kinds without logging raw bodies.
7. Preserve AiModelGateway TX1/TX2, cancel-on-timeout, DETERMINISTIC_LOCAL, RAG snapshots, and tool orchestration from ADR-0009.

## Alternatives considered

- **Environment variables only** — insufficient for multi-tenant org-managed credentials and rotation UX.
- **External KMS / cloud secret managers** — deferred; adds operational dependency before Sprint 1 needs are met.
- **Arbitrary OpenAI-compatible base URLs** — rejected for SSRF and supply-chain risk; allowlisted profiles only.
- **Vendor SDKs inside Platform API** — prefer bounded HTTP RestClient adapters to avoid SDK lock-in and large dependency surface.

## Consequences

### Positive

- Org-scoped encrypted credentials with clear RBAC and audit-safe metadata.
- Production chat adapters without weakening the browser → Platform API boundary.
- Clear upgrade path to external KMS and more vendors.

### Negative and risks

- Master key compromise decrypts vault ciphertext; key rotation / KMS is future work.
- Process-local concurrency and non-streaming limits from Phase 9 remain.

## Security and privacy impact

- No plaintext secrets in REST after create/rotate, logs, Angular storage, or invocation rows.
- No arbitrary provider URLs; TLS verification on; no cross-host redirects.
- Audits carry IDs and safe codes only — never prompts, completions, secrets, or raw provider payloads.

## Operational and cost impact

- Operators must set `NOVA_SECRET_MASTER_KEY` wherever vault secrets are used.
- Provider-reported token usage feeds existing daily aggregates; still no billing.

## Follow-up actions

- [ ] External KMS / secret-manager integration
- [ ] Additional vendor adapters (Anthropic, Gemini, Bedrock)
- [ ] Streaming responses
- [ ] Distributed concurrency limits
