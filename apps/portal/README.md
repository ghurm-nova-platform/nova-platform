# Nova Portal

Angular 20 administration portal for organizations, projects, agents, feedback, policies, usage, and settings.

## Sprint 2 Phase 1 scope

- Orchestration runs under `/orchestration-runs`
- Create / edit DRAFT runs, list filters, detail with progress and events
- Practical list-based task graph builder (`/orchestration-runs/:runId/graph`)
- Ready / start / cancel / archive lifecycle actions
- Typed OrchestrationRunService and ORCHESTRATION_* permission helper (Platform API only)
- No heavy graph visualization libraries

## Sprint 3 Phase 4 scope

- CI Observation Agent under `/ci`
- Observe GitHub Actions workflow runs for successful PR operations; read-only — no rerun/merge/deploy
- Overall CI badge, workflow/job/step hierarchy, durations, failed jobs/steps, failure summary, retry recommendation
- GitHub Actions links open in new tab (`noopener noreferrer`)
- CI_* permission helper (Platform API only)
- Safety statement: observes CI only; never reruns, approves, merges, or deploys

## Sprint 3 Phase 3 scope

- Pull Request Agent under `/pull-requests`
- Publish successful Git Integration branch to configured remote; create PR; no merge/approve/force-push
- Status badge for all PR lifecycle states, provider, repository, branches, hashes, PR link, timeline, error code
- PR_* permission helper (Platform API only)
- Safety statement: creates PRs but never approves or merges them

## Sprint 3 Phase 2 scope

- Git Integration under `/git`
- Apply validated patch onto isolated `ai/task-{taskId}` branch; one commit; no merge/push/delete
- Branch, commit hash, patch hash, status badge, timeline, copy actions
- GIT_* permission helper (Platform API only)

## Sprint 3 Phase 1 scope

- Patch Agent under `/patch`
- Generate / load latest Unified Diff for an approved orchestration task
- Summary, statistics, changed files, validation badge, download `.patch`
- Unified Diff viewer (no Monaco); PATCH_* permission helper (Platform API only)
- Does not apply patches or invoke git

## Sprint 2 Phase 5 scope

- Testing Agent under `/testing`
- Generate / load latest test plans for an orchestration task with generated artifacts
- Coverage gauge (green/yellow/red), suites, cases, priority badges
- Type / priority filters and search; expandable suite details
- TESTING_* permission helper (Platform API only)
- Does not execute tests

## Sprint 2 Phase 4 scope

- Review Agent under `/review`
- Run / load latest review for an orchestration task with generated artifacts
- Score badge (green/yellow/red), approval badge, severity counts
- Finding filters by severity, category, artifact, and search text
- REVIEW_* permission helper (Platform API only)

## Sprint 2 Phase 3 scope

- Coding Agent under `/coding`
- Generate / load artifacts for an orchestration task
- Search and filter by filename, language, artifact type
- Lightweight diff preview (no Monaco / VS Code), copy, download
- CODING_* permission helper (Platform API only)

## Sprint 2 Phase 2 scope

- AI Planner under `/planner`
- Generate plan, DAG preview (zoom/pan), estimates, edit JSON, create draft run
- PLANNER_* permission helper (Platform API only)

## Sprint 1 Phase 11 scope

- Organization AI model catalog under `/ai-models`
- Capabilities and aliases management
- Provider model sync from provider detail
- Catalog RBAC (`MODEL_CATALOG_*`, `MODEL_ALIAS_MANAGE`, `MODEL_CAPABILITY_MANAGE`)

## Sprint 1 Phase 10 scope

- Provider secret metadata under organization credential vault routes
- Create / rotate / revoke flows (plaintext only at submit; never re-displayed or stored in browser)
- Provider connection testing UI (status + safe error code only)
- OpenAI / Azure OpenAI provider configuration (allowlisted endpoint profiles)
- Credential references `vault:provider-secret:<uuid>` or `env:NOVA_PROVIDER_*` only

## Sprint 1 Phase 9 scope

