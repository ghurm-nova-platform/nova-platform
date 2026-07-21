# Automated Pull Request Review Engine

Sprint 6 Phase 3 — org-scoped heuristic PR analysis producing findings, risk scores, and recommendations only. The engine **never** auto-merges, auto-commits, auto-pushes, auto-fixes, auto-approves, or writes to GitHub. It does not invoke LLM providers, embeddings, or vector search.

## Boundary

```text
Portal / REST clients
  → Platform API
      → PullRequestReviewController
      → PullRequestReviewService (run, list, history, export)
      → ReviewAggregator
          → ArchitectureReviewService
          → SecurityReviewService
          → PerformanceReviewService
          → QualityReviewService
          → TestingReviewService
          → DocumentationReviewService
          → DatabaseReviewService
          → ApiReviewService
          → InfrastructureReviewService
      → ReviewKnowledgeService → KnowledgeMemoryService (reuse; no duplicate storage)
      → ReviewRiskScoreService
      → ReviewRecommendationService
      → Audit Center (AuditSource.PR_REVIEW)
      → Micrometer metrics (nova.pr_review.*)
```

The browser and API clients supply diff content and PR metadata; the engine never fetches remotes, mutates Git state, or posts GitHub review decisions.

## Capabilities

1. **Heuristic analysis** across architecture, security, performance, code quality, testing, documentation, database, API design, and infrastructure
2. **Findings** with category, severity, title, description, recommendation, file path, line hint, rule code, and evidence excerpt
3. **Knowledge linkage** — keyword overlap against Knowledge Engine memory documents (ADR, decisions, bugs, PR reviews, releases, best practices, runbooks); references stored as document IDs only
4. **Risk scoring** — six category scores (0–100), overall score, and risk score (`100 - overallScore`)
5. **Review result** — `APPROVED`, `APPROVED_WITH_SUGGESTIONS`, `REQUEST_CHANGES`, or `REJECTED` from severities and overall score
6. **Recommendations** derived from findings with priority (`HIGH`, `MEDIUM`, `LOW`)
7. **Run history** per project and pull request
8. **Export** — markdown, JSON, and PDF
9. **Audit integration** — run lifecycle events via Audit Center
10. **Portal UI** at `/pr-review` with runs, findings, recommendations, risk, and knowledge references

## Package

`ai.nova.platform.prreview`

| Component | Role |
|-----------|------|
| `PullRequestReviewController` | REST API under `/api/pr-review` |
| `PullRequestReviewService` | Orchestrates runs, persistence, export, audit |
| `ReviewAggregator` | Runs category analyzers, attaches knowledge, computes scores and result |
| `ArchitectureReviewService` | Layering, coupling, and structural heuristics |
| `SecurityReviewService` | Secrets, injection, auth, and unsafe-pattern checks |
| `PerformanceReviewService` | Hot-path and resource-use heuristics |
| `QualityReviewService` | Maintainability, complexity, and style heuristics |
| `TestingReviewService` | Test coverage and test-file change signals |
| `DocumentationReviewService` | Docs and comment change signals |
| `DatabaseReviewService` | Migration and schema-change heuristics |
| `ApiReviewService` | API contract and endpoint-change heuristics |
| `InfrastructureReviewService` | Config, deployment, and infra file heuristics |
| `ReviewKnowledgeService` | Attaches Knowledge Engine document IDs to findings |
| `ReviewRiskScoreService` | Category, overall, and risk score computation |
| `ReviewRecommendationService` | Builds prioritized recommendations from findings |

## Finding categories

`Architecture`, `Security`, `Performance`, `CodeQuality`, `Testing`, `Maintainability`, `Documentation`, `Database`, `ApiDesign`, `Frontend`, `Backend`, `Infrastructure`

Category analyzers map findings into six score buckets: Architecture, Security, Performance, Quality (includes CodeQuality, Maintainability, Database, ApiDesign, Frontend, Backend, Infrastructure), Testing, and Documentation.

## Severities

| Severity | Score penalty |
|----------|---------------|
| `INFO` | 1 |
| `SUGGESTION` | 3 |
| `WARNING` | 10 |
| `ERROR` | 20 |
| `BLOCKER` | 40 |

Each score bucket starts at 100; penalties are subtracted per finding (minimum 0).

## Review results

| Result | Condition |
|--------|-----------|
| `REJECTED` | Any `BLOCKER` finding |
| `REQUEST_CHANGES` | Any `ERROR` finding, or overall score &lt; 50 |
| `APPROVED_WITH_SUGGESTIONS` | Any `WARNING` or `SUGGESTION`, otherwise passing |
| `APPROVED` | No blockers, errors, warnings, or suggestions |

## Risk scoring

- **Category scores** (0–100 each): Architecture, Security, Performance, Quality, Testing, Documentation
- **Overall score** — arithmetic mean of the six category scores
- **Risk score** — `100 - overallScore` (0 = lowest risk, 100 = highest)
- Scores and result are persisted on `pr_review_runs`

## Knowledge integration

`ReviewKnowledgeService` calls `KnowledgeMemoryService.getRelevantDocuments` with types:

`ADR`, `DECISION`, `BUG`, `FIX`, `PULL_REQUEST`, `RELEASE`, `BEST_PRACTICE`, `RUNBOOK`

Matching uses keyword token overlap between finding text and document title/summary. Matched document IDs are stored on findings and recommendations as JSON references — **no duplicate document storage** in PR review tables.

