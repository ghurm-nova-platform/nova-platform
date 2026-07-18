package ai.nova.platform.tool.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.nova.platform.agent.entity.Agent;
import ai.nova.platform.agent.repository.AgentRepository;
import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.agent.runtime.ExecutionRequest;
import ai.nova.platform.agent.runtime.RuntimeFinalResponse;
import ai.nova.platform.agent.runtime.RuntimeKnowledgeCitation;
import ai.nova.platform.agent.runtime.RuntimeKnowledgeContext;
import ai.nova.platform.agent.runtime.RuntimeMessage;
import ai.nova.platform.agent.runtime.RuntimeToolCallBatch;
import ai.nova.platform.agent.runtime.RuntimeToolCallRequest;
import ai.nova.platform.agent.runtime.RuntimeToolResultMessage;
import ai.nova.platform.agent.runtime.RuntimeToolSpec;
import ai.nova.platform.agent.runtime.RuntimeTurnResult;
import ai.nova.platform.conversation.service.ConversationService;
import ai.nova.platform.conversation.validation.ConversationProperties;
import ai.nova.platform.execution.dto.ExecutionDtos.ExecuteResponse;
import ai.nova.platform.execution.entity.AgentExecution;
import ai.nova.platform.execution.entity.ExecutionMessage;
import ai.nova.platform.execution.entity.ExecutionStatus;
import ai.nova.platform.execution.entity.MessageRole;
import ai.nova.platform.execution.mapper.ExecutionMapper;
import ai.nova.platform.execution.repository.AgentExecutionRepository;
import ai.nova.platform.execution.repository.ExecutionMessageRepository;
import ai.nova.platform.execution.service.ExecutionLifecycleService;
import ai.nova.platform.knowledge.config.KnowledgeProperties;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.KnowledgeCitationResponse;
import ai.nova.platform.knowledge.service.ExecutionKnowledgeSnapshotService;
import ai.nova.platform.knowledge.service.KnowledgeRetrievalService;
import ai.nova.platform.knowledge.service.KnowledgeRetrievalService.RetrievalResult;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.tool.config.ToolProperties;
import ai.nova.platform.tool.entity.ExecutionToolCall;
import ai.nova.platform.tool.entity.ToolCallStatus;
import ai.nova.platform.tool.entity.ToolDefinition;
import ai.nova.platform.tool.entity.ToolStatus;
import ai.nova.platform.tool.executor.ToolExecutionContext;
import ai.nova.platform.tool.executor.ToolExecutionOutcome;
import ai.nova.platform.tool.executor.ToolExecutionResult;
import ai.nova.platform.tool.executor.ToolExecutor;
import ai.nova.platform.tool.executor.ToolExecutorRegistry;
import ai.nova.platform.tool.repository.AgentToolAssignmentRepository;
import ai.nova.platform.tool.service.ToolAuditService.ToolCallContext;
import ai.nova.platform.tool.validation.ToolInputValidator;
import ai.nova.platform.tool.validation.ToolValidationException;
import ai.nova.platform.web.error.ApiException;

