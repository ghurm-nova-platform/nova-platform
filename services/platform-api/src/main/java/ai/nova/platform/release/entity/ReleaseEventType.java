package ai.nova.platform.release.entity;

public enum ReleaseEventType {
    CREATED,
    PREPARE_STARTED,
    MANIFEST_GENERATED,
    READY,
    PUBLISHED,
    ARCHIVED,
    FAILED,
    IDEMPOTENT_RETURN
}
