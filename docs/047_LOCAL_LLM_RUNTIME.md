# Local LLM Runtime

Sprint 6 Phase 5 — org-scoped local model inference runtime on Platform API. Agents, portal clients, and other modules **must** call inference only through `LLMGateway` / `/api/llm`. Direct HTTP to Ollama, llama.cpp, or vLLM from other packages is forbidden.

## Boundary

```text
Portal / REST clients / internal modules
  → Platform API
      → Llm*Controller (/api/llm/*)
      → InferenceService / ModelRegistryService / PromptTemplateService / …
      → LLMGateway (LLMGatewayService)   ← sole inference entry
          → LlmProviderRegistry
              → DeterministicLlmProvider (default / fallback)
              → OllamaLlmProvider
              → LlamaCppLlmProvider
              → VllmLlmProvider
      → Audit Center (AuditSource.LLM_RUNTIME)
      → Micrometer metrics (nova.llm.*)
      → PostgreSQL (V59 llm_* tables)
```

The browser never talks to local model servers. Other Platform API modules must inject `LLMGateway` (or call `/api/llm`) — never construct Ollama/vLLM clients themselves.

## Gateway-only rule

1. **All completions** go through `LLMGateway.complete` / `completeByModelCode`.
2. Controllers and agent code must not open sockets to `127.0.0.1:11434` (Ollama), llama.cpp, or vLLM base URLs.
3. Provider adapters live under `ai.nova.platform.llm.provider.*` and are registered in `LlmProviderRegistry`.
4. When a remote provider fails and `nova.llm.fallback-to-deterministic=true`, the gateway falls back to `DeterministicLlmProvider`.

## Providers

| Type | Role |
|------|------|
| `DETERMINISTIC` | In-process stub for tests and safe default |
| `OLLAMA` | OpenAI-compatible chat against Ollama (`nova.llm.ollama.*`) |
| `LLAMA_CPP` | llama.cpp server (`nova.llm.llamacpp.*`) |
| `VLLM` | vLLM OpenAI-compatible server (`nova.llm.vllm.*`) |
| Cloud enums (`OPENAI`, `AZURE_OPENAI`, …) | Reserved in schema; cloud traffic remains on Model Gateway (ADR-0009) |

## Capabilities

1. **Model registry** — register, update, enable/disable, delete local models
2. **Lifecycle** — download, install, load, unload, start, stop, restart, warmup, per-model health
3. **Inference** — chat, text completions, batch, SSE stream, cancel token
4. **Conversations** — create, list, message history, append, summarize
5. **Prompt templates** — CRUD and variable render
6. **Provider health** — list + on-demand health check
7. **Runtime config** — feature flags summary + key/value runtime entries
8. **Metrics & audit** — Micrometer counters/timers and Audit Center events

## Package

`ai.nova.platform.llm`

| Component | Role |
|-----------|------|
| `LlmController` | Config, health, metrics, runtime-config |
| `LlmModelController` | Model CRUD + lifecycle |
| `LlmInferenceController` | Chat / completions / batch / stream / cancel |
| `LlmConversationController` | Conversations and messages |
| `LlmPromptController` | Prompt templates |
| `LlmProviderController` | Provider status |
| `LLMGateway` / `LLMGatewayService` | Sole completion gateway |
| `InferenceService` | Chat/text orchestration |
| `ModelRegistryService` / `ModelLifecycleService` | Registry and lifecycle |
| `PromptTemplateService` | Templates |
| `HealthCheckService` | Provider probes |
| `LlmAuditService` | Audit Center integration |
| `LlmMetrics` | Micrometer |

## REST API

| Method | Path | Permission |
|--------|------|------------|
| GET | `/api/llm/config` | `LLM_READ` |
| GET | `/api/llm/runtime-config` | `LLM_READ` |
| POST | `/api/llm/runtime-config` | `LLM_ADMIN` |
| GET | `/api/llm/health` | `LLM_READ` |
| GET | `/api/llm/metrics` | `LLM_READ` |
| GET | `/api/llm/providers` | `LLM_READ` |
| POST | `/api/llm/providers/health` | `LLM_READ` |
| GET | `/api/llm/models` | `LLM_READ` |
| GET | `/api/llm/models/{id}` | `LLM_READ` |
| POST | `/api/llm/models` | `LLM_MODEL_ADMIN` |
| PUT | `/api/llm/models/{id}` | `LLM_MODEL_ADMIN` |
| DELETE | `/api/llm/models/{id}` | `LLM_MODEL_ADMIN` |
| POST | `/api/llm/models/{id}/enable` | `LLM_MODEL_ADMIN` |
| POST | `/api/llm/models/{id}/disable` | `LLM_MODEL_ADMIN` |
| POST | `/api/llm/models/{id}/install` | `LLM_MODEL_ADMIN` |
| POST | `/api/llm/models/{id}/download` | `LLM_MODEL_ADMIN` |
| POST | `/api/llm/models/{id}/load` | `LLM_MODEL_ADMIN` |
| POST | `/api/llm/models/{id}/unload` | `LLM_MODEL_ADMIN` |
| POST | `/api/llm/models/{id}/start` | `LLM_MODEL_ADMIN` |
| POST | `/api/llm/models/{id}/stop` | `LLM_MODEL_ADMIN` |
| POST | `/api/llm/models/{id}/restart` | `LLM_MODEL_ADMIN` |
| POST | `/api/llm/models/{id}/warmup` | `LLM_MODEL_ADMIN` |
| GET | `/api/llm/models/{id}/health` | `LLM_READ` |
| POST | `/api/llm/chat` | `LLM_INFER` |
| POST | `/api/llm/completions` | `LLM_INFER` |
| POST | `/api/llm/batch` | `LLM_INFER` |
| POST | `/api/llm/chat/stream` | `LLM_INFER` |
| POST | `/api/llm/cancel/{token}` | `LLM_INFER` |
| GET | `/api/llm/conversations` | `LLM_READ` |
| POST | `/api/llm/conversations` | `LLM_INFER` |
| GET | `/api/llm/conversations/{id}` | `LLM_READ` |
| GET | `/api/llm/conversations/{id}/messages` | `LLM_READ` |
| POST | `/api/llm/conversations/{id}/messages` | `LLM_INFER` |
| POST | `/api/llm/conversations/{id}/summary` | `LLM_INFER` |
| GET | `/api/llm/prompts` | `LLM_READ` |
| GET | `/api/llm/prompts/{id}` | `LLM_READ` |
| POST | `/api/llm/prompts` | `LLM_PROMPT_ADMIN` |
| PUT | `/api/llm/prompts/{id}` | `LLM_PROMPT_ADMIN` |
| DELETE | `/api/llm/prompts/{id}` | `LLM_PROMPT_ADMIN` |
| POST | `/api/llm/prompts/{id}/render` | `LLM_READ` |

