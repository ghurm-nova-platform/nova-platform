# Product Roadmap

## Phase 0 — Foundation (Weeks 1–2)

- Approve project charter and architecture baseline
- Establish monorepo structure
- Add coding, security, and contribution standards
- Configure GitHub Actions baseline
- Define API and event contracts
- Create development environment with Docker Compose

**Exit criteria:** repository builds, tests, and validates documentation changes through CI.

## Phase 1 — Platform Skeleton (Weeks 3–6)

- Angular portal shell
- Spring Boot platform API
- PostgreSQL schema baseline
- Authentication foundation
- Organizations, workspaces, and projects
- Audit event storage
- Initial usage metering

**Exit criteria:** user can sign in, create an organization, workspace, and project.

## Phase 2 — Web IDE (Weeks 7–10)

- File explorer
- Monaco Editor
- Tabs and file persistence
- Project search
- Basic Git status and diff
- Terminal connection to isolated workspace
- Workflow timeline panel

**Exit criteria:** user can import a repository, edit files, run an approved command, and review changes.

## Phase 3 — Agent Runtime MVP (Weeks 11–14)

- Agent registry
- Model provider abstraction
- Tool gateway
- Workflow state machine
- Policy checks
- Read-only code search agent
- File-change agent restricted to branches
- Structured execution timeline

**Exit criteria:** an approved agent task can inspect a project and produce a validated branch diff.

## Phase 4 — Feedback Intelligence (Weeks 15–18)

- Feedback intake UI and API
- Classification agent
- Duplicate detection
- Priority scoring
- Routing workflows
- GitHub issue creation
- Draft fix workflow

**Exit criteria:** submitted feedback is classified, routed, linked to an issue, and can generate a reviewed draft patch.

## Phase 5 — Beta Hardening (Weeks 19–22)

- Security review
- Load and resilience testing
- Usage and cost controls
- Backup and recovery
- Product analytics
- Onboarding
- Ten-user closed beta

**Exit criteria:** Beta 0.1 is deployable with documented limitations, monitoring, rollback, and support procedures.

### Sprint 5 — Enterprise Dashboard (Phase 2)

- Read-only executive dashboard aggregating existing domain APIs
- Portal `/dashboard` with pipeline charts, tables, and 30s polling
- CSV/Excel/PDF export; in-memory cache with admin refresh

**Exit criteria:** Operators can view unified pipeline/release/deployment/audit signals without new operational tables or real-time infrastructure.

### Sprint 6 — Multi-Agent Collaboration (Phase 1)

- Collaboration sessions coordinating existing agents on shared orchestration runs
- Immutable messages, shared context, parallel participant groups, conflict detection
- Portal `/collaboration` with timeline, participants, tasks, and 10s HTTP polling
- Audit integration via `AuditSource.COLLABORATION`

**Exit criteria:** Multiple agents collaborate in one session with full timeline, human intervention, and audit trail without distributed messaging.

### Sprint 6 — Knowledge and Memory Engine (Phase 2)

- Structured knowledge documents with types, categories, tags, and visibility
- Keyword search and memory surfacing for agents; document relations and import/export
- REST API under `/api/knowledge`; audit via `AuditSource.KNOWLEDGE`
- Parallel to existing RAG stack; no embeddings, vector stores, or semantic retrieval

**Exit criteria:** Operators can store, search, relate, and export structured project knowledge via API with full audit trail, without embedding provider dependency.

### Sprint 6 — Automated PR Review Engine (Phase 3)

- Rule-based PR analysis across architecture, security, performance, quality, testing, documentation, database, API, and infrastructure (`ai.nova.platform.prreview`)
- Six category scores (0–100), overall score, and risk score (`100 - overallScore`); results `APPROVED`..`REJECTED`
- Findings and recommendations only — no auto-merge, commit, push, fix, approve, LLM, or GitHub writes
- Knowledge Engine reuse via `KnowledgeMemoryService` (ADR, decisions, bugs, PR reviews, releases, best practices, runbooks); no duplicate storage
- Flyway V57 tables: `pr_review_runs`, `pr_review_findings`, `pr_review_recommendations`
- Permissions `PR_REVIEW_READ`, `PR_REVIEW_RUN`, `PR_REVIEW_ADMIN`; audit via `AuditSource.PR_REVIEW`
- Portal `/pr-review`; REST `/api/pr-review`; export markdown, JSON, PDF

**Exit criteria:** Operators can run a PR review from supplied diff/metadata, inspect findings/risk/knowledge links, and export reports without any Git write actions.

### Sprint 6 — Enterprise Identity (Phase 4)

- Enterprise identity layer as sole auth entry wrapping existing JWT (ADR-0002)
- Provider registry: `LOCAL`, `SAML`, `OIDC`, `LDAP`; session management and admin revoke
- Login history audit trail; MFA enrollment (TOTP); SMS OTP / WebAuthn deferred
- Optional SCIM 2.0 user listing (`GET /api/scim/v2/Users`)
- Flyway V58 tables: `identity_providers`, `identity_sessions`, `identity_login_events`, `identity_mfa_enrollments`, `identity_password_policies`
- Permissions `IDENTITY_READ`, `IDENTITY_ADMIN`, `IDENTITY_PROVIDER_MANAGE`, `IDENTITY_MFA_MANAGE`, `SCIM_PROVISION`; audit via `AuditSource.IDENTITY`
- Portal `/identity`; REST `/api/identity`

**Exit criteria:** Operators can view providers, sessions, login history, and MFA status; admins can revoke sessions and enroll MFA without changing the JWT portal contract.

### Sprint 6 — Local LLM Runtime (Phase 5)

- Gateway-only local inference via `LLMGateway` (`ai.nova.platform.llm`); no direct Ollama/llama.cpp/vLLM from other modules
- Providers: `DETERMINISTIC` (default/fallback), `OLLAMA`, `LLAMA_CPP`, `VLLM`
- Model registry and lifecycle (download/install/load/start/stop/warmup); chat, completions, stream, conversations, prompt templates
- Flyway V59 tables: `llm_models`, `llm_conversations`, `llm_messages`, `llm_prompt_templates`, `llm_provider_status`, …
- Permissions `LLM_READ`, `LLM_ADMIN`, `LLM_INFER`, `LLM_MODEL_ADMIN`, `LLM_PROMPT_ADMIN`; audit via `AuditSource.LLM_RUNTIME`
- Portal `/llm`; REST `/api/llm`; metrics `nova.llm.*`

**Exit criteria:** Operators can inspect provider health, manage model lifecycle, chat through `/api/llm/chat`, and confirm other modules never bypass the gateway.

## After Beta

- LLM-assisted PR review (optional)
- SMS OTP for MFA (if required)
- Self-hosted deployment
- Marketplace foundations
- Desktop client
