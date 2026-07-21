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
- Prompt Management (project-scoped reusable prompts and immutable published versions)
- Agent Execution (Platform API orchestration with replaceable AgentRuntimeClient)
- Conversation Memory (bounded durable history assembled by Platform API)
- Tool Registry (allowlisted executors orchestrated by Platform API)
- Knowledge Bases and RAG (allowlisted embeddings + vector store owned by Platform API)
- AI Model Gateway (allowlisted providers, routing, usage owned by Platform API)
- Model Catalog (capabilities, aliases, provider sync owned by Platform API)
- Multi-agent orchestration foundation, Planner Agent (plan → draft run import), Coding Agent (task → generated artifacts), Review Agent (artifacts → findings), Testing Agent (artifacts → test plans; no execution), Patch Agent (approved artifacts → Unified Diff; no git apply), Git Integration Agent (validated patch → isolated branch + commit; no merge/push), Pull Request Agent (successful git branch → remote push + PR; no merge/approve), CI Observation Agent (successful PR → workflow health summary; read-only, no rerun/merge/deploy), Repair Agent (failure signals → new PatchResult; no overwrite/merge/CI trigger), Approval Gate (trusted evidence → governance decision + human approvals; no merge/deploy), Merge Agent (approved decision → provider merge + verification; no deploy/CI rerun), Release Manager (merged content → immutable release + manifest; no deploy), Deployment Observation (release-linked deployment state across environments; observe-only, no deploy/rollback), Deployment Execution Engine (validated queue/start via pluggable providers; no release mutation / auto rollback / canary), Rollback Manager (release/deployment-linked rollback plans; planning/validation only, no execution), Release Policies (deterministic release evaluation by reference; no upstream mutation), Environment Management (project-scoped environment metadata; extends global catalog; no deploy/secrets), and Enterprise Audit Center (append-only cross-domain audit trail; read-only APIs; never mutates business data)
- Multi-agent orchestration foundation (durable runs, graphs, claim/execute)
- Provider Secret Vault (AES-256-GCM credentials; OpenAI / Azure OpenAI adapters)
- Agent Runtime
- Workflows and Events
- Tools and Sandboxes
- Models and Usage
- Knowledge and Memory
- Feedback Intelligence
- Audit and Compliance — Enterprise Audit Center publishes lifecycle events from orchestration, all pipeline agents, approval/merge/release managers, environments, policies, deployments, rollbacks, and security/REST capture; V51 database triggers enforce immutability on event rows
- Enterprise Dashboard — read-only org-scoped aggregation across pipeline, releases, deployments, environments, audit, approvals, CI, and rollbacks; in-memory TTL cache and HTTP polling only (V54 permissions)
- Marketplace, deferred until after beta

## 5. Agent execution lifecycle

```text
Request received
→ policy evaluation
→ context retrieval (assigned knowledge bases)
→ model routing
→ provider invocation via gateway
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
- The browser must never call Agent Runtime, embedding providers, vector stores, or AI model providers.
- The browser must never carry internal service API keys or provider credentials.
- Platform API JWTs carry `userId`, `organizationId`, and `roles` for RBAC.
- Organization and project APIs are scoped by JWT organization membership and role codes
  (`ORG_ADMIN`, `PROJECT_ADMIN`, `USER`).
- Knowledge retrieval is tenant-scoped; raw embeddings and document content are not exposed in list APIs or INFO/WARN logs.
- Model invocations store counts and safe error codes only — never prompts, completions, or secrets.
- Provider secrets are stored as AES-256-GCM ciphertext; plaintext is accepted only at create/rotate and never returned again.
- Provider HTTP calls use allowlisted hosts only (no arbitrary URLs from the browser).

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