@Service
public class ToolCallingOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ToolCallingOrchestrator.class);

    private final AgentRuntimeClient agentRuntimeClient;
    private final AgentToolAssignmentService agentToolAssignmentService;
    private final AgentToolAssignmentRepository assignmentRepository;
    private final ToolCallService toolCallService;
    private final ToolExecutorRegistry executorRegistry;
    private final ToolInputValidator inputValidator;
    private final ToolProperties toolProperties;
    private final ToolAuditService auditService;
    private final ExecutionLifecycleService lifecycleService;
    private final ExecutionMapper executionMapper;
    private final AgentExecutionRepository executionRepository;
    private final ExecutionMessageRepository executionMessageRepository;
    private final AgentRepository agentRepository;
    private final ConversationService conversationService;
    private final ConversationProperties conversationProperties;
    private final ObjectMapper objectMapper;
    private final ExecutorService toolExecutionExecutor;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final KnowledgeProperties knowledgeProperties;
    private final ExecutionKnowledgeSnapshotService knowledgeSnapshotService;

    public ToolCallingOrchestrator(
            AgentRuntimeClient agentRuntimeClient,
            AgentToolAssignmentService agentToolAssignmentService,
            AgentToolAssignmentRepository assignmentRepository,
            ToolCallService toolCallService,
            ToolExecutorRegistry executorRegistry,
            ToolInputValidator inputValidator,
            ToolProperties toolProperties,
            ToolAuditService auditService,
            ExecutionLifecycleService lifecycleService,
            ExecutionMapper executionMapper,
            AgentExecutionRepository executionRepository,
            ExecutionMessageRepository executionMessageRepository,
            AgentRepository agentRepository,
            ConversationService conversationService,
            ConversationProperties conversationProperties,
            ObjectMapper objectMapper,
            @Qualifier("toolExecutionExecutor") ExecutorService toolExecutionExecutor,
            KnowledgeRetrievalService knowledgeRetrievalService,
            KnowledgeProperties knowledgeProperties,
            ExecutionKnowledgeSnapshotService knowledgeSnapshotService) {
        this.agentRuntimeClient = agentRuntimeClient;
        this.agentToolAssignmentService = agentToolAssignmentService;
        this.assignmentRepository = assignmentRepository;
        this.toolCallService = toolCallService;
        this.executorRegistry = executorRegistry;
        this.inputValidator = inputValidator;
        this.toolProperties = toolProperties;
        this.auditService = auditService;
        this.lifecycleService = lifecycleService;
        this.executionMapper = executionMapper;
        this.executionRepository = executionRepository;
        this.executionMessageRepository = executionMessageRepository;
        this.agentRepository = agentRepository;
        this.conversationService = conversationService;
        this.conversationProperties = conversationProperties;
        this.objectMapper = objectMapper;
        this.toolExecutionExecutor = toolExecutionExecutor;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
        this.knowledgeProperties = knowledgeProperties;
        this.knowledgeSnapshotService = knowledgeSnapshotService;
    }

    public record OrchestrationRequest(
            UUID executionId,
            Agent agent,
            AuthenticatedUser user,
            UUID projectId,
            UUID agentId,
            String renderedPrompt,
            List<RuntimeMessage> messages,
            UUID conversationId,
            RuntimeKnowledgeContext knowledgeContext) {
    }

    public ExecuteResponse orchestrate(OrchestrationRequest request) {
        ExecuteResponse cancelled = cancelledResponseIfNeeded(request.executionId(), request.renderedPrompt());
        if (cancelled != null) {
            return cancelled;
        }

        RuntimeKnowledgeContext knowledgeContext = request.knowledgeContext();
        if (knowledgeContext == null) {
            knowledgeContext = retrieveKnowledge(request);
            cancelled = cancelledResponseIfNeeded(request.executionId(), request.renderedPrompt());
            if (cancelled != null) {
                return cancelled;
            }
        }
        knowledgeSnapshotService.saveIfAbsent(
                request.executionId(),
                request.user().getOrganizationId(),
                request.projectId(),
                knowledgeContext);

        OrchestrationRequest enriched = new OrchestrationRequest(
                request.executionId(),
                request.agent(),
                request.user(),
                request.projectId(),
                request.agentId(),
                request.renderedPrompt(),
                request.messages(),
                request.conversationId(),
                knowledgeContext);

        List<ToolDefinition> assignedTools = agentToolAssignmentService.loadAssignedActiveTools(
                enriched.projectId(), enriched.agentId(), enriched.user().getOrganizationId());
        Map<String, ToolDefinition> toolsByKey = assignedTools.stream()
                .collect(Collectors.toMap(
                        ToolDefinition::getToolKey, tool -> tool, (left, right) -> left, LinkedHashMap::new));
        List<RuntimeToolSpec> availableTools = toRuntimeToolSpecs(assignedTools);

        return runLoop(
                enriched,
                availableTools,
                toolsByKey,
                enriched.messages(),
                List.of(),
                0);
    }

    private RuntimeKnowledgeContext retrieveKnowledge(OrchestrationRequest request) {
        if (!knowledgeProperties.isRetrievalEnabled()) {
            return RuntimeKnowledgeContext.empty();
        }
        if (!knowledgeRetrievalService.hasEnabledAssignments(
                request.projectId(), request.agentId(), request.user().getOrganizationId())) {
            return RuntimeKnowledgeContext.empty();
        }
        String query = lastUserContent(request.messages());
        RetrievalResult result = knowledgeRetrievalService.retrieve(
                request.projectId(),
                request.agentId(),
                query,
                request.executionId(),
                request.conversationId(),
                request.user());
        return result.context();
    }

    private static String lastUserContent(List<RuntimeMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        String last = "";
        for (RuntimeMessage message : messages) {
            if ("USER".equals(message.role())) {
                last = message.content();
            }
        }
        return last;
    }

    private static List<KnowledgeCitationResponse> toCitationDtos(RuntimeKnowledgeContext context) {
        if (context == null || context.citations().isEmpty()) {
            return List.of();
        }
        List<KnowledgeCitationResponse> citations = new ArrayList<>();
        for (RuntimeKnowledgeCitation citation : context.citations()) {
            citations.add(new KnowledgeCitationResponse(
                    citation.label(),
                    citation.knowledgeBaseId(),
                    citation.knowledgeBaseName(),
                    citation.documentId(),
                    citation.documentName(),
                    citation.chunkIndex(),
                    citation.score()));
        }
        return citations;
    }

    public ExecuteResponse continueAfterApproval(
            UUID projectId, UUID executionId, AuthenticatedUser user, String renderedPrompt) {
        AgentExecution execution = requireRunningExecution(projectId, executionId, user.getOrganizationId());
        Agent agent = agentRepository
                .findByIdAndProjectIdAndOrganizationId(
                        execution.getAgentId(), projectId, user.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AGENT_NOT_FOUND", "Agent not found"));

        List<ToolDefinition> assignedTools = agentToolAssignmentService.loadAssignedActiveTools(
                projectId, execution.getAgentId(), user.getOrganizationId());
        Map<String, ToolDefinition> toolsByKey = assignedTools.stream()
                .collect(Collectors.toMap(
                        ToolDefinition::getToolKey, tool -> tool, (left, right) -> left, LinkedHashMap::new));
        List<RuntimeToolSpec> availableTools = toRuntimeToolSpecs(assignedTools);
        List<RuntimeMessage> messages = loadRuntimeMessages(executionId);
        RuntimeKnowledgeContext knowledgeContext = knowledgeSnapshotService.load(
                executionId, projectId, user.getOrganizationId());

        List<ExecutionToolCall> approvedCalls = toolCallService
                .findByExecutionAndStatuses(executionId, projectId, user.getOrganizationId(), List.of(ToolCallStatus.APPROVED));
        List<RuntimeToolResultMessage> toolResults = new ArrayList<>();
        for (ExecutionToolCall approvedCall : approvedCalls) {
            Optional<RuntimeToolResultMessage> existing = toStoredResult(approvedCall);
            if (existing.isPresent()) {
                toolResults.add(existing.get());
                continue;
            }
            ToolProcessOutcome outcome = processApprovedCall(
                    approvedCall, toolsByKey, execution, agent, user, projectId, execution.getConversationId());
            if (outcome.awaitingApproval()) {
                return withCitations(outcome.response(), knowledgeContext);
            }
            if (outcome.failed()) {
                return withCitations(outcome.response(), knowledgeContext);
            }
            if (outcome.cancelled()) {
                return withCitations(outcome.response(), knowledgeContext);
            }
            toolResults.add(outcome.result());
        }

        OrchestrationRequest request = new OrchestrationRequest(
                executionId,
                agent,
                user,
                projectId,
                execution.getAgentId(),
                renderedPrompt,
                messages,
                execution.getConversationId(),
                knowledgeContext);

        return runLoop(request, availableTools, toolsByKey, messages, toolResults, 0);
    }

    private ExecuteResponse runLoop(
            OrchestrationRequest request,
            List<RuntimeToolSpec> availableTools,
            Map<String, ToolDefinition> toolsByKey,
            List<RuntimeMessage> messages,
            List<RuntimeToolResultMessage> initialToolResults,
            int startingRound) {
        List<RuntimeToolResultMessage> toolResults = new ArrayList<>(initialToolResults);
        int round = startingRound;

        while (true) {
            ExecuteResponse cancelled = cancelledResponseIfNeeded(
                    request.executionId(), request.renderedPrompt());
            if (cancelled != null) {
                return cancelled;
            }

            RuntimeTurnResult turn;
            try {
                turn = agentRuntimeClient.execute(new ExecutionRequest(
                        request.user().getOrganizationId(),
                        request.projectId(),
                        request.agentId(),
                        request.executionId(),
                        request.agent().getModelProvider(),
                        null,
                        request.renderedPrompt(),
                        messages,
                        request.conversationId(),
                        availableTools,
                        toolResults,
                        request.knowledgeContext()));
            } catch (RuntimeException ex) {
                log.warn(
                        "Execution {} runtime call failed (details omitted from persistence)",
                        request.executionId());
                AgentExecution failed = lifecycleService.failIfRunning(request.executionId());
                return executionMapper.toExecuteResponse(
                        failed, null, request.renderedPrompt(), toCitationDtos(request.knowledgeContext()));
            }

            if (turn.isFinal()) {
                RuntimeFinalResponse finalResponse = turn.finalResponse();
                AgentExecution completed =
                        lifecycleService.completeIfRunning(request.executionId(), finalResponse);
                String responseText = completed.getStatus() == ExecutionStatus.COMPLETED
                        ? finalResponse.responseText()
                        : null;
                return executionMapper.toExecuteResponse(
                        completed,
                        responseText,
                        request.renderedPrompt(),
                        false,
                        null,
                        toCitationDtos(request.knowledgeContext()),
                        turn.modelMetadata());
            }

            if (!turn.isToolCalls()) {
                AgentExecution failed = lifecycleService.failIfRunning(
                        request.executionId(), "TOOL_EXECUTION_FAILED");
                return executionMapper.toExecuteResponse(failed, null, request.renderedPrompt());
            }

            toolResults = new ArrayList<>();
            RuntimeToolCallBatch batch = turn.toolCallBatch();
            for (RuntimeToolCallRequest call : batch.toolCalls()) {
                cancelled = cancelledResponseIfNeeded(request.executionId(), request.renderedPrompt());
                if (cancelled != null) {
                    return cancelled;
                }

                ToolProcessOutcome outcome = processToolCall(
                        call,
                        toolsByKey,
                        request.executionId(),
                        request.agent(),
                        request.user(),
                        request.projectId(),
                        request.agentId(),
                        request.conversationId(),
                        request.knowledgeContext());
                if (outcome.awaitingApproval()) {
                    return outcome.response();
                }
                if (outcome.failed()) {
                    return outcome.response();
                }
                if (outcome.cancelled()) {
                    return outcome.response();
                }
                toolResults.add(outcome.result());
            }

            round++;
            if (round > toolProperties.getMaxOrchestrationRounds()) {
                AgentExecution failed = lifecycleService.failIfRunning(
                        request.executionId(), "TOOL_ORCHESTRATION_LIMIT_EXCEEDED");
                return executionMapper.toExecuteResponse(failed, null, request.renderedPrompt());
            }
        }
    }

    private ToolProcessOutcome processToolCall(
            RuntimeToolCallRequest call,
            Map<String, ToolDefinition> toolsByKey,
            UUID executionId,
            Agent agent,
            AuthenticatedUser user,
            UUID projectId,
            UUID agentId,
            UUID conversationId,
            RuntimeKnowledgeContext knowledgeContext) {
        Optional<ExecutionToolCall> existing =
                toolCallService.findByExecutionAndRuntimeCallId(executionId, call.runtimeCallId());
        if (existing.isPresent()) {
            return handleExistingToolCall(
                    existing.get(), call, user, projectId, conversationId, knowledgeContext);
        }

        ToolDefinition tool = toolsByKey.get(call.toolKey());
        if (tool == null) {
            return failToolExecution(
                    executionId,
                    call,
                    toolsByKey.containsKey(call.toolKey()) ? "TOOL_NOT_ASSIGNED" : "TOOL_NOT_FOUND",
                    user,
                    projectId,
                    agentId,
                    conversationId,
                    null);
        }
        if (tool.getStatus() != ToolStatus.ACTIVE || !isAssignmentEnabled(projectId, agentId, tool.getId(), user.getOrganizationId())) {
            return failToolExecution(
                    executionId, call, "TOOL_NOT_ASSIGNED", user, projectId, agentId, conversationId, tool);
        }

        JsonNode inputSchema;
        try {
            inputSchema = objectMapper.readTree(tool.getInputSchema());
            inputValidator.validate(inputSchema, call.arguments());
        } catch (ToolValidationException ex) {
            return failToolExecution(
                    executionId, call, ex.getCode(), user, projectId, agentId, conversationId, tool);
        } catch (Exception ex) {
            return failToolExecution(
                    executionId, call, "TOOL_INPUT_INVALID", user, projectId, agentId, conversationId, tool);
        }

        String inputPayload = call.arguments().toString();
        ExecutionToolCall toolCallRecord;
        try {
            toolCallRecord = toolCallService.createRequestedToolCall(
                    user.getOrganizationId(),
                    projectId,
                    agentId,
                    executionId,
                    conversationId,
                    tool,
                    call.runtimeCallId(),
                    inputPayload,
                    user.getUserId());
        } catch (DataIntegrityViolationException ex) {
            Optional<ExecutionToolCall> raced = toolCallService.findByExecutionAndRuntimeCallId(
                    executionId, call.runtimeCallId());
            if (raced.isPresent()) {
                return handleExistingToolCall(
                        raced.get(), call, user, projectId, conversationId, knowledgeContext);
            }
            throw ex;
        }

        if (toolCallRecord.getStatus() == ToolCallStatus.APPROVAL_REQUIRED) {
            AgentExecution execution = executionRepository.findById(executionId).orElseThrow();
            ExecuteResponse response = executionMapper.toExecuteResponse(
                    execution,
                    null,
                    null,
                    true,
                    toolCallRecord.getId(),
                    toCitationDtos(knowledgeContext));
            return ToolProcessOutcome.awaitingApproval(response);
        }

        return executeToolCallRecord(
                toolCallRecord, tool, call, agent, user, projectId, agentId, conversationId);
    }

    private ToolProcessOutcome processApprovedCall(
            ExecutionToolCall toolCallRecord,
            Map<String, ToolDefinition> toolsByKey,
            AgentExecution execution,
            Agent agent,
            AuthenticatedUser user,
            UUID projectId,
            UUID conversationId) {
        Optional<RuntimeToolResultMessage> existing = toStoredResult(toolCallRecord);
        if (existing.isPresent()) {
            return ToolProcessOutcome.success(existing.get());
        }

        ToolDefinition tool = toolsByKey.get(toolCallRecord.getToolKey());
        if (tool == null || tool.getStatus() != ToolStatus.ACTIVE) {
            RuntimeToolCallRequest call = new RuntimeToolCallRequest(
                    toolCallRecord.getRuntimeCallId(),
                    toolCallRecord.getToolKey(),
                    parseArguments(toolCallRecord.getInputPayload()));
            return failToolExecution(
                    execution.getId(),
                    call,
                    "TOOL_NOT_ASSIGNED",
                    user,
                    projectId,
                    execution.getAgentId(),
                    conversationId,
                    tool);
        }

        RuntimeToolCallRequest call = new RuntimeToolCallRequest(
                toolCallRecord.getRuntimeCallId(),
                toolCallRecord.getToolKey(),
                parseArguments(toolCallRecord.getInputPayload()));
        return executeToolCallRecord(
                toolCallRecord, tool, call, agent, user, projectId, execution.getAgentId(), conversationId);
    }

    private ToolProcessOutcome executeToolCallRecord(
            ExecutionToolCall toolCallRecord,
            ToolDefinition tool,
            RuntimeToolCallRequest call,
            Agent agent,
            AuthenticatedUser user,
            UUID projectId,
            UUID agentId,
            UUID conversationId) {
        UUID executionId = toolCallRecord.getExecutionId();
        ExecuteResponse cancelled = cancelledResponseIfNeeded(executionId, null);
        if (cancelled != null) {
            return ToolProcessOutcome.cancelled(cancelled);
        }

        toolCallService.markRunning(
                toolCallRecord.getId(), user.getOrganizationId(), projectId, user.getUserId());

        long startNanos = System.nanoTime();
        int timeoutSeconds = Math.min(tool.getMaxExecutionSeconds(), toolProperties.getMaximumTimeoutSeconds());
        ToolExecutionResult executionResult;
        try {
            ToolExecutor executor = executorRegistry.require(tool.getExecutorKey());
            ToolExecutionContext context = new ToolExecutionContext(
                    user.getOrganizationId(),
                    projectId,
                    agentId,
                    executionId,
                    tool.getId(),
                    tool.getToolKey(),
                    tool.getExecutorKey(),
                    Math.min(tool.getMaxOutputCharacters(), toolProperties.getMaxOutputCharacters()),
                    timeoutSeconds);
            Future<ToolExecutionResult> future =
                    toolExecutionExecutor.submit(() -> executor.execute(context, call.arguments()));
            executionResult = future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            log.debug("Tool call {} timed out for execution {}", toolCallRecord.getId(), executionId);
            long durationMs = elapsedMs(startNanos);
            toolCallService.fail(
                    toolCallRecord.getId(),
                    user.getOrganizationId(),
                    projectId,
                    "TOOL_EXECUTION_FAILED",
                    durationMs,
                    user.getUserId());
            AgentExecution failed = lifecycleService.failIfRunning(executionId, "TOOL_EXECUTION_FAILED");
            return ToolProcessOutcome.failed(
                    executionMapper.toExecuteResponse(failed, null, null));
        } catch (Exception ex) {
            log.debug("Tool call {} failed for execution {}", toolCallRecord.getId(), executionId);
            long durationMs = elapsedMs(startNanos);
            toolCallService.fail(
                    toolCallRecord.getId(),
                    user.getOrganizationId(),
                    projectId,
                    "TOOL_EXECUTION_FAILED",
                    durationMs,
                    user.getUserId());
            AgentExecution failed = lifecycleService.failIfRunning(executionId, "TOOL_EXECUTION_FAILED");
            return ToolProcessOutcome.failed(
                    executionMapper.toExecuteResponse(failed, null, null));
        }

        long durationMs = elapsedMs(startNanos);
        if (executionResult.status() == ToolExecutionOutcome.SUCCESS) {
            String outputPayload = serializeOutput(executionResult.output(), tool, toolCallRecord, user);
            toolCallService.complete(
                    toolCallRecord.getId(),
                    user.getOrganizationId(),
                    projectId,
                    outputPayload,
                    durationMs,
                    user.getUserId());
            RuntimeToolResultMessage result = new RuntimeToolResultMessage(
                    call.runtimeCallId(),
                    call.toolKey(),
                    "COMPLETED",
                    executionResult.output(),
                    null);
            persistToolArtifacts(
                    executionId, conversationId, call.toolKey(), result, outputPayload, user, projectId);
            return ToolProcessOutcome.success(result);
        }

        toolCallService.fail(
                toolCallRecord.getId(),
                user.getOrganizationId(),
                projectId,
                sanitizeErrorCode(executionResult.errorCode()),
                durationMs,
                user.getUserId());
        RuntimeToolResultMessage result = new RuntimeToolResultMessage(
                call.runtimeCallId(),
                call.toolKey(),
                "FAILED",
                null,
                sanitizeErrorCode(executionResult.errorCode()));
        persistToolArtifacts(executionId, conversationId, call.toolKey(), result, null, user, projectId);

        AgentExecution failed = lifecycleService.failIfRunning(executionId, "TOOL_EXECUTION_FAILED");
        return ToolProcessOutcome.failed(executionMapper.toExecuteResponse(failed, null, null));
    }

    private ToolProcessOutcome handleExistingToolCall(
            ExecutionToolCall existing,
            RuntimeToolCallRequest call,
            AuthenticatedUser user,
            UUID projectId,
            UUID conversationId,
            RuntimeKnowledgeContext knowledgeContext) {
        if (existing.getStatus() == ToolCallStatus.COMPLETED) {
            return ToolProcessOutcome.success(toStoredResult(existing).orElseThrow());
        }
        if (existing.getStatus() == ToolCallStatus.FAILED) {
            AgentExecution failed = lifecycleService.failIfRunning(existing.getExecutionId(), "TOOL_EXECUTION_FAILED");
            return ToolProcessOutcome.failed(executionMapper.toExecuteResponse(
                    failed, null, null, toCitationDtos(knowledgeContext)));
        }
        if (existing.getStatus() == ToolCallStatus.APPROVAL_REQUIRED) {
            AgentExecution execution = executionRepository.findById(existing.getExecutionId()).orElseThrow();
            ExecuteResponse response = executionMapper.toExecuteResponse(
                    execution,
                    null,
                    null,
                    true,
                    existing.getId(),
                    toCitationDtos(knowledgeContext));
            return ToolProcessOutcome.awaitingApproval(response);
        }
        if (existing.getStatus() == ToolCallStatus.RUNNING) {
            AgentExecution execution = executionRepository.findById(existing.getExecutionId()).orElseThrow();
            return ToolProcessOutcome.failed(executionMapper.toExecuteResponse(
                    execution, null, null, toCitationDtos(knowledgeContext)));
        }
        return ToolProcessOutcome.success(new RuntimeToolResultMessage(
                call.runtimeCallId(), call.toolKey(), "FAILED", null, "TOOL_EXECUTION_FAILED"));
    }

    private ExecuteResponse withCitations(ExecuteResponse response, RuntimeKnowledgeContext knowledgeContext) {
        if (response == null) {
            return null;
        }
        List<KnowledgeCitationResponse> citations = toCitationDtos(knowledgeContext);
        if (citations.isEmpty()
                || (response.citations() != null && !response.citations().isEmpty())) {
            return response;
        }
        return new ExecuteResponse(
                response.executionId(),
                response.status(),
                response.response(),
                response.latencyMs(),
                response.tokens(),
                response.renderedPrompt(),
                response.errorMessage(),
                response.awaitingApproval(),
                response.pendingToolCallId(),
                citations,
                response.model());
    }

    private ToolProcessOutcome failToolExecution(
            UUID executionId,
            RuntimeToolCallRequest call,
            String errorCode,
            AuthenticatedUser user,
            UUID projectId,
            UUID agentId,
            UUID conversationId,
            ToolDefinition tool) {
        if (tool != null) {
            try {
                ExecutionToolCall record = toolCallService.createRequestedToolCall(
                        user.getOrganizationId(),
                        projectId,
                        agentId,
                        executionId,
                        conversationId,
                        tool,
                        call.runtimeCallId(),
                        call.arguments().toString(),
                        user.getUserId());
                toolCallService.fail(
                        record.getId(),
                        user.getOrganizationId(),
                        projectId,
                        sanitizeErrorCode(errorCode),
                        0L,
                        user.getUserId());
            } catch (DataIntegrityViolationException ignored) {
                log.debug("Duplicate tool call record for runtimeCallId {}", call.runtimeCallId());
            }
        }
        AgentExecution failed = lifecycleService.failIfRunning(executionId, sanitizeErrorCode(errorCode));
        return ToolProcessOutcome.failed(executionMapper.toExecuteResponse(failed, null, null));
    }

    private void persistToolArtifacts(
            UUID executionId,
            UUID conversationId,
            String toolKey,
            RuntimeToolResultMessage result,
            String outputPayload,
            AuthenticatedUser user,
            UUID projectId) {
        String conciseJson = buildConciseToolMessage(toolKey, result, outputPayload);
        lifecycleService.persistToolMessage(executionId, conciseJson);
        if (conversationId != null && conversationProperties.isStoreToolMessages()) {
            conversationService.appendToolMessage(projectId, conversationId, executionId, conciseJson, user);
        }
    }

    private String buildConciseToolMessage(
            String toolKey, RuntimeToolResultMessage result, String outputPayload) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("toolKey", toolKey);
        node.put("status", result.status());
        if ("COMPLETED".equals(result.status()) && outputPayload != null) {
            try {
                node.set("output", objectMapper.readTree(outputPayload));
            } catch (Exception ex) {
                node.put("output", outputPayload);
            }
        }
        if (result.errorCode() != null) {
            node.put("errorCode", result.errorCode());
        }
        return node.toString();
    }

    private String serializeOutput(
            JsonNode output, ToolDefinition tool, ExecutionToolCall toolCallRecord, AuthenticatedUser user) {
        String serialized = output != null ? output.toString() : "{}";
        int maxChars = Math.min(tool.getMaxOutputCharacters(), toolProperties.getMaxOutputCharacters());
        if (serialized.length() > maxChars) {
            serialized = serialized.substring(0, maxChars);
            auditService.toolOutputTruncated(
                    new ToolCallContext(
                            toolCallRecord.getOrganizationId(),
                            toolCallRecord.getProjectId(),
                            tool.getId(),
                            toolCallRecord.getAgentId(),
                            toolCallRecord.getExecutionId(),
                            toolCallRecord.getId(),
                            tool.getToolKey(),
                            toolCallRecord.getRuntimeCallId(),
                            toolCallRecord.getSequenceNumber(),
                            ToolCallStatus.COMPLETED.name()),
                    user.getUserId());
        }
        return serialized;
    }

    private Optional<RuntimeToolResultMessage> toStoredResult(ExecutionToolCall toolCall) {
        if (toolCall.getStatus() == ToolCallStatus.COMPLETED) {
            JsonNode output = parseArguments(toolCall.getOutputPayload());
            return Optional.of(new RuntimeToolResultMessage(
                    toolCall.getRuntimeCallId(),
                    toolCall.getToolKey(),
                    "COMPLETED",
                    output,
                    null));
        }
        if (toolCall.getStatus() == ToolCallStatus.FAILED) {
            return Optional.of(new RuntimeToolResultMessage(
                    toolCall.getRuntimeCallId(),
                    toolCall.getToolKey(),
                    "FAILED",
                    null,
                    toolCall.getErrorCode()));
        }
        return Optional.empty();
    }

    private List<RuntimeToolSpec> toRuntimeToolSpecs(List<ToolDefinition> tools) {
        List<RuntimeToolSpec> specs = new ArrayList<>();
        for (ToolDefinition tool : tools) {
            try {
                JsonNode schema = objectMapper.readTree(tool.getInputSchema());
                specs.add(new RuntimeToolSpec(
                        tool.getToolKey(), tool.getName(), tool.getDescription(), schema));
            } catch (Exception ex) {
                log.debug("Skipping tool {} due to invalid input schema", tool.getToolKey());
            }
        }
        return specs;
    }

    private List<RuntimeMessage> loadRuntimeMessages(UUID executionId) {
        return executionMessageRepository.findByExecutionIdOrderByCreatedAtAsc(executionId).stream()
                .filter(message -> message.getRole() != MessageRole.SYSTEM
                        || conversationProperties.isStoreSystemMessage())
                .map(message -> new RuntimeMessage(message.getRole().name(), message.getContent()))
                .toList();
    }

    private ExecuteResponse cancelledResponseIfNeeded(UUID executionId, String renderedPrompt) {
        AgentExecution execution = executionRepository.findById(executionId).orElse(null);
        if (execution == null || execution.getStatus() != ExecutionStatus.CANCELLED) {
            return null;
        }
        return executionMapper.toExecuteResponse(execution, null, renderedPrompt);
    }

    private AgentExecution requireRunningExecution(UUID projectId, UUID executionId, UUID organizationId) {
        AgentExecution execution = executionRepository
                .findByIdAndProjectIdAndOrganizationId(executionId, projectId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "EXECUTION_NOT_FOUND", "Execution not found"));
        if (execution.getStatus() == ExecutionStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "EXECUTION_CANCELLED", "Execution was cancelled");
        }
        if (execution.getStatus() == ExecutionStatus.FAILED
                || execution.getStatus() == ExecutionStatus.COMPLETED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EXECUTION_NOT_RUNNING",
                    "Execution is not running");
        }
        if (execution.getStatus() != ExecutionStatus.RUNNING) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EXECUTION_NOT_RUNNING",
                    "Execution is not running");
        }
        return execution;
    }

    private boolean isAssignmentEnabled(UUID projectId, UUID agentId, UUID toolId, UUID organizationId) {
        return assignmentRepository
                .findByAgentIdAndToolIdAndProjectIdAndOrganizationId(agentId, toolId, projectId, organizationId)
                .map(assignment -> assignment.isEnabled())
                .orElse(false);
    }

    private JsonNode parseArguments(String payload) {
        if (payload == null || payload.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(payload);
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private static String sanitizeErrorCode(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return "TOOL_EXECUTION_FAILED";
        }
        return errorCode.trim();
    }

    private record ToolProcessOutcome(
            RuntimeToolResultMessage result,
            ExecuteResponse response,
            boolean awaitingApproval,
            boolean failed,
            boolean cancelled) {

        static ToolProcessOutcome success(RuntimeToolResultMessage result) {
            return new ToolProcessOutcome(result, null, false, false, false);
        }

        static ToolProcessOutcome awaitingApproval(ExecuteResponse response) {
            return new ToolProcessOutcome(null, response, true, false, false);
        }

        static ToolProcessOutcome failed(ExecuteResponse response) {
            return new ToolProcessOutcome(null, response, false, true, false);
        }

        static ToolProcessOutcome cancelled(ExecuteResponse response) {
            return new ToolProcessOutcome(null, response, false, false, true);
        }
    }
}
