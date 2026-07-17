# Prompt Versioning

Sprint 1 Phase 4 version lifecycle for project-scoped prompts.

## Lifecycle

1. Creating a prompt creates **version 1** in `DRAFT` and sets `current_draft_version_id`.
2. Draft versions may be edited (`PUT .../versions/{versionId}`).
3. Published versions are **immutable**.
4. Publishing a draft:
   - Validates content and variable definitions
   - Sets the version to `PUBLISHED`
   - Marks the previous published version as `SUPERSEDED` (not `ARCHIVED`) so
     existing agent references to that version continue to work
   - Updates `prompts.published_version_id` and prompt `status` to `PUBLISHED`
   - Clears `current_draft_version_id` when the published version was that draft
5. Editing after publish:
   - `POST .../versions` creates a new draft with the next sequential version number
   - Copied from the current published content when available
   - Must not modify the published row
6. Rollback:
   - Does **not** rewrite history
   - Creates a new draft copied from a selected historical version
   - Assigns the next version number
   - Optional reason stored in `change_summary`
   - User may publish the new draft afterward
7. Version numbers are sequential per prompt, generated server-side, and protected by
   transactional boundaries / optimistic locking on the parent prompt.

## Prompt status

| Status | Meaning |
|--------|---------|
| `DRAFT` | Never published, or only draft content exists |
| `PUBLISHED` | Has a current published version |
| `ARCHIVED` | Soft-deleted; not physically removed |

## Version status

| Status | Meaning |
|--------|---------|
| `DRAFT` | Editable working copy |
| `PUBLISHED` | Current published version for the prompt |
| `SUPERSEDED` | Previously published; immutable; may still be referenced by agents |
| `ARCHIVED` | Soft-retired version (not used for publish supersession) |

`DELETE` archives a prompt. Archive is blocked with `PROMPT_IN_USE` when an
`ACTIVE` agent references the prompt.

## Immutability

Physical updates to `PUBLISHED` prompt version rows are rejected with
`PROMPT_VERSION_IMMUTABLE`.

## Preview

Preview substitutes `{{variables}}` with provided sample values only.
It does not call an LLM, persist preview values, or log those values.

## Follow-up work

- Monaco Editor with richer highlighting
- LLM execution / evaluation workflows via Platform API → Agent Runtime
- Deprecate agent `systemPrompt` in favor of published prompt references
