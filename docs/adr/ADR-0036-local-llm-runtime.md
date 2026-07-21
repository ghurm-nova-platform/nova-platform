# ADR-0036: Local LLM Runtime

## Status

Accepted

## Context

Sprint 6 Phase 4 delivered Enterprise Identity (ADR-0035). Operators and agents need **local** model inference (Ollama, llama.cpp, vLLM) without coupling every feature module to provider HTTP clients. Roadmap items “Local model support” and optional LLM-assisted flows require a single, auditable entry point on Platform API.

Existing AI Model Gateway (ADR-0009) targets cloud/provider-neutral routing with secrets vault. Local runtimes need:

- Org-scoped model registry and lifecycle (download/load/start/stop)
- Chat, completions, streaming, conversations, and prompt templates
- Health checks, Micrometer metrics, and Audit Center events
- **Gateway-only** rule so Planner/Coding/Review/Knowledge never call Ollama directly
- Safe default via deterministic provider for CI and fallback

## Decision

Implement `ai.nova.platform.llm` on Platform API as the **Local LLM Runtime**:

1. **Schema** — Flyway V59 creates `llm_*` tables and extends audit constraints with `LLM_RUNTIME`
2. **Gateway** — `LLMGateway` / `LLMGatewayService` is the only completion path; providers register via `LlmProviderRegistry`
3. **Providers** — `DeterministicLlmProvider` (default/fallback), `OllamaLlmProvider`, `LlamaCppLlmProvider`, `VllmLlmProvider`
4. **REST** — full surface under `/api/llm` (config, models, inference, conversations, prompts, providers)
5. **Permissions** — `LLM_READ`, `LLM_ADMIN`, `LLM_INFER`, `LLM_MODEL_ADMIN`, `LLM_PROMPT_ADMIN`
6. **Portal** — Angular `/llm` with dashboard, models, chat, prompts, configuration tabs
7. **Audit / metrics** — `AuditSource.LLM_RUNTIME`, meters `nova.llm.*`
8. **OpenAPI** — `docs/openapi/llm-openapi.yaml`

Out of scope for this phase: replacing cloud Model Gateway, fine-tuning pipelines, GPU orchestration clusters, and browser-direct WebGPU inference.

## Consequences

### Positive

- Single audited path for local inference across portal and agents
- Provider adapters isolated; other modules stay provider-agnostic
- Deterministic fallback keeps CI and degraded environments usable
- Operators gain lifecycle and health visibility in the portal

### Negative

- Dual AI surfaces (cloud Model Gateway vs local LLM runtime) until a later unification
- Local servers remain out-of-process; Platform API only proxies and schedules
- Streaming and long-running loads need careful timeout and cancel handling

### Alternatives considered

- **Call Ollama from each agent module** — rejected; violates isolation and auditability
- **Extend Model Gateway only** — rejected for this phase; local lifecycle and health differ from cloud routing/secrets
- **Browser → Ollama** — rejected; breaks Platform API boundary and auth model
- **Require GPU cluster orchestration first** — deferred; registry + HTTP providers sufficient for Phase 5

## References

- [Local LLM Runtime](../047_LOCAL_LLM_RUNTIME.md)
- [Local LLM OpenAPI](../openapi/llm-openapi.yaml)
- [ADR-0009: Provider-neutral model gateway](ADR-0009-provider-neutral-model-gateway.md)
- [ADR-0035: Enterprise Identity](ADR-0035-enterprise-identity.md)
