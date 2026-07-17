# Nova Portal

Angular 20 administration portal for organizations, projects, agents, feedback, policies, usage, and settings.

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