OpenAPI: [llm-openapi.yaml](openapi/llm-openapi.yaml).

## Permissions

| Code | Use |
|------|-----|
| `LLM_READ` | Config, health, metrics, models, prompts, conversations (read) |
| `LLM_INFER` | Chat, completions, stream, cancel, conversation writes |
| `LLM_ADMIN` | Runtime config writes |
| `LLM_MODEL_ADMIN` | Model registry and lifecycle |
| `LLM_PROMPT_ADMIN` | Prompt template mutations |

`ORG_ADMIN` grants all of the above.

## Configuration

`nova.llm` in `application.yml`:

| Key | Default | Description |
|-----|---------|-------------|
| `enabled` | `true` | Master switch for local runtime |
| `default-provider` | `DETERMINISTIC` | Default provider type |
| `fallback-to-deterministic` | `true` | Fallback on provider failure |
| `timeout.seconds` | `60` | Completion timeout |
| `retry.max-attempts` | `2` | Gateway retries |
| `retry.backoff-ms` | `200` | Retry backoff |
| `cache.enabled` | `true` | Response cache |
| `cache.ttl-seconds` | `300` | Cache TTL |
| `scheduler.enabled` | `true` | Inference scheduler |
| `scheduler.worker-count` | `2` | Worker threads |
| `scheduler.queue-capacity` | `64` | Queue size |
| `ollama.enabled` / `base-url` | `false` / `http://127.0.0.1:11434` | Ollama adapter |
| `llamacpp.enabled` / `base-url` | `false` / `http://127.0.0.1:8080` | llama.cpp adapter |
| `vllm.enabled` / `base-url` | `false` / `http://127.0.0.1:8000` | vLLM adapter |

## Metrics

| Meter | Type | Meaning |
|-------|------|---------|
| `nova.llm.requests` | counter | Completion attempts |
| `nova.llm.successes` | counter | Successful completions |
| `nova.llm.failures` | counter | Failed completions |
| `nova.llm.fallbacks` | counter | Deterministic fallbacks |
| `nova.llm.latency` | timer | Completion latency |

`GET /api/llm/metrics` returns a summary map for the portal dashboard.

## Audit

- `AuditSource.LLM_RUNTIME`
- `AuditEntityType.LLM_RUNTIME`
- Lifecycle and admin actions recorded via `LlmAuditService` into Audit Center

## Schema

Flyway **V59** (`V59__local_llm_runtime.sql`) creates `llm_models`, `llm_model_versions`, `llm_prompt_templates`, `llm_conversations`, `llm_messages`, `llm_provider_status`, `llm_runtime_config`, `llm_usage_metrics`, and extends audit source/entity checks with `LLM_RUNTIME`.

## Portal

Angular feature at `/llm` (`apps/portal/src/app/features/llm/`):

- Dashboard — provider health + metrics
- Models — list + lifecycle actions
- Chat — conversations + `/api/llm/chat`
- Prompts — template list
- Configuration — enabled flags from `/api/llm/config`

## Constraints

- **No direct Ollama / llama.cpp / vLLM** from Planner, Coding, Review, Knowledge, or any module outside `ai.nova.platform.llm.provider`
- Cloud Model Gateway (ADR-0009) remains separate; Local LLM Runtime is for self-hosted local inference
- Deterministic provider is always available for CI and disabled-provider environments
- Streaming uses SSE (`text/event-stream`); non-stream chat returns `CompletionResponse` JSON

## References

- [ADR-0036: Local LLM Runtime](adr/ADR-0036-local-llm-runtime.md)
- [OpenAPI](openapi/llm-openapi.yaml)
- [AI Model Gateway](019_AI_MODEL_GATEWAY.md)
- [ADR-0009: Provider-neutral model gateway](adr/ADR-0009-provider-neutral-model-gateway.md)
