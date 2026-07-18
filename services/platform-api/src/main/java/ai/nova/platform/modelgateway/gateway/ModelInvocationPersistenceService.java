package ai.nova.platform.modelgateway.gateway;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.execution.entity.AgentExecution;
import ai.nova.platform.execution.entity.ExecutionStatus;
import ai.nova.platform.execution.repository.AgentExecutionRepository;
import ai.nova.platform.modelgateway.entity.InvocationStatus;
import ai.nova.platform.modelgateway.entity.ModelInvocation;
import ai.nova.platform.modelgateway.provider.ProviderInvokeResult;
import ai.nova.platform.modelgateway.repository.ModelInvocationRepository;

@Service
public class ModelInvocationPersistenceService {

    private final ModelInvocationRepository invocationRepository;
    private final AgentExecutionRepository executionRepository;

    public ModelInvocationPersistenceService(
            ModelInvocationRepository invocationRepository, AgentExecutionRepository executionRepository) {
        this.invocationRepository = invocationRepository;
        this.executionRepository = executionRepository;
    }

    @Transactional
    public ModelInvocation createRunning(
            UUID invocationId,
            UUID organizationId,
            UUID projectId,
            UUID agentId,
            UUID executionId,
            UUID conversationId,
            UUID providerId,
            UUID modelId,
            UUID routingPolicyId,
            int attemptNumber,
            int inputCharacterCount,
            UUID createdBy,
            UUID fallbackFromInvocationId) {
        Instant now = Instant.now();
        ModelInvocation invocation = new ModelInvocation();
        invocation.setId(invocationId);
        invocation.setOrganizationId(organizationId);
        invocation.setProjectId(projectId);
        invocation.setAgentId(agentId);
        invocation.setExecutionId(executionId);
        invocation.setConversationId(conversationId);
        invocation.setProviderId(providerId);
        invocation.setModelId(modelId);
        invocation.setRoutingPolicyId(routingPolicyId);
        invocation.setAttemptNumber(attemptNumber);
        invocation.setStatus(InvocationStatus.RUNNING);
        invocation.setInputCharacterCount(inputCharacterCount);
        invocation.setStartedAt(now);
        invocation.setCreatedBy(createdBy);
        invocation.setFallbackFromInvocationId(fallbackFromInvocationId);
        return invocationRepository.saveAndFlush(invocation);
    }

    @Transactional(readOnly = true)
    public boolean isExecutionCancelled(UUID executionId) {
        return executionRepository
                .findById(executionId)
                .map(execution -> execution.getStatus() == ExecutionStatus.CANCELLED)
                .orElse(false);
    }

    /**
     * Transaction 2 success path: lock invocation + execution and decide COMPLETED vs CANCELLED atomically.
     */
    @Transactional
    public CompletionOutcome completeSuccess(UUID invocationId, ProviderInvokeResult result) {
        ModelInvocation invocation = requireForUpdate(invocationId);
        if (invocation.getStatus() != InvocationStatus.RUNNING) {
            return CompletionOutcome.from(invocation);
        }
        AgentExecution execution = requireExecutionForUpdate(invocation.getExecutionId());
        if (execution.getStatus() == ExecutionStatus.CANCELLED) {
            invocation.setStatus(InvocationStatus.CANCELLED);
            invocation.setDurationMs(result.latencyMs());
            invocation.setCompletedAt(Instant.now());
            return CompletionOutcome.from(invocationRepository.save(invocation));
        }
        invocation.setStatus(InvocationStatus.COMPLETED);
        invocation.setOutputCharacterCount(
                result.responseText() != null ? result.responseText().length() : 0);
        invocation.setEstimatedInputTokens(result.inputTokens());
        invocation.setEstimatedOutputTokens(result.outputTokens());
        invocation.setProviderInputTokens(result.inputTokens());
        invocation.setProviderOutputTokens(result.outputTokens());
        invocation.setFinishReason(result.finishReason());
        invocation.setProviderRequestId(result.providerRequestId());
        invocation.setDurationMs(result.latencyMs());
        invocation.setCompletedAt(Instant.now());
        return CompletionOutcome.from(invocationRepository.save(invocation));
    }

    /**
     * Transaction 2 failure path: lock invocation + execution and decide failure vs CANCELLED atomically.
     */
    @Transactional
    public CompletionOutcome completeFailure(
            UUID invocationId, InvocationStatus status, String errorCode, long durationMs) {
        ModelInvocation invocation = requireForUpdate(invocationId);
        if (invocation.getStatus() != InvocationStatus.RUNNING) {
            return CompletionOutcome.from(invocation);
        }
        AgentExecution execution = requireExecutionForUpdate(invocation.getExecutionId());
        if (execution.getStatus() == ExecutionStatus.CANCELLED) {
            invocation.setStatus(InvocationStatus.CANCELLED);
        } else {
            invocation.setStatus(status);
            invocation.setErrorCode(errorCode);
        }
        invocation.setDurationMs(durationMs);
        invocation.setCompletedAt(Instant.now());
        return CompletionOutcome.from(invocationRepository.save(invocation));
    }

    @Transactional(readOnly = true)
    public int nextAttemptNumber(UUID executionId) {
        return invocationRepository.countByExecutionId(executionId) + 1;
    }

    private ModelInvocation requireForUpdate(UUID invocationId) {
        return invocationRepository
                .findByIdForUpdate(invocationId)
                .orElseThrow(() -> new IllegalStateException("Invocation not found: " + invocationId));
    }

    private AgentExecution requireExecutionForUpdate(UUID executionId) {
        return executionRepository
                .findByIdForUpdate(executionId)
                .orElseThrow(() -> new IllegalStateException("Execution not found: " + executionId));
    }

    public record CompletionOutcome(ModelInvocation invocation, InvocationStatus status) {

        static CompletionOutcome from(ModelInvocation invocation) {
            return new CompletionOutcome(invocation, invocation.getStatus());
        }

        public boolean completed() {
            return status == InvocationStatus.COMPLETED;
        }

        public boolean cancelled() {
            return status == InvocationStatus.CANCELLED;
        }
    }
}
