# Secure Provider Integration and Secret Vault

Sprint 1 Phase 10 — encrypted provider credentials, allowlisted OpenAI / Azure OpenAI adapters, and SSRF-safe connection testing on Platform API.

Builds on the AI Model Gateway ([`019_AI_MODEL_GATEWAY.md`](019_AI_MODEL_GATEWAY.md), ADR-0009).

## Boundary

```text
Browser
  → Platform API
      → Provider Secret APIs (metadata only after create/rotate)
      → SecretEncryptionService (AES-256-GCM)
      → AiModelGateway
      → Allowlisted AiModelProvider (DETERMINISTIC_LOCAL | OPENAI | AZURE_OPENAI)
      → Server-built allowlisted HTTPS endpoint
      → Provider-neutral RuntimeTurnResult
```

The browser never calls providers, never receives stored credentials after create/rotate, and never holds secrets in localStorage, sessionStorage, URLs, or route state.

## Secret vault

| Concern | Behavior |
|---------|----------|
| Algorithm | AES-256-GCM |
| Master key | `NOVA_SECRET_MASTER_KEY` (Base64-encoded 32 bytes) |
| Config | `nova.secrets.master-key=${NOVA_SECRET_MASTER_KEY:}` |
| Persistence | Ciphertext + nonce + `key_version` only — never plaintext |
| Metadata | optional `last4`, status, provider type (internal HMAC fingerprint is never returned) |

Startup fails (or vault operations fail clearly) when vault work is required without a valid master key. Tests use a fixed test-only Base64 key in `application.yml`.

### Reference formats

| Format | Example | Resolver |
|--------|---------|----------|
| Vault | `vault:provider-secret:<uuid>` | Decrypt ACTIVE org-scoped secret |
| Environment | `env:NOVA_PROVIDER_<NAME>` | Read process environment (Phase 9) |

Plaintext API keys, bearer tokens, PEM private keys, and JSON secrets are rejected. `DETERMINISTIC_LOCAL` requires `NULL` credentials.

Resolution order: vault reference first, then environment (`CompositeProviderCredentialResolver`).

### Create / rotate / revoke

| Operation | Plaintext in request | Response |
|-----------|----------------------|----------|
| Create | Once | Metadata + `credentialReference` only |
| Rotate | Once (new value) | Metadata only |
| Revoke | None | Status `REVOKED` |
| List / get | Never | Metadata only — secret never returned again |

Stored fields include ciphertext, nonce, algorithm `AES-256-GCM`, `key_version`, an internal HMAC fingerprint (never returned via API), and `last4` when the secret ends in four alphanumeric characters. Rotation creates a **new** ACTIVE secret row, marks the previous row `ROTATED`, remaps provider credential references, and returns the new `vault:provider-secret:<uuid>`.

Secret lifecycle: `ACTIVE` → `REVOKED` / `ARCHIVED`. Deterministic-local provider type is forbidden on vault rows.

## Production adapters

| Adapter key | Provider type | Endpoint profile | Host |
|-------------|---------------|------------------|------|
| `OPENAI` | `OPENAI` | `OPENAI_PUBLIC` | `api.openai.com` |
| `AZURE_OPENAI` | `AZURE_OPENAI` | `AZURE_OPENAI_RESOURCE` | `{resource}.openai.azure.com` |
| `DETERMINISTIC_LOCAL` | `DETERMINISTIC_LOCAL` | none | none |

Reserved (inactive until adapters exist): `ANTHROPIC`, `GOOGLE_GEMINI`, `AWS_BEDROCK`, `CUSTOM_MANAGED`.

Capabilities for OpenAI / Azure: tools, knowledge context, JSON output, and system messages supported; streaming not supported in this phase.

### Invoke paths (server-built only)

- OpenAI: `POST https://api.openai.com/v1/chat/completions`
- Azure OpenAI: `POST https://{resource}.openai.azure.com/openai/deployments/{deployment}/chat/completions?api-version=...`  
  (`deployment` = model `provider_model_id`)

Activation of OPENAI / AZURE_OPENAI requires a registered adapter, valid endpoint profile fields, a resolvable ACTIVE vault or present env credential, **and** `last_connection_test_status = SUCCESS` for the current credential/endpoint settings. Updating credential or endpoint fields resets connection test status to `NEVER`.

## Endpoint allowlisting / SSRF

