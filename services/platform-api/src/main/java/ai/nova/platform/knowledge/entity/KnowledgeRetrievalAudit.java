package ai.nova.platform.knowledge.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "knowledge_retrieval_audit")
public class KnowledgeRetrievalAudit {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "knowledge_base_id", nullable = false)
    private UUID knowledgeBaseId;

    @Column(name = "agent_id")
    private UUID agentId;

    @Column(name = "execution_id")
    private UUID executionId;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "query_hash", nullable = false, length = 64)
    private String queryHash;

    @Column(name = "query_character_count", nullable = false)
    private Integer queryCharacterCount;

    @Column(name = "requested_top_k", nullable = false)
    private Integer requestedTopK;

    @Column(name = "candidate_count", nullable = false)
    private Integer candidateCount;

    @Column(name = "returned_count", nullable = false)
    private Integer returnedCount;

    @Column(name = "minimum_score", precision = 8, scale = 6)
    private BigDecimal minimumScore;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(name = "performed_by", nullable = false)
    private UUID performedBy;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    protected KnowledgeRetrievalAudit() {
    }

    public KnowledgeRetrievalAudit(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID knowledgeBaseId,
            UUID agentId,
            UUID executionId,
            UUID conversationId,
            String queryHash,
            Integer queryCharacterCount,
            Integer requestedTopK,
            Integer candidateCount,
            Integer returnedCount,
            BigDecimal minimumScore,
            Long durationMs,
            UUID performedBy,
            Instant performedAt,
            String correlationId) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.knowledgeBaseId = knowledgeBaseId;
        this.agentId = agentId;
        this.executionId = executionId;
        this.conversationId = conversationId;
        this.queryHash = queryHash;
        this.queryCharacterCount = queryCharacterCount;
        this.requestedTopK = requestedTopK;
        this.candidateCount = candidateCount;
        this.returnedCount = returnedCount;
        this.minimumScore = minimumScore;
        this.durationMs = durationMs;
        this.performedBy = performedBy;
        this.performedAt = performedAt;
        this.correlationId = correlationId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public UUID getAgentId() {
        return agentId;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public String getQueryHash() {
        return queryHash;
    }

    public Integer getQueryCharacterCount() {
        return queryCharacterCount;
    }

    public Integer getRequestedTopK() {
        return requestedTopK;
    }

    public Integer getCandidateCount() {
        return candidateCount;
    }

    public Integer getReturnedCount() {
        return returnedCount;
    }

    public BigDecimal getMinimumScore() {
        return minimumScore;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public UUID getPerformedBy() {
        return performedBy;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