- Organization model providers under `/model-providers`
- Model catalog under provider routes
- Project models, agent primary/fallback assignments, routing policies, usage dashboard
- Playground selected provider/model, fallback and attempt indicators
- Credential **references** only (never raw API keys; never store secrets in browser storage)
- Typed model-gateway services and permission helper (Platform API only)

## Sprint 1 Phase 8 scope

- Project knowledge bases under `/projects/:projectId/knowledge-bases`
- Document upload (TEXT / MARKDOWN), chunk metadata, reprocess/archive
- Agent knowledge assignment page
- Playground citations and retrieval indicator
- Typed KnowledgeBaseService and permission helper (Platform API only)
- No browser-side embeddings, vector search, or document content in local/session storage

## Sprint 1 Phase 7 scope

- Project tools under `/projects/:projectId/tools`
- Agent tool assignment page
- Execution tool-call timeline with approve/reject/continue
- Playground assigned-tools and approval states
- Typed ToolService and permission helper (Platform API only)

## Sprint 1 Phase 6 scope

- Project-scoped conversations under `/projects/:projectId/conversations`
- Conversation detail timeline and composer
- Agent playground Stateless vs Conversation modes
- Typed ConversationService and permission helper (Platform API only)

## Sprint 1 Phase 5 scope

- Agent playground under `/projects/:projectId/agents/:agentId/playground`
- Execute via Platform API only (NoOp runtime server-side)
- Execution history, token/latency display, cancel action
- Typed ExecutionService and permission helper

## Sprint 1 Phase 4 scope

- Project-scoped prompt routes under `/projects/:projectId/prompts`
- Prompt list/detail/create/edit, version history, compare, preview
- Typed PromptService and permission helper (Platform API only)
- Material textarea editor (Monaco Editor deferred)

## Sprint 1 Phase 3 scope

- Project-scoped agent routes under `/projects/:projectId/agents`
- Agent list/detail/create/edit with status actions
- Typed AgentService and permission helper (Platform API only)

## Sprint 1 Phase 2 scope

- Organizations page (table, search, pagination, sorting, dialogs)
- Projects page (status badges, archive confirmation, CRUD dialogs)
- Typed OrganizationService and ProjectService (Platform API only)

## Sprint 1 Phase 1 scope

- Login page and route guard
- JWT access token interceptor (Platform API only)
- User session service (`sessionStorage`, no passwords)
- Shell sign-out against `/api/auth/logout`

## Sprint 0 scope

- Application shell and navigation
- Arabic RTL and English LTR readiness
- Light and dark theme foundations
- Routes for Dashboard, Projects, Agents, Feedback, and Settings
- HTTP client foundation with correlation ID (Platform API / BFF only)

## Architecture

```text
Browser (Nova Portal)
  → Platform API / BFF (JWT user auth)
      → Agent Runtime (server-to-server, internal API key)
```

- The browser calls **only** the Platform API / BFF.
- User auth uses Bearer access tokens from Platform API.
- Platform API attaches the internal Agent Runtime API key on server-to-server calls.
- The browser must never load or send internal service credentials.

## Stack

- Angular 20 (standalone components, strict TypeScript)
- Angular Material
- Lazy-loaded feature routes
- ESLint + Prettier

## Prerequisites

- Node.js 22+
- npm 10+

## Configuration

Build-time defaults live in `src/environments/`.

Runtime overrides load from `public/runtime-config.json`:

```json
{
  "platformApiUrl": "http://localhost:8080"
}
```

Do not put API keys or Agent Runtime URLs in browser configuration.

## Auth package layout

```text
src/app/auth/
  login/
  guards/
  interceptors/
  services/
```

Local demo credentials (Platform API Flyway seed):

- `admin@nova.local` / `ChangeMe123!`

## Install

```bash
cd apps/portal
npm install
```

## Run

```bash
npm start
```

Open http://localhost:4200

## Test

```bash
npm test -- --watch=false --browsers=ChromeHeadless
```

## Lint and format

```bash
npm run lint
npm run format
```

## Build

```bash
npm run build
```
