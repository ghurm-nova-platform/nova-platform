package ai.nova.platform.modelgateway.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "model_invocations")
public class ModelInvocation {

    @Id
    private UUID id;
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    @Column(name = "project_id", nullable = false)
    private UUID projectId;
    @Column(name = "agent_id", nullable = false)
    private UUID agentId;
    @Column(name = "execution_id", nullable = false)
    private UUID executionId;
    @Column(name = "conversation_id")
    private UUID conversationId;
    @Column(name = "provider_id", nullable = false)
    private UUID providerId;
    @Column(name = "model_id", nullable = false)
    private UUID modelId;
    @Column(name = "routing_policy_id")
    private UUID routingPolicyId;
    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvocationStatus status;
    @Column(name = "provider_request_id")
    private String providerRequestId;
    @Column(name = "input_character_count", nullable = false)
    private Integer inputCharacterCount;
    @Column(name = "output_character_count")
    private Integer outputCharacterCount;
    @Column(name = "estimated_input_tokens")
    private Integer estimatedInputTokens;
    @Column(name = "estimated_output_tokens")
    private Integer estimatedOutputTokens;
    @Column(name = "provider_input_tokens")
    private Integer providerInputTokens;
    @Column(name = "provider_output_tokens")
    private Integer providerOutputTokens;
    @Column(name = "duration_ms")
    private Long durationMs;
    @Column(name = "finish_reason", length = 100)
    private String finishReason;
    @Column(name = "error_code", length = 100)
    private String errorCode;
    @Column(name = "fallback_from_invocation_id")
    private UUID fallbackFromInvocationId;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    public ModelInvocation() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public UUID getAgentId() { return agentId; }
    public void setAgentId(UUID agentId) { this.agentId = agentId; }
    public UUID getExecutionId() { return executionId; }
    public void setExecutionId(UUID executionId) { this.executionId = executionId; }
    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
    public UUID getProviderId() { return providerId; }
    public void setProviderId(UUID providerId) { this.providerId = providerId; }
    public UUID getModelId() { return modelId; }
    public void setModelId(UUID modelId) { this.modelId = modelId; }
    public UUID getRoutingPolicyId() { return routingPolicyId; }
    public void setRoutingPolicyId(UUID routingPolicyId) { this.routingPolicyId = routingPolicyId; }
    public Integer getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; }
    public InvocationStatus getStatus() { return status; }
    public void setStatus(InvocationStatus status) { this.status = status; }
    public String getProviderRequestId() { return providerRequestId; }
    public void setProviderRequestId(String providerRequestId) { this.providerRequestId = providerRequestId; }
    public Integer getInputCharacterCount() { return inputCharacterCount; }
    public void setInputCharacterCount(Integer inputCharacterCount) { this.inputCharacterCount = inputCharacterCount; }
    public Integer getOutputCharacterCount() { return outputCharacterCount; }
    public void setOutputCharacterCount(Integer outputCharacterCount) { this.outputCharacterCount = outputCharacterCount; }
    public Integer getEstimatedInputTokens() { return estimatedInputTokens; }
    public void setEstimatedInputTokens(Integer estimatedInputTokens) { this.estimatedInputTokens = estimatedInputTokens; }
    public Integer getEstimatedOutputTokens() { return estimatedOutputTokens; }
    public void setEstimatedOutputTokens(Integer estimatedOutputTokens) { this.estimatedOutputTokens = estimatedOutputTokens; }
    public Integer getProviderInputTokens() { return providerInputTokens; }
    public void setProviderInputTokens(Integer providerInputTokens) { this.providerInputTokens = providerInputTokens; }
    public Integer getProviderOutputTokens() { return providerOutputTokens; }
    public void setProviderOutputTokens(Integer providerOutputTokens) { this.providerOutputTokens = providerOutputTokens; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getFinishReason() { return finishReason; }
    public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public UUID getFallbackFromInvocationId() { return fallbackFromInvocationId; }
    public void setFallbackFromInvocationId(UUID fallbackFromInvocationId) { this.fallbackFromInvocationId = fallbackFromInvocationId; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
