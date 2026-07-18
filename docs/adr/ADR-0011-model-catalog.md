# ADR-0011: Database-Driven Model Catalog

## Status

Accepted (Sprint 1 Phase 11)

## Context

Phase 9 introduced `ai_models` with boolean capability flags and agent assignment routing. Phase 10 added production adapters and a secret vault. Operators still needed a catalog that can sync provider model inventories, express fine-grained capabilities, resolve stable aliases, and feed the gateway without hardcoding model names in Java or Angular.

## Decision

1. Evolve existing `ai_models` (V26) instead of creating a parallel table, preserving FKs from project/agent assignments.
2. Keep `model_key` as the org-stable Nova identifier and `provider_model_id` as the vendor deployment/model id; uniqueness is `(organization_id, model_key)` and `(provider_id, provider_model_id)`.
3. Store capabilities in `ai_model_capabilities` (V27) with conservative inference only from explicit mappers or operator edits.
4. Scope aliases to the organization; normalize with `Locale.ROOT` lowercase; resolve key then alias; fail closed on ambiguity.
5. Discover models via allowlisted HTTP clients; OpenAI lists `/v1/models`; Azure uses deployments listing when metadata allows, otherwise returns `MODEL_SYNC_UNSUPPORTED` without fabricating inventory.
6. New discovered models start `DRAFT` / `PROVIDER_SYNC` and are never auto-activated or auto-deleted when absent from a later sync.
7. Run external sync HTTP outside DB transactions with stale-result protection (provider version + endpoint snapshot), matching connection-test hardening from ADR-0010.
8. Add optional `modelReference` on gateway requests for catalog resolution while keeping agent-assignment routing as the default compatibility path.

## Alternatives considered

- **Hardcoded model enums in code** — rejected; blocks multi-tenant and multi-vendor evolution.
- **New parallel catalog tables** — rejected; would bifurcate routing FKs and duplicate lifecycle.
- **Auto-activate / auto-delete on sync** — rejected; unsafe for production traffic and history.
- **Aggressive substring capability inference** — rejected; false positives (e.g. “vision” in unrelated ids).

## Consequences

### Positive

- Operators manage models and aliases without redeploys.
- Gateway can resolve stable keys/aliases with capability enforcement.
- Sync cannot overwrite newer provider configuration mid-flight.

### Negative and risks

- Azure deployment listing remains limited by API/metadata; manual catalog entry required when unsupported.
- Dual surfaces (`/api/ai-models` and nested provider model routes) until nested APIs are fully deprecated.

## Security and privacy impact

No secrets or raw provider bodies leave Platform API. SSRF allowlisting and test-only localhost gates are unchanged. Catalog RBAC is independent of portal visibility.

## Operational and cost impact

Sync increases outbound provider list calls; operators should sync intentionally after connection tests succeed. Pricing fields remain administrative estimates.

## Follow-up actions

- [ ] Deprecate nested provider model CRUD in favor of `/api/ai-models`
- [ ] Enrich Azure discovery when a stable deployments API contract is confirmed for configured API versions
- [ ] Optional streaming execution path once capability and gateway support land
