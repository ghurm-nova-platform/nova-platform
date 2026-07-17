package ai.nova.platform.execution.service;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.agent.runtime.RuntimeFinalResponse;
import ai.nova.platform.execution.entity.AgentExecution;
import ai.nova.platform.execution.entity.ExecutionMessage;
import ai.nova.platform.execution.entity.ExecutionMetric;
import ai.nova.platform.execution.entity.ExecutionStatus;
import ai.nova.platform.execution.entity.MessageRole;
import ai.nova.platform.execution.repository.AgentExecutionRepository;
import ai.nova.platform.execution.repository.ExecutionMessageRepository;
import ai.nova.platform.execution.repository.ExecutionMetricRepository;
import ai.nova.platform.web.correlation.CorrelationIdFilter;
import ai.nova.platform.web.error.ApiException;

/**
 * Short transactional boundaries for execution lifecycle.
 * Runtime calls must happen outside these methods so RUNNING is visible to cancel.
 */
@Service
public class ExecutionLifecycleService {

    public static final String SAFE_ERROR_MESSAGE = "Execution failed";
    public static final String ERROR_CODE_EXECUTION_FAILED = "EXECUTION_FAILED";

    private final AgentExecutionRepository executionRepository;
    private final ExecutionMessageRepository messageRepository;
    private final ExecutionMetricRepository metricRepository;

    public ExecutionLifecycleService(
            AgentExecutionRepository executionRepository,
            ExecutionMessageRepository messageRepository,
            ExecutionMetricRepository metricRepository) {
        this.executionRepository = executionRepository;
        this.messageRepository = messageRepository;
        this.metricRepository = metricRepository;
    }

    @Transactional
    public AgentExecution startRunning(
            UUID executionId,
            UUID organizationId,
            UUID projectId,
            UUID agentId,
            UUID promptVersionId,
            UUID conversationId,
            String provider,
            String model,
            UUID createdBy,
            String renderedPrompt,
            String userMessage) {
        Instant now = Instant.now();
        AgentExecution execution = new AgentExecution(
                executionId,
                organizationId,
                projectId,
                agentId,
                promptVersionId,
                conversationId,
                provider,
                model,
                ExecutionStatus.PENDING,
                createdBy,
                now);
        executionRepository.save(execution);
        messageRepository.save(new ExecutionMessage(
                UUID.randomUUID(), executionId, MessageRole.SYSTEM, renderedPrompt, now));
        messageRepository.save(new ExecutionMessage(
                UUID.randomUUID(), executionId, MessageRole.USER, userMessage, now));

        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setStartedAt(Instant.now());
        return executionRepository.saveAndFlush(execution);
    }

    @Transactional
    public AgentExecution completeIfRunning(UUID executionId, RuntimeFinalResponse result) {
        AgentExecution execution = require(executionId);
        if (execution.getStatus() == ExecutionStatus.CANCELLED) {
            return execution;
        }
        if (execution.getStatus() != ExecutionStatus.RUNNING) {
            return execution;
        }

        Instant completedAt = Instant.now();
        execution.setStatus(ExecutionStatus.COMPLETED);
        execution.setInputTokens(result.inputTokens());
        execution.setOutputTokens(result.outputTokens());
        execution.setTotalTokens(result.totalTokens());
        execution.setLatencyMs(Math.toIntExact(result.latencyMs()));
        execution.setCompletedAt(completedAt);
        executionRepository.save(execution);

        messageRepository.save(new ExecutionMessage(
                UUID.randomUUID(),
                executionId,
                MessageRole.ASSISTANT,
                result.responseText(),
                completedAt));
        metricRepository.save(new ExecutionMetric(
                UUID.randomUUID(), executionId, "latency_ms", String.valueOf(result.latencyMs()), completedAt));
        metricRepository.save(new ExecutionMetric(
                UUID.randomUUID(),
                executionId,
                "total_tokens",
                String.valueOf(result.totalTokens()),
                completedAt));
        return execution;
    }

    @Transactional
    public AgentExecution failIfRunning(UUID executionId) {
        return failIfRunning(executionId, ERROR_CODE_EXECUTION_FAILED);
    }

    @Transactional
    public AgentExecution failIfRunning(UUID executionId, String errorCode) {
        AgentExecution execution = require(executionId);
        if (execution.getStatus() == ExecutionStatus.CANCELLED) {
            return execution;
        }
        if (execution.getStatus() != ExecutionStatus.RUNNING) {
            return execution;
        }

        Instant completedAt = Instant.now();
        execution.setStatus(ExecutionStatus.FAILED);
        execution.setCompletedAt(completedAt);
        execution.setErrorMessage(SAFE_ERROR_MESSAGE);
        executionRepository.save(execution);

        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        metricRepository.save(new ExecutionMetric(
                UUID.randomUUID(),
                executionId,
                "error_code",
                errorCode != null ? errorCode : ERROR_CODE_EXECUTION_FAILED,
                completedAt));
        if (correlationId != null && !correlationId.isBlank()) {
            metricRepository.save(new ExecutionMetric(
                    UUID.randomUUID(),
                    executionId,
                    "correlation_id",
                    correlationId,
                    completedAt));
        }
        return execution;
    }

    @Transactional
    public void persistToolMessage(UUID executionId, String content) {
        messageRepository.save(new ExecutionMessage(
                UUID.randomUUID(), executionId, MessageRole.TOOL, content, Instant.now()));
    }

    @Transactional
    public AgentExecution cancelIfActive(UUID projectId, UUID executionId, UUID organizationId) {
        AgentExecution execution = executionRepository
                .findByIdAndProjectIdAndOrganizationId(executionId, projectId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "EXECUTION_NOT_FOUND", "Execution not found"));

        if (execution.getStatus() != ExecutionStatus.PENDING
                && execution.getStatus() != ExecutionStatus.RUNNING) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EXECUTION_CANCELLED",
                    "Execution cannot be cancelled in status " + execution.getStatus());
        }

        execution.setStatus(ExecutionStatus.CANCELLED);
        execution.setCompletedAt(Instant.now());
        return executionRepository.saveAndFlush(execution);
    }

    private AgentExecution require(UUID executionId) {
        return executionRepository
                .findById(executionId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "EXECUTION_NOT_FOUND", "Execution not found"));
    }
}
