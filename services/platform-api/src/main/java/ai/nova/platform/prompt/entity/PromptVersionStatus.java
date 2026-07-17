package ai.nova.platform.prompt.entity;

public enum PromptVersionStatus {
    DRAFT,
    PUBLISHED,
    /** Previously published; retained so existing agent references keep working. */
    SUPERSEDED,
    ARCHIVED
}
