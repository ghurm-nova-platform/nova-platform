# System Architecture — Initial Baseline

## 1. Architectural style

Nova Platform begins as a modular monorepo with clear domain boundaries. The first beta should avoid premature microservices. Services may be extracted later when scale, security isolation, ownership, or deployment independence justifies the operational cost.

## 2. Logical architecture

```text
Browser
  ├─ Administration Portal
  └─ Web IDE
          │
          ▼
API Gateway / Backend-for-Frontend
          │
  ┌───────┼─────────────────────────────────────────┐
  ▼       ▼               ▼             ▼           ▼
Identity Projects     Agent Runtime   Feedback    Audit/Usage
          │               │             │           │
          └─────────────── Event Bus ────────────────┘
                          │
                 Tool and Model Gateways
                          │
       ┌──────────────────┼───────────────────────┐
       ▼                  ▼                       ▼
 GitHub integrations  Sandbox workers       AI providers
```

## 3. Recommended implementation stack

### Frontend

- Angular 20
- TypeScript
- Angular Material
- Monaco Editor
- xterm.js
- NgRx Signals or a lightweight state layer where appropriate
- WebSocket or Server-Sent Events for workflow streaming

### Platform backend

- Java 21
- Spring Boot 3.x
- Spring Security
- PostgreSQL
- Redis
- RabbitMQ initially; evaluate Kafka only when throughput and retention needs justify it
- S3-compatible object storage for artifacts

### AI and agent services

- Python 3.12
- FastAPI
- Pydantic
- Provider-neutral model adapter
- Workflow implementation may start with explicit application code; introduce LangGraph or another framework only behind internal interfaces
- Qdrant for vector retrieval when semantic project search is introduced

### Infrastructure

- Docker Compose for local development
- Kubernetes after the beta requires multi-tenant scaling
- OpenTelemetry
- Prometheus and Grafana
- Centralized structured logs
- GitHub Actions

## 4. Domain boundaries

- Identity and Access
- Organizations and Teams
- Projects and Repositories
- Workspace and Web IDE
- Agent Runtime
- Workflows and Events
- Tools and Sandboxes
- Models and Usage
- Knowledge and Memory
- Feedback Intelligence
- Audit and Compliance
- Marketplace, deferred until after beta

## 5. Agent execution lifecycle

```text
Request received
→ policy evaluation
→ context retrieval
→ plan creation
→ approval gate when required
→ tool execution in sandbox
→ result validation
→ tests and checks
→ reviewer agent
→ human approval when required
→ branch and pull request
→ workflow completion event
```

## 6. Security boundaries

- Models never receive raw stored credentials.
- Tool calls use scoped, short-lived credentials.
- Terminal commands run in disposable isolated sandboxes.
- Repository writes occur on branches by default.
- Protected branches cannot be directly modified by agents.
- High-risk actions require explicit user approval.
- Every tool invocation records actor, agent, input summary, result, duration, and policy decision.
- Browser clients authenticate only to Platform API using user JWTs.
- The browser must never call Agent Runtime or carry internal service API keys.
- Platform API JWTs carry `userId`, `organizationId`, and `roles` for RBAC.

## 7. Data stores

- PostgreSQL: transactional product data and workflow state
- Redis: caching, locks, rate limits, ephemeral coordination
- Object storage: logs, patches, build artifacts, screenshots, and attachments
- Qdrant: embeddings and semantic indexes
- Search engine deferred until PostgreSQL search becomes insufficient

## 8. Repository layout

```text
/apps
  /portal
  /web-ide
/services
  /platform-api
  /agent-runtime
  /sandbox-worker
/packages
  /contracts
  /ui
  /agent-sdk
  /model-sdk
/infrastructure
/docs
```

## 9. Architecture rules

1. Domain modules communicate through published interfaces and events.
2. AI framework types must not leak into core domain contracts.
3. Model providers are adapters, not domain dependencies.
4. Every external action must pass through the policy and audit layers.
5. Workflows must be resumable and idempotent.
6. Agent outputs are treated as untrusted until validated.
7. New infrastructure dependencies require an ADR.
