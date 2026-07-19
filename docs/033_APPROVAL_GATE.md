# Approval Gate

Sprint 3 Phase 6 — Approval Gate evaluates whether an orchestration task and its Pull Request satisfy configured organizational requirements before they become eligible for future Merge Agent processing. It produces a governance decision only. It never merges, GitHub-approves, pushes, modifies patches/commits, reruns CI, or deploys.

## Purpose

Given an orchestration `taskId`, Approval Gate:

1. Collects **trusted evidence** from persisted Nova agent outputs (Review, Testing, Patch, Git, Pull Request, CI Observation, Repair)
2. Selects the active organization or project approval policy
3. Evaluates immutable policy rules against collected evidence
4. Computes deterministic evidence and decision fingerprints
5. Records automated gate results and human approval/rejection actions
6. Calculates `eligibleForMerge` for downstream Merge Agent consumption
7. Persists append-only governance history

## Safety boundary

Must **not**:

- merge a Pull Request or enable auto-merge
- approve a GitHub Pull Request
- push commits or modify branches
- create or modify patches, commits, or CI runs
- deploy to any environment
- modify repository settings or branch protection
- store credentials or secrets in approval tables or API responses
- trust request-supplied evidence IDs

Portal safety statement:

> Approval Gate evaluates and records approval eligibility. It never merges or deploys code.

For `APPROVED` decisions the portal also states: **"Merge has not been performed."**

## Architectural boundary

Previous agents produce evidence; Approval Gate consumes it:

```text
Review → Testing → Patch → Git → Pull Request → CI Observation → Repair (optional)
                              ↓
                       Approval Gate
                              ↓
                    ApprovalDecision (future Merge Agent input)
```

Approval Gate MUST NOT regenerate or modify upstream evidence.

## Decision types

| Decision | Meaning |
|----------|---------|
| `PENDING` | Evaluation in progress |
| `ELIGIBLE` | Automated requirements passed (human approval may still be required) |
| `BLOCKED` | One or more blocking automated requirements failed |
| `REQUIRES_HUMAN_APPROVAL` | Automated gate passed; required human approvals not yet met |
| `APPROVED` | All automated and human requirements satisfied |
| `REJECTED` | Authorized human explicitly rejected |
| `EXPIRED` | Decision exceeded configured validity duration |
| `SUPERSEDED` | Newer relevant patch, commit, PR, CI, repair, or decision exists |
| `INVALIDATED` | Previously approved evidence no longer matches trusted state |
| `ERROR` | Unexpected evaluation failure |

Operation status (`ApprovalOperationStatus`) is separate: `PENDING` → `COLLECTING` → `EVALUATING` → `WAITING_FOR_HUMAN` | `SUCCEEDED` | `FAILED`.

## Policy model

- Policies are versioned (`approval_policies.version`); active policies are never edited in place
- Project-scoped policy takes precedence over organization default when both are `ACTIVE`
- Only one active default policy per organization/project scope
- Normalized rules in `approval_policy_rules` (`rule_code`, `operator`, `expected_value`, `blocking`, `severity`)
- Seed default policy: `Default Approval Policy` v1 for demo organization

## Evidence collection

Evidence is loaded from existing storage services (`findLatest` by task and organization). Request bodies supply only `taskId` and optional human-action comments — never evidence IDs.

Evidence types: `REVIEW`, `TESTING`, `PATCH`, `GIT`, `PULL_REQUEST`, `CI`, `REPAIR`, `POLICY`.

Persisted in `approval_evidence` as references (`source_operation_id`, `source_result_id`, `source_hash`, `observed_status`, `observed_value`).

## Evidence fingerprint

Deterministic SHA-256 over canonical ordered fields:

- organization, project, task, policy id/version
- review, testing, patch, git, PR, CI, repair identifiers and hashes
- PR branch and commit alignment fields

Secrets and tokens are excluded. Stored as `evidence_fingerprint` (64-char hex).

## Decision fingerprint

Includes evidence fingerprint, decision value, requirement results, human action state, and decision timestamp. Stored as `decision_fingerprint`. Provides integrity correlation — not legal non-repudiation.

## Human approvals

- `APPROVAL_GATE_APPROVE` — optional sanitized comment; bound to exact evidence fingerprint
- `APPROVAL_GATE_REJECT` — required sanitized comment
- Immutable append-only rows in `approval_human_actions` (`APPROVE`, `REJECT`, `WITHDRAW_APPROVAL`)
- Rules: distinct approvers, prohibit author approval when configured, no duplicate active approvals per actor/fingerprint
- Approvals against stale fingerprints do not count toward new decisions

## Stale decision protection

Before returning `APPROVED` or `eligibleForMerge=true`, revalidate:

- latest patch, git commit, PR head, PR open state, CI status/commit
- policy still active; decision not expired
- no newer repair or superseding evidence

