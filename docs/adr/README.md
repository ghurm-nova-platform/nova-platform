# Architecture Decision Records

Architecture Decision Records (ADRs) capture decisions that materially affect Nova Platform.

## Rules

1. Copy `ADR_TEMPLATE.md`.
2. Use the next four-digit sequence number.
3. Open a pull request before implementation depends on the decision.
4. Never rewrite accepted history. Supersede an ADR with a new ADR.
5. Record security, operational, cost, and migration consequences.

## Index

| ADR | Title | Status |
|-----|-------|--------|
| [ADR-0001](ADR-0001-modular-monorepo.md) | Modular monorepo | Accepted |
| [ADR-0002](ADR-0002-jwt-auth-foundation.md) | JWT authentication foundation on Platform API | Accepted |
| [ADR-0003](ADR-0003-agent-lifecycle.md) | Project-scoped agent lifecycle on Platform API | Accepted |
| [ADR-0004](ADR-0004-prompt-versioning.md) | Immutable published prompt versions | Accepted |
| [ADR-0005](ADR-0005-agent-execution-noop.md) | NoOp-first agent execution behind Platform API | Accepted |
| [ADR-0006](ADR-0006-conversation-memory.md) | Bounded conversation memory on Platform API | Accepted |
| [ADR-0007](ADR-0007-secure-tool-calling.md) | Secure allowlisted tool calling on Platform API | Accepted |
| [ADR-0008](ADR-0008-provider-neutral-rag.md) | Provider-neutral knowledge bases and RAG on Platform API | Accepted |
| [ADR-0009](ADR-0009-provider-neutral-model-gateway.md) | Provider-neutral AI model gateway and routing on Platform API | Accepted |
| [ADR-0010](ADR-0010-provider-secret-vault.md) | Provider secret vault and production OpenAI / Azure adapters | Accepted |
| [ADR-0011](ADR-0011-model-catalog.md) | Database-driven model catalog, capabilities, aliases, and sync | Accepted |
| [ADR-0012](ADR-0012-durable-agent-orchestration.md) | Durable multi-agent orchestration foundation | Accepted |
| [ADR-0013](ADR-0013-planner-agent.md) | Planner Agent and dynamic workflow generation | Accepted |
| [ADR-0014](ADR-0014-coding-agent.md) | Coding Agent generates artifacts only | Accepted |
| [ADR-0015](ADR-0015-review-agent.md) | Review Agent evaluates artifacts only | Accepted |
| [ADR-0016](ADR-0016-testing-agent.md) | Testing Agent generates plans only | Accepted |
| [ADR-0017](ADR-0017-patch-agent.md) | Patch Agent generates diffs only | Accepted |
| [ADR-0018](ADR-0018-git-integration-agent.md) | Git Integration Agent applies patches on isolated branches only | Accepted |
| [ADR-0019](ADR-0019-pull-request-agent.md) | Pull Request Agent creates PRs only | Accepted |
| [ADR-0020](ADR-0020-ci-observation-agent.md) | Read-only CI observation via provider abstraction | Accepted |
| [ADR-0021](ADR-0021-repair-agent.md) | Repair Agent creates new PatchResults only | Accepted |
| [ADR-0022](ADR-0022-approval-gate.md) | Approval Gate evaluates eligibility only | Accepted |
| [ADR-0023](ADR-0023-merge-agent.md) | Merge Agent merges only after Approval Gate | Accepted |
| [ADR-0024](ADR-0024-release-manager.md) | Release Manager owns immutable release lifecycle | Accepted |
| [ADR-0025](ADR-0025-deployment-observation.md) | Deployment Observation is observe-only | Accepted |
| [ADR-0026](ADR-0026-rollback-manager.md) | Rollback Manager is planning-only | Accepted |
| [ADR-0027](ADR-0027-release-policies.md) | Release Policies evaluate without mutating upstream records | Accepted |
| [ADR-0028](ADR-0028-environment-management.md) | Environment Management extends global catalog without deployment | Accepted |
| [ADR-0029](ADR-0029-audit-center.md) | Enterprise Audit Center append-only trail | Accepted |
