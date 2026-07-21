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
@Table(name = "llm_usage_metrics")
public class LlmUsageMetricEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "model_id")
    private UUID modelId;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 40)
    private LlmProviderType providerType;

    @Column(name = "request_type", nullable = false, length = 40)
    private String requestType;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "context_tokens", nullable = false)
    private int contextTokens;

    @Column(name = "queue_time_ms", nullable = false)
    private long queueTimeMs;

    @Column(name = "inference_time_ms", nullable = false)
    private long inferenceTimeMs;

    @Column(name = "streaming_duration_ms")
    private Long streamingDurationMs;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LlmUsageMetricEntity() {
    }

    public LlmUsageMetricEntity(
            UUID id,
            UUID organizationId,
            UUID modelId,
            UUID conversationId,
            LlmProviderType providerType,
            String requestType,
            int inputTokens,
            int outputTokens,
            long inferenceTimeMs,
            boolean success,
            String errorCode,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.modelId = modelId;
        this.conversationId = conversationId;
        this.providerType = providerType;
        this.requestType = requestType;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.contextTokens = 0;
        this.queueTimeMs = 0;
        this.inferenceTimeMs = inferenceTimeMs;
        this.success = success;
        this.errorCode = errorCode;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getModelId() { return modelId; }
    public UUID getConversationId() { return conversationId; }
    public LlmProviderType getProviderType() { return providerType; }
    public String getRequestType() { return requestType; }
    public int getInputTokens() { return inputTokens; }
    public int getOutputTokens() { return outputTokens; }
    public int getContextTokens() { return contextTokens; }
    public long getQueueTimeMs() { return queueTimeMs; }
    public long getInferenceTimeMs() { return inferenceTimeMs; }
    public Long getStreamingDurationMs() { return streamingDurationMs; }
    public boolean isSuccess() { return success; }
    public String getErrorCode() { return errorCode; }
    public Instant getCreatedAt() { return createdAt; }

    public void setContextTokens(int contextTokens) { this.contextTokens = contextTokens; }
    public void setQueueTimeMs(long queueTimeMs) { this.queueTimeMs = queueTimeMs; }
    public void setStreamingDurationMs(Long streamingDurationMs) { this.streamingDurationMs = streamingDurationMs; }
}