Stale decisions become `SUPERSEDED` or `INVALIDATED`; human actions are disabled until re-evaluation.

## Idempotency

- Re-running evaluation with identical policy version and evidence fingerprint returns the existing decision when human-action state is unchanged
- Duplicate human approvals by the same actor for the same fingerprint are rejected
- Optional `idempotencyKey` on approve/reject requests

## APIs

| Method | Path | Permission |
|--------|------|------------|
| POST | `/api/approval-gate/run` | `APPROVAL_GATE_RUN` |
| GET | `/api/approval-gate/{taskId}` | `APPROVAL_GATE_READ` |
| GET | `/api/approval-gate/{taskId}/history` | `APPROVAL_GATE_READ` |
| GET | `/api/approval-gate/{taskId}/requirements` | `APPROVAL_GATE_READ` |
| POST | `/api/approval-gate/{taskId}/approve` | `APPROVAL_GATE_APPROVE` |
| POST | `/api/approval-gate/{taskId}/reject` | `APPROVAL_GATE_REJECT` |

Run request:

```json
{
  "taskId": "11111111-1111-1111-1111-111111111025"
}
```

Approve request:

```json
{
  "comment": "Optional safe comment",
  "idempotencyKey": "optional-uuid"
}
```

Reject request (comment required):

```json
{
  "comment": "Required rejection reason"
}
```

Response DTO (`ApprovalDecision`) includes decision, eligibility, policy, fingerprints, evidence summaries, requirements, human actions, timeline — **no secrets**.

## Configuration

Prefix: `nova.approval-gate`

| Property | Description |
|----------|-------------|
| `enabled` | Master switch (default `false` in main `application.yml`) |
| `default-required-human-approvals` | Fallback when policy omits value |
| `default-decision-validity-minutes` | Default decision TTL |
| `require-distinct-approvers` | Global safety default |
| `prohibit-author-approval` | Global safety default |
| `invalidate-on-new-patch` | Stale guard toggles |
| `invalidate-on-new-commit` | Stale guard toggles |
| `invalidate-on-new-ci-observation` | Stale guard toggles |
| `invalidate-on-pr-head-change` | Stale guard toggles |
| `fail-closed` | Missing evidence → BLOCKED/ERROR, never APPROVED |
| `maximum-comment-length` | Human action comment limit |
| `max-repair-attempts` | Repair rule upper bound |

Database policy is authoritative after selection; configuration supplies safe defaults only.

## Permissions

Seeded in V43:

- `APPROVAL_GATE_RUN`, `APPROVAL_GATE_READ`, `APPROVAL_GATE_APPROVE`, `APPROVAL_GATE_REJECT`
- `APPROVAL_POLICY_READ`, `APPROVAL_POLICY_MANAGE`

## Database (V43)

- `approval_policies`, `approval_policy_rules`
- `approval_gate_operations`, `approval_decisions`
- `approval_evidence`, `approval_requirements`
- `approval_human_actions`, `approval_decision_events`

## Error codes

`APPROVAL_GATE_DISABLED`, `APPROVAL_TASK_NOT_FOUND`, `APPROVAL_POLICY_NOT_FOUND`, `APPROVAL_EVIDENCE_MISSING`, `APPROVAL_REVIEW_FAILED`, `APPROVAL_CI_NOT_SUCCESSFUL`, `APPROVAL_HUMAN_APPROVAL_REQUIRED`, `APPROVAL_REJECTION_COMMENT_REQUIRED`, `APPROVAL_DECISION_EXPIRED`, `APPROVAL_DECISION_SUPERSEDED`, `APPROVAL_AUTHOR_CANNOT_APPROVE`, …

## Relationship with Merge Agent

Approval Gate records eligibility. A future Merge Agent will consume a valid `APPROVED` decision with `eligibleForMerge=true` and matching fingerprints. Merge Agent is out of scope for this phase.

## Known limitations

- No merge, GitHub PR approval, auto-merge, or deployment
- No branch protection management
- Internal Nova human approvals only — no GitHub reviewer sync yet
- No external identity-provider approval workflow yet
- No scheduled automatic re-evaluation
- GitHub Actions evidence only through current CI provider abstraction
- Basic policy templates; no advanced risk scoring
- SHA-256 fingerprints provide integrity correlation, not cryptographic non-repudiation

## Portal

Route `/approval-gate` — task ID, Evaluate / Load latest / Load history, decision and eligibility badges, policy name/version, masked evidence fingerprint with copy, validity, requirement sections, evidence summaries, human approval counts, Approve/Reject (permission-gated), timeline, history, stale warnings.

## Related

- ADR-0022
- Repair Agent (`032`, ADR-0021)
- CI Observation Agent (`031`, ADR-0020)
- Pull Request Agent (`030`, ADR-0019)
