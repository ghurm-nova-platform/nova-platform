package ai.nova.platform.llm.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "llm_messages")
public class LlmMessageEntity {

    @Id
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LlmMessageRole role;

    @Column(nullable = false)
    private String content;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "metadata_json", nullable = false)
    private String metadataJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LlmMessageEntity() {
    }

    public LlmMessageEntity(
            UUID id,
            UUID conversationId,
            LlmMessageRole role,
            String content,
            int sequenceNo,
            Instant createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.sequenceNo = sequenceNo;
        this.metadataJson = "{}";
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getConversationId() { return conversationId; }
    public LlmMessageRole getRole() { return role; }
    public String getContent() { return content; }
    public Integer getTokenCount() { return tokenCount; }
    public int getSequenceNo() { return sequenceNo; }
    public String getMetadataJson() { return metadataJson; }
    public Instant getCreatedAt() { return createdAt; }

    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
}
