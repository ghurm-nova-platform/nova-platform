# CI/CD Guide — Sprint 0 Baseline

## Purpose

This guide documents the GitHub Actions continuous integration baseline for Nova Platform.
Deployment workflows are intentionally out of scope for Sprint 0.

## Security principles

- Workflows use least-privilege permissions (`contents: read` by default).
- Pull requests never receive repository secrets.
- `pull_request_target` is not used.
- Third-party actions are pinned to a stable major version.
- Dependency review runs on pull requests when GitHub Dependency graph is available.
- Internal Agent Runtime API keys must never appear in browser applications.
- Repository security guards run on every pull request and cover all code and infrastructure.

## Workflows

| Workflow file | Name | Trigger focus |
|---------------|------|---------------|
| `.github/workflows/ci-portal.yml` | CI Portal | `apps/portal/**` (path-filtered) |
| `.github/workflows/ci-platform-api.yml` | CI Platform API | `services/platform-api/**` (path-filtered) |
| `.github/workflows/ci-agent-runtime.yml` | CI Agent Runtime | `services/agent-runtime/**` (path-filtered) |
| `.github/workflows/ci-docs-security.yml` | CI Docs and Security | **every pull request** and foundation/main pushes |
| `.github/workflows/dependency-review.yml` | Dependency Review | every pull request |

Each module CI workflow also runs when its own workflow file changes, and supports `workflow_dispatch`.

### Mandatory module checks

Portal, Platform API, and Agent Runtime jobs are **mandatory** when their path filters match.
They do **not** use pre-checkout `hashFiles(...)` job guards. Required CI must never silently skip
because the workspace was empty before `actions/checkout`.

Only expensive module builds stay path-filtered. Repository security, documentation, and
dependency review run broadly so service and infrastructure changes cannot bypass guards.

### Concurrency

Superseded runs on the same branch or PR are cancelled via workflow `concurrency` groups.

### Caching

- Portal: npm cache via `actions/setup-node`
- Platform API: Maven cache via `actions/setup-java`
- Agent Runtime: Poetry virtualenv cache via `actions/cache`

Cache may accelerate installs but must not replace dependency reconciliation.
Agent Runtime always runs `poetry install --no-interaction --sync` after cache restore.

## Jobs and local equivalents

### Angular Portal

Working directory: `apps/portal`

CI steps:

1. Node.js 22
2. `npm ci`
3. `npm run lint`
4. `npm test -- --watch=false --browsers=ChromeHeadless`
5. `npm run build`

Local:

```bash
cd apps/portal
npm ci
npm run lint
npm test -- --watch=false --browsers=ChromeHeadless
npm run build
```

### Spring Boot Platform API

Working directory: `services/platform-api`

CI steps:

1. Java 21 (Temurin)
2. `./mvnw -B -DskipTests compile`
3. `./mvnw -B test`
4. `./mvnw -B -DskipTests package`

Local (Windows):

```powershell
cd services/platform-api
.\mvnw.cmd -B -DskipTests compile
.\mvnw.cmd -B test
.\mvnw.cmd -B -DskipTests package
```

### Python Agent Runtime

Working directory: `services/agent-runtime`

CI steps:

1. Python 3.12
2. Poetry 2.4.x
3. Restore cached `.venv` when available
4. `poetry install --no-interaction --sync` (always)
5. `poetry run ruff check src tests`
6. `poetry run mypy`
7. `poetry run pytest`

Local:

```bash
cd services/agent-runtime
poetry install --no-interaction --sync
poetry run ruff check src tests
poetry run mypy
poetry run pytest
```

### Documentation and repository checks

Runs on **every pull request**. Scope includes services, apps, infrastructure, docs,
GitHub workflows/scripts, and root configuration/Docker files.

CI steps:

1. Markdown lint (`markdownlint-cli2`)
2. Offline local Markdown link check (`lychee`)
3. `docker compose -f infrastructure/local/docker-compose.yml config`
4. Repository guards (`scripts/ci/check-repo-guards.sh`)

Local:

```bash
npm install --global markdownlint-cli2@0.17.2
markdownlint-cli2 "**/*.md" "#**/node_modules/**" "#**/dist/**" "#**/target/**" "#**/.venv/**"

# Link check (requires lychee): https://github.com/lycheeverse/lychee
lychee --offline --include-fragments ./README.md ./docs/**/*.md ./apps/*/README.md ./services/*/README.md ./infrastructure/**/*.md

docker compose -f infrastructure/local/docker-compose.yml config --quiet
bash scripts/ci/check-repo-guards.sh
```

Repository guards fail when:

- committed `.env` files (except `.env.example`) are present
- obvious private key PEM blocks are present
- browser code under `apps/` contains `INTERNAL_API_KEY` or `X-API-Key` outside unit tests

### Dependency review

Runs on every pull request using `actions/dependency-review-action@v4` and fails on high or
critical advisories. Requires GitHub Dependency graph / Advanced Security features where
supported by the organization plan.

## Dependabot

`.github/dependabot.yml` opens weekly update PRs for:

- GitHub Actions
- npm (`apps/portal`)
- Maven (`services/platform-api`)
- pip / Poetry (`services/agent-runtime`)

## Required merge checks (policy recommendation)

Until branch protection is configured in the repository settings, treat these as required for merge readiness:

1. Relevant module CI jobs for changed paths (mandatory; never skipped by workspace guards)
2. CI Docs and Security on every pull request
3. Dependency Review on every pull request

Recommended branch protection for `sprint-0/foundation` and `main`:

- Require a pull request before merging
- Require status checks to pass
- Do not allow bypass of required checks for routine contributors

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Portal tests hang | Browser not headless | Use `--browsers=ChromeHeadless` |
| Maven wrapper fails | Wrong Java version | Use JDK 21 and `JAVA_HOME` |
| Poetry mypy fails | Missing sync install | Run `poetry install --no-interaction --sync` |
| Markdown lint fails | Style rule violation | Fix markdown or adjust `.markdownlint.json` intentionally |
| Compose validation fails | Invalid YAML/interpolation | Fix `infrastructure/local/docker-compose.yml` |
| Repo guards fail on apps | API key pattern in browser source | Keep credentials server-side only |
| Dependency review skipped/fails | Org feature unavailable | Enable Dependency graph or adjust org settings |

## Adding a new application or service

1. Create the module under `apps/` or `services/` with a README and standard build/test commands.
2. Add a dedicated workflow under `.github/workflows/` with:
   - `permissions: contents: read`
   - path filters for the module and workflow file
   - concurrency cancellation
   - dependency caching
   - **no** pre-checkout `hashFiles` job skip guards
3. Keep repository security on every PR (do not path-filter the guard away for the new module).
4. Add Dependabot ecosystem coverage when applicable.
5. Document local commands in this guide and the module README.
6. Open a PR and confirm the new job runs for path changes while docs/security still runs on every PR.

## Out of scope

- Deployment, release, or environment promotion workflows
- Publishing artifacts to external registries
- Production secret injection into pull request jobs
