package ai.nova.platform.prompt.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "prompt_tags")
public class PromptTag {

    @Id
    private UUID id;

    @Column(name = "prompt_id", nullable = false)
    private UUID promptId;

    @Column(name = "tag_name", nullable = false, length = 100)
    private String tagName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PromptTag() {
    }

    public PromptTag(UUID id, UUID promptId, String tagName, Instant createdAt) {
        this.id = id;
        this.promptId = promptId;
        this.tagName = tagName;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPromptId() {
        return promptId;
    }

    public String getTagName() {
        return tagName;
    }
}
