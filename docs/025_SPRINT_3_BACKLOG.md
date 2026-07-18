# Sprint 3 Backlog

**Sprint goal (draft):** Specialized execution agents, review loops, and first Git-facing artifact workflows — without changing Coding Agent V35 until multi-generation storage is explicitly scheduled.

## Planned

### S3 — Artifact Versioning / Multi-Generation Storage

Evolve `generated_artifacts` so a task can retain multiple generate attempts instead of latest-only replacement.

**Context**

Coding Agent V35 is intentionally latest-only per `task_id`. That keeps storage, validation, retrieval, and orchestration deterministic. Multi-generation history becomes valuable when Review Agent, retry policies, and Git integration need to compare attempts and choose a winner before patches or commits.

**Proposed model**

- Add `generation_id UUID` to `generated_artifacts`
- Index `(task_id, generation_id)`
- Replace `uq_generated_artifacts_task_path` with `uq_generated_artifacts_generation_path` (path unique within one generation)

**Goals**

- Preserve multiple generations for the same task
- Compare generations
- Perform AI review across generations
- Choose the best generation before creating patches
- Support retry history
- Support human approval workflows

**Acceptance criteria (when scheduled)**

- A single `POST /api/coding/generate` assigns one `generation_id` to all artifacts from that call
- Earlier generations for the same task remain readable
- Path uniqueness is enforced per generation, not per task
- Portal/API can list generations and select one for downstream patch/commit flows
- V35 latest-only behavior is superseded only by an explicit migration and API contract

**Out of scope until scheduled**

- No change to V35 schema in Sprint 2
- No API or database changes as part of documentation-only roadmap notes

## Related

- [`024_CODING_AGENT.md`](024_CODING_AGENT.md) — Future Evolution
- [`adr/ADR-0014-coding-agent.md`](adr/ADR-0014-coding-agent.md)
