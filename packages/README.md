# Shared Packages

- `contracts`: versioned API and event schemas shared across services.
- `ui`: reusable, accessible, bilingual UI components and design tokens.
- `agent-sdk`: contracts and helpers for implementing Nova agents.
- `model-sdk`: provider-neutral model, embedding, and usage interfaces.

Packages must remain independently testable and must not depend on application-layer code.