Requires `KNOWLEDGE_READ` (or `ORG_ADMIN`) for knowledge attachment; review still completes without knowledge links when read access is unavailable.

## API

| Method | Path | Permission |
|--------|------|------------|
| GET | `/api/pr-review/config` | `PR_REVIEW_READ` |
| GET | `/api/pr-review` | `PR_REVIEW_READ` |
| GET | `/api/pr-review/history` | `PR_REVIEW_READ` |
| GET | `/api/pr-review/{id}` | `PR_REVIEW_READ` |
| GET | `/api/pr-review/{id}/findings` | `PR_REVIEW_READ` |
| GET | `/api/pr-review/{id}/recommendations` | `PR_REVIEW_READ` |
| GET | `/api/pr-review/{id}/risk-score` | `PR_REVIEW_READ` |
| GET | `/api/pr-review/{id}/knowledge` | `PR_REVIEW_READ` |
| GET | `/api/pr-review/{id}/export` | `PR_REVIEW_READ` |
| POST | `/api/pr-review/{id}/export` | `PR_REVIEW_READ` |
| POST | `/api/pr-review/run` | `PR_REVIEW_RUN` |
| POST | `/api/pr-review/{id}/rerun` | `PR_REVIEW_RUN` |

### Run request body

| Field | Required | Description |
|-------|----------|-------------|
| `projectId` | Yes | Project scope |
| `diffContent` | Yes | Unified diff or patch excerpt (capped) |
| `pullRequestOperationId` | No | Link to existing pull request operation |
| `pullRequestNumber` | No | PR number for history grouping |
| `pullRequestTitle` | No | Display title |
| `repositoryRef` | No | Repository identifier |
| `sourceBranch` / `targetBranch` | No | Branch names |
| `commitSha` | No | Head commit |
| `changedFiles` | No | List of changed file paths |

### History query parameters

| Parameter | Description |
|-----------|-------------|
| `projectId` | Filter by project |
| `pullRequestOperationId` | Filter by pull request operation |
| `pullRequestNumber` | Filter by PR number |

### Export

`GET` or `POST /api/pr-review/{id}/export?format=` supports `markdown` (default), `json`, and `pdf`.

## Configuration

```yaml
nova:
  pr-review:
    enabled: true
    max-diff-characters: 200000
    default-limit: 50
    max-findings: 200
    max-recommendations: 100
    parallel-analysis: true
    export-enabled: true
    cache:
      enabled: true
      ttl-seconds: 60
```

| Property | Default | Description |
|----------|---------|-------------|
| `enabled` | `true` | Master switch; disabled returns `503 PR_REVIEW_DISABLED` |
| `max-diff-characters` | `200000` | Maximum diff size accepted per run |
| `default-limit` | `50` | Default list/history page size |
| `max-findings` | `200` | Cap findings persisted per run |
| `max-recommendations` | `100` | Cap recommendations per run |
| `parallel-analysis` | `true` | Run category analyzers concurrently |
| `export-enabled` | `true` | Allow export endpoints |
| `cache.enabled` | `true` | In-process response cache flag |
| `cache.ttl-seconds` | `60` | Cache TTL |

## Permissions

| Permission | Purpose |
|------------|---------|
| `PR_REVIEW_READ` | View runs, findings, recommendations, risk scores, knowledge links, and export |
| `PR_REVIEW_RUN` | Execute and rerun PR review analysis |
| `PR_REVIEW_ADMIN` | Administer PR review configuration and runs |

`ORG_ADMIN` bypasses permission checks.

## Database

Flyway `V57__pr_review_engine.sql` creates:

- `pr_review_runs` — run metadata, status, result, overall/risk/category scores, diff excerpt
- `pr_review_findings` — category findings with severity, rule code, evidence, knowledge document IDs
- `pr_review_recommendations` — prioritized recommendations linked to findings

Audit constraints extended for `AuditSource.PR_REVIEW` and `AuditEntityType.PR_REVIEW`.

Run status values: `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`.

## Portal

Angular route `/pr-review` — dashboard listing review runs with detail views for findings, recommendations, risk scores, knowledge references, and export actions. Requires `PR_REVIEW_READ` to view and `PR_REVIEW_RUN` to start analysis.

## Audit

Review run lifecycle events publish to Audit Center with `AuditSource.PR_REVIEW` and `AuditEntityType.PR_REVIEW`.

## Constraints and non-goals

- **No LLM providers** — rule-based heuristics only; no model gateway invocation
- **No auto-merge, commit, push, fix, or GitHub approve** — recommendations and reports only
- **No GitHub write actions** — no posting review comments or status checks to remotes
- **No embeddings or vector search** — knowledge linkage uses keyword overlap via Knowledge Engine memory surfacing
- **No duplicate knowledge storage** — references Knowledge Engine document IDs only
- **No remote Git fetch** — client must supply diff content and metadata
- **No WebSockets, Kafka, RabbitMQ, or Redis**
- In-process cache only; no cross-instance invalidation

## Relationship to other agents

| Concern | PR Review Engine (045) | Review Agent (015) |
|---------|------------------------|-------------------|
| Input | Supplied PR diff and metadata | Generated artifacts from Coding Agent |
| Output | Findings, scores, recommendations | Artifact evaluation findings |
| Git actions | None | None |
| Knowledge | Links to Knowledge Engine memory | Not integrated in this phase |

See [ADR-0034](adr/ADR-0034-pr-review-engine.md) and [Knowledge Engine](044_KNOWLEDGE_ENGINE.md).
