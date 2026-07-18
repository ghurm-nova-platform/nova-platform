# Model Catalog, Capabilities, Aliases, and Provider Sync

Sprint 1 Phase 11 — database-driven AI model catalog owned by Platform API.

## Boundary

```text
Browser
  → Platform API (/api/ai-models, /api/model-providers/{id}/models/sync)
    → AiModelCatalogService / ModelAliasService / ModelCatalogSyncService
    → Allowlisted ProviderModelCatalogClient (OpenAI / Azure)
    → ai_models + ai_model_capabilities + ai_model_aliases
```

The browser never calls providers, never receives credentials, and never sees raw provider payloads.

## Architecture

| Concern | Owner |
|---------|--------|
| Catalog CRUD + lifecycle | `AiModelCatalogService` (`ai.nova.platform.modelcatalog`) |
| Aliases | `ModelAliasService` |
| Reference resolution | `ModelReferenceResolver` |
| Capability matching | `ModelCapabilityMatcher` |
| Provider discovery sync | `ModelCatalogSyncService` + catalog clients |
| Gateway invoke | `AiModelGateway` (optional `modelReference`) |
| Agent assignment routing | Existing `ModelRoutingService` (unchanged default path) |

Existing V20 `ai_models` was **evolved** (not replaced) so `project_models` / `agent_model_assignments` keep working.

## Schema

Migrations: `V26` (catalog columns + provider sync summary), `V27` (capabilities + aliases), `V28` (RBAC).

### Lifecycle

`DRAFT` → `ACTIVE` → `DISABLED` / `DEPRECATED` / `ARCHIVED`

Activation requires:

- same-org provider `ACTIVE`
- ≥1 enabled capability
- non-blank `provider_model_id`
- valid unique `model_key` (`^[a-z0-9][a-z0-9._:-]{1,149}$`)

Discovered models start as `DRAFT` with `source=PROVIDER_SYNC` and are never auto-activated.

### Capability semantics

Capabilities live in `ai_model_capabilities`. Boolean `supports_*` columns on `ai_models` are a denormalized cache updated when capabilities change (compat for routing).

Inference is conservative: sync mappers only assign capabilities from explicit controlled rules. Unknown provider model ids get **no** assumed capabilities.

### Alias resolution

Organization-scoped. Normalization: trim + `Locale.ROOT` lowercase.

Order:

1. Exact active `model_key`
2. Normalized active alias

If both match different models → `MODEL_REFERENCE_AMBIGUOUS`. Aliases cannot target `ARCHIVED` models.

## Sync behavior

`POST /api/model-providers/{providerId}/models/sync` requires `MODEL_CATALOG_SYNC`.

Prerequisites: provider ACTIVE, connection test SUCCESS, resolvable credential.

1. Short TX loads provider snapshot (version + credential/endpoint fields)
2. HTTP discovery runs **outside** any DB transaction (SSRF allowlist + test-only localhost unchanged)
3. Short TX upserts only if snapshot still matches; else `MODEL_SYNC_STALE`

Upsert rules:

- Key: `provider_id + provider_model_id`
- New rows: `DRAFT`, `PROVIDER_SYNC`, `discovered_at` set once
- Before HTTP, snapshot existing model ids + optimistic `version` + `source`
- On apply: if a matching model’s version changed during discovery, skip field overwrites (preserve operator edit)
- `source=MANUAL` (and operator-managed rows): never overwrite display/family/context/capabilities; only sync visibility timestamps when version is unchanged
- Update `last_synced_at` / `last_seen_at` for eligible provider-sync rows
- Never delete missing models
- Never auto-activate
- Do not replace manual aliases
- Capabilities on update: only seed when row has none and mapper has authoritative caps

### OpenAI

`GET /v1/models` via `ProviderRestClientFactory`. Capability mapping in `OpenAiModelCapabilityMapper`.

### Azure limitations

When Azure endpoint metadata (`AZURE_OPENAI_RESOURCE` + api version) is complete, sync calls `GET /openai/deployments?api-version=...`. Deployments are mapped with **no** fabricated chat capabilities unless controlled mapping applies.

If metadata is incomplete or discovery is not safely supported → `MODEL_SYNC_UNSUPPORTED` (honest failure; no invented inventory). Manual catalog entry remains supported.

## Gateway integration

`ModelGatewayRequest.modelReference` (optional):

- When set → resolve via catalog (key or alias) → ACTIVE model + ACTIVE provider → require enabled `CHAT` capability → additional capability checks → invoke with `provider_model_id`
- When absent → existing agent-assignment routing

Capability gates on the direct `modelReference` path:

- Chat generation → enabled `CHAT` (embeddings-only / image-generation-only models are rejected; `REASONING` alone is not accepted)
- Tools → `TOOL_CALLING` or `FUNCTION_CALLING`
- JSON / structured → `JSON_MODE` or `STRUCTURED_OUTPUT` when requested (future request-shape work if not yet on the invoke DTO)
- Vision inputs → `VISION` or `IMAGE_UNDERSTANDING` when requested (same)

Streaming capability may be recorded; production streaming is out of scope.

Nested `/api/model-providers/{id}/models` remains for compatibility; primary surface is `/api/ai-models`.

## Security

- Org isolation on every repository query
- No credentials, Authorization headers, fingerprints, or raw provider bodies in APIs/logs
- Provider errors use unified safe mapping
- Localhost overrides only under Spring `test` profile

## RBAC (V28)

| Permission | ORG_ADMIN | PROJECT_ADMIN | USER / ORG_MEMBER |
|------------|-----------|---------------|-------------------|
| `MODEL_CATALOG_READ` | Yes | Yes | Yes |
| `MODEL_CATALOG_CREATE` | Yes | No | No |
| `MODEL_CATALOG_UPDATE` | Yes | No | No |
| `MODEL_CATALOG_DELETE` | Yes | No | No |
| `MODEL_CATALOG_SYNC` | Yes | No | No |
| `MODEL_ALIAS_MANAGE` | Yes | No | No |
| `MODEL_CAPABILITY_MANAGE` | Yes | No | No |

## Portal

- `/ai-models` list / create / detail / edit
- Aliases + capabilities on detail
- Provider detail: Sync models (ACTIVE + connection SUCCESS + permission)

## Operational notes

1. Activate providers and run connection test before sync.
2. Review DRAFT discovered models, set capabilities if needed, then activate.
3. Prefer aliases (`primary-chat`) over hardcoding provider model ids in agents.
4. Re-sync updates `last_seen_at`; missing models are retained for history.

See [ADR-0011](adr/ADR-0011-model-catalog.md).

Orchestration runs may pass optional `modelReference` into the gateway; see [`022_MULTI_AGENT_ORCHESTRATION_FOUNDATION.md`](022_MULTI_AGENT_ORCHESTRATION_FOUNDATION.md).
