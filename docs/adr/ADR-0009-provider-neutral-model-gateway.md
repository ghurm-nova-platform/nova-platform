# ADR-0009: Provider-Neutral Model Gateway

## Status

Accepted (Sprint 1 Phase 9)

## Context

Agents need model invocation, but binding Platform API to a single vendor or allowing the browser to hold credentials is unsafe. Tool calling and RAG already depend on a provider-neutral `AgentRuntimeClient` contract. A gateway must select models, enforce limits, and record privacy-safe usage without opening DB transactions across network calls.

## Decision

1. Own providers, models, project availability, agent assignments, routing policies, invocations, and daily usage in Platform API (Flyway V20–V22).
2. Resolve providers only from Spring-managed `AiModelProvider` beans by `adapter_key`.
3. Store credential **references** only; resolve server-side via allowlisted environment names in this phase.
4. Ship `DeterministicLocalModelProvider` for tests/dev; keep reserved provider types inactive until adapters exist.
5. Route with FIXED_PRIMARY / PRIORITY_FALLBACK / CAPABILITY_BASED — no scripts or cost-based AI routing.
6. Invoke outside DB transactions; retry/fallback with distinct invocation rows; process-local concurrency.
7. Preserve NoOpAgentRuntimeClient; map gateway responses into the existing runtime turn contract.
8. Keep V19 execution knowledge snapshots for tool-approval continuation.

## Consequences

- Strong security boundary and clear upgrade path to real vendors.
- Deterministic local models are not production quality.
- Process-local concurrency is not multi-instance safe until a future distributed limiter exists.
