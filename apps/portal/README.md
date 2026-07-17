# Nova Portal

Angular 20 administration portal for organizations, projects, agents, feedback, policies, usage, and settings.

## Sprint 0 scope

- Application shell and navigation
- Arabic RTL and English LTR readiness
- Light and dark theme foundations
- Routes for Dashboard, Projects, Agents, Feedback, and Settings
- HTTP client foundation with correlation ID (Platform API / BFF only)
- No production business workflows yet

## Architecture

```text
Browser (Nova Portal)
  → Platform API / BFF
      → Agent Runtime (server-to-server, internal API key)
```

- The browser calls **only** the Platform API / BFF.
- Future browser auth will use a user session or OAuth/OIDC access token.
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

## Production build

```bash
npm run build
```

Output: `dist/portal`

## Docker

```bash
docker build -t nova-portal .
docker run --rm -p 8080:80 nova-portal
```

## Ownership

Frontend Platform Team
