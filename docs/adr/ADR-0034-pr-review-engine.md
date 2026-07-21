# ADR-0034: Automated Pull Request Review Engine

## Status

Accepted

## Context

Sprint 6 Phase 2 delivered the Knowledge and Memory Engine (ADR-0033). Operators need automated, audit-friendly pull request analysis before merge — without granting the platform write access to Git remotes or coupling review quality to LLM providers.

The existing Review Agent (ADR-0015) evaluates generated artifacts from the Coding Agent pipeline; it does not analyze arbitrary PR diffs supplied by operators. Requirements for this phase:

- Rule-based analysis across architecture, security, performance, quality, testing, documentation, database, API, and infrastructure
- Findings, risk scores, and recommendations only — no merge, commit, push, fix, or GitHub approve actions
- Knowledge linkage via Knowledge Engine memory surfacing (ADR, decisions, bugs, PR reviews, releases, best practices, runbooks) without duplicating document storage
- REST API, portal visibility, export (markdown, JSON, PDF), and Audit Center integration
- Permissions: `PR_REVIEW_READ`, `PR_REVIEW_RUN`, `PR_REVIEW_ADMIN`
- Explicit prohibition of LLM providers, embeddings, vector search, and GitHub write automation

## Decision

Implement `ai.nova.platform.prreview` on Platform API as a **recommendations-only PR review engine**:

1. **Schema** — Flyway V57 creates `pr_review_runs`, `pr_review_findings`, and `pr_review_recommendations`; extends audit constraints for `PR_REVIEW`
2. **PullRequestReviewController / PullRequestReviewService** — REST under `/api/pr-review`; run, list, history, export, audit
3. **ReviewAggregator** — orchestrates nine category analyzers (Architecture, Security, Performance, Quality, Testing, Documentation, Database, API, Infrastructure); optional parallel execution
4. **ReviewKnowledgeService** — attaches Knowledge Engine document IDs via `KnowledgeMemoryService`; no duplicate storage
5. **ReviewRiskScoreService** — six category scores (0–100), overall score, risk score (`100 - overallScore`)
6. **ReviewRecommendationService** — prioritized recommendations derived from findings
7. **Review result** — `APPROVED`, `APPROVED_WITH_SUGGESTIONS`, `REQUEST_CHANGES`, `REJECTED` from severities (`INFO`..`BLOCKER`) and overall score
8. **Authorization** — `PR_REVIEW_READ`, `PR_REVIEW_RUN`, `PR_REVIEW_ADMIN`
9. **Portal** — Angular `/pr-review` route
10. **Audit** — `AuditSource.PR_REVIEW`, `AuditEntityType.PR_REVIEW`
11. **Metrics** — Micrometer counters/timers under `nova.pr_review.*`

Out of scope: LLM providers, embeddings/vector search, auto-merge/commit/push/fix, GitHub write actions, message brokers, WebSockets, remote Git fetch.

## Consequences

### Positive

- Deterministic, testable PR analysis without model-provider coupling
- Clear safety boundary: report and recommend only; no Git or GitHub mutations
- Reuses Knowledge Engine memory without duplicated document storage
- Full audit trail for review runs
- Portal visibility and export for operator workflows
- Upgrade path: future phases may add optional LLM enrichment without replacing the rule-based core

### Negative

- Heuristic rules produce false positives and false negatives compared to human or LLM review
- Diff content must be supplied by the client; no remote Git fetch in this phase
- Keyword knowledge matching is less precise than semantic retrieval
- Parallel analysis uses a short-lived thread pool per run
- In-process cache does not invalidate across Platform API instances

### Alternatives considered

- **Extend artifact Review Agent (`ai.nova.platform.review`)** — rejected; different input domain (pipeline artifacts vs. operator-supplied PR diffs) and lifecycle
- **LLM-first PR review** — rejected for this phase; deferred to post-beta optional enrichment (see roadmap)
- **GitHub-native review posting** — rejected; violates read-only / no-write safety boundary
- **Duplicate knowledge storage in PR review tables** — rejected; references Knowledge Engine document IDs only
- **Vector/embedding similarity for knowledge linkage** — rejected (explicit constraint for this phase)

## References

- [Automated PR Review Engine](../045_PR_REVIEW_ENGINE.md)
- [Knowledge and Memory Engine](../044_KNOWLEDGE_ENGINE.md)
- [ADR-0015: Review Agent](ADR-0015-review-agent.md)
- [ADR-0019: Pull Request Agent](ADR-0019-pull-request-agent.md)
- [ADR-0033: Knowledge and Memory Engine](ADR-0033-knowledge-engine.md)