- Clients never supply arbitrary provider URLs.
- Hosts are allowlisted; URLs are constructed server-side.
- TLS verification remains enabled.
- Cross-host redirects are not followed.
- HTTP uses Spring-managed bounded clients with provider timeouts.
- Non-allowlisted host strings are rejected before connect.
- Localhost / `127.0.0.1` overrides are allowed **only** when the Spring `test` profile is active (`TestLocalhostEndpointOverrideGate`). Non-test profiles always use `DenyLocalhostEndpointOverrideGate`. There is **no** production property that can disable SSRF / allowlist checks.

## Connection testing

`POST /api/model-providers/{providerId}/connection-test` requires `PROVIDER_CONNECTION_TEST`.

Loads a probe snapshot (provider version + credential/endpoint fields) in a short read transaction, runs the HTTP probe **outside** any database transaction, then persists status in a short write transaction **only if** the current provider still matches that snapshot. If credential or endpoint settings changed during the probe, the result is discarded (`NEVER` + `CONNECTION_TEST_STALE`).

Resolves credentials, probes an allowlisted lightweight endpoint, and updates:

- `last_connection_test_at`
- `last_connection_test_status` (`NEVER` | `SUCCESS` | `FAILED`)
- `last_connection_test_error_code` (safe code only)

Response: `{ status, errorCode, testedAt }` — never secrets or raw provider bodies.

## Error mapping

HTTP / provider failures map to gateway codes and `ProviderFailureKind` without raw bodies:

| Condition | Error code | Kind |
|-----------|------------|------|
| 401 | `PROVIDER_AUTHENTICATION_FAILED` | Permanent |
| 403 | `PROVIDER_PERMISSION_DENIED` | Permanent |
| 429 | `PROVIDER_RATE_LIMITED` | Transient |
| 408 / 504 | `PROVIDER_TIMEOUT` | Transient |
| 5xx | `PROVIDER_UNAVAILABLE` | Transient |
| Context length | `CONTEXT_LIMIT_EXCEEDED` | Permanent |

Retry/fallback continues to use transient vs permanent rules from Phase 9.

## Token usage

When the provider response includes usage, adapters map prompt/completion/total tokens into `ProviderInvokeResult`. Daily aggregates remain server-calculated; estimated costs stay administrative and clearly labeled. Browser clients cannot set usage.

## RBAC

| Permission | ORG_ADMIN | PROJECT_ADMIN |
|------------|-----------|---------------|
| `PROVIDER_SECRET_READ` | Yes | Yes |
| `PROVIDER_SECRET_CREATE` | Yes | No |
| `PROVIDER_SECRET_ROTATE` | Yes | No |
| `PROVIDER_SECRET_REVOKE` | Yes | No |
| `PROVIDER_CONNECTION_TEST` | Yes | Yes |

Cross-tenant secret access returns 404. Existing model-provider / model / routing permissions are unchanged.

## Audit privacy

Audit and INFO/WARN logs may include organization, actor, secret/provider IDs, status, and safe error codes only.

Must never include: plaintext secrets, Authorization headers, ciphertext as leakable text, prompts, completions, tool payloads, or raw provider responses.

## Migrations

- `V23__provider_secret_vault.sql` — `provider_secrets`
- `V24__provider_connection_metadata.sql` — connection columns on `ai_providers`
- `V25__provider_secret_permissions.sql` — secret + connection-test permissions

V1–V22 unchanged (including model gateway V20–V22 and knowledge V16–V19).

Phase 11 catalog migrations continue at V26–V28; see [`021_MODEL_CATALOG.md`](021_MODEL_CATALOG.md).

## Compatibility

| Concern | Behavior |
|---------|----------|
| Gateway TX1 / TX2 | Unchanged: validate + RUNNING, invoke outside TX, atomic COMPLETED vs CANCELLED |
| Cancel / timeout | `Future.cancel(true)`, permit release after worker ends; no assistant append or usage on cancel |
| `DETERMINISTIC_LOCAL` | Fully supported; no credentials; no network |
| `env:NOVA_PROVIDER_*` | Still valid alongside vault references |
| RAG / tools | Same gateway path; V19 knowledge snapshots; tool results return through gateway |
| Retry / fallback / concurrency | Process-local limits and distinct invocation rows unchanged |

## Limitations

- No streaming responses.
- No arbitrary OpenAI-compatible base URLs in production.
- No external KMS / cloud secret managers yet (local AES master key only).
- No distributed concurrency limiter.
- No billing reconciliation, fine-tuning, or image/audio providers.

## Future work

External KMS, additional vendor adapters, streaming, distributed concurrency, billing reconciliation.
