package ai.nova.platform.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.nova.platform.agent.entity.Agent;
import ai.nova.platform.agent.entity.AgentStatus;
import ai.nova.platform.agent.entity.AgentVisibility;
import ai.nova.platform.agent.repository.AgentRepository;
import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.agent.runtime.ExecutionRequest;
import ai.nova.platform.agent.runtime.RuntimeFinalResponse;
import ai.nova.platform.agent.runtime.RuntimeMessage;
import ai.nova.platform.agent.runtime.RuntimeToolCallBatch;
import ai.nova.platform.agent.runtime.RuntimeToolCallRequest;
import ai.nova.platform.agent.runtime.RuntimeToolSpec;
import ai.nova.platform.agent.runtime.RuntimeTurnResult;
import ai.nova.platform.conversation.service.ConversationService;
import ai.nova.platform.conversation.validation.ConversationProperties;
import ai.nova.platform.execution.dto.ExecutionDtos.ExecuteResponse;
import ai.nova.platform.execution.entity.AgentExecution;
import ai.nova.platform.execution.entity.ExecutionStatus;
import ai.nova.platform.execution.mapper.ExecutionMapper;
import ai.nova.platform.execution.repository.AgentExecutionRepository;
import ai.nova.platform.execution.repository.ExecutionMessageRepository;
import ai.nova.platform.execution.service.ExecutionLifecycleService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.tool.config.ToolProperties;
import ai.nova.platform.tool.entity.ExecutionToolCall;
import ai.nova.platform.tool.entity.ToolCallStatus;
import ai.nova.platform.tool.entity.ToolDefinition;
import ai.nova.platform.tool.entity.ToolStatus;
import ai.nova.platform.tool.entity.ToolType;
import ai.nova.platform.tool.executor.ToolExecutor;
import ai.nova.platform.tool.executor.ToolExecutorRegistry;
import ai.nova.platform.tool.repository.AgentToolAssignmentRepository;
import ai.nova.platform.tool.service.AgentToolAssignmentService;
import ai.nova.platform.tool.service.ToolAuditService;
import ai.nova.platform.tool.service.ToolCallService;
import ai.nova.platform.tool.service.ToolCallingOrchestrator;
import ai.nova.platform.tool.service.ToolCallingOrchestrator.OrchestrationRequest;
import ai.nova.platform.tool.validation.ToolInputValidator;

@ExtendWith(MockitoExtension.class)
class ToolCallingOrchestratorTest {

    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PROJECT_ID = UUID.fromString("55555555-5555-5555-5555-555555555501");
    private static final UUID AGENT_ID = UUID.fromString("66666666-6666-6666-6666-666666666601");
    private static final UUID EXECUTION_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");

    @Mock
    private AgentRuntimeClient agentRuntimeClient;
    @Mock
    private AgentToolAssignmentService agentToolAssignmentService;
    @Mock
    private AgentToolAssignmentRepository assignmentRepository;
    @Mock
    private ToolCallService toolCallService;
    @Mock
    private ToolExecutorRegistry executorRegistry;
    @Mock
    private ToolAuditService auditService;
    @Mock
    private ExecutionLifecycleService lifecycleService;
    @Mock
    private ExecutionMapper executionMapper;
    @Mock
    private AgentExecutionRepository executionRepository;
    @Mock
    private ExecutionMessageRepository executionMessageRepository;
    @Mock
    private AgentRepository agentRepository;
    @Mock
    private ConversationService conversationService;

    private ToolCallingOrchestrator orchestrator;
    private ObjectMapper objectMapper;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        executorService = Executors.newFixedThreadPool(2);
        ToolProperties toolProperties = new ToolProperties();
        toolProperties.setMaxOrchestrationRounds(5);
        ConversationProperties conversationProperties = new ConversationProperties();
        orchestrator = new ToolCallingOrchestrator(
                agentRuntimeClient,
                agentToolAssignmentService,
                assignmentRepository,
                toolCallService,
                executorRegistry,
                new ToolInputValidator(),
                toolProperties,
                auditService,
                lifecycleService,
                executionMapper,
                executionRepository,
                executionMessageRepository,
                agentRepository,
                conversationService,
                conversationProperties,
                objectMapper,
                executorService);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void finalRuntimeResponseCompletesExecution() {
        Agent agent = activeAgent();
        AuthenticatedUser user = user();
        when(agentToolAssignmentService.loadAssignedActiveTools(PROJECT_ID, AGENT_ID, ORG_ID))
                .thenReturn(List.of());
        when(executionRepository.findById(EXECUTION_ID)).thenReturn(Optional.of(runningExecution()));
        when(agentRuntimeClient.execute(any(ExecutionRequest.class)))
                .thenReturn(RuntimeTurnResult.finalResponse(
                        new RuntimeFinalResponse("hello", 1, 2, 3, 10L)));
        AgentExecution completed = runningExecution();
        completed.setStatus(ExecutionStatus.COMPLETED);
        when(lifecycleService.completeIfRunning(eq(EXECUTION_ID), any(RuntimeFinalResponse.class)))
                .thenReturn(completed);
        when(executionMapper.toExecuteResponse(completed, "hello", "prompt"))
                .thenReturn(new ExecuteResponse(
                        EXECUTION_ID, ExecutionStatus.COMPLETED, "hello", 10L, null, "prompt", null, null, null));

        ExecuteResponse response = orchestrator.orchestrate(new OrchestrationRequest(
                EXECUTION_ID, agent, user, PROJECT_ID, AGENT_ID, "prompt", List.of(), null));

        assertThat(response.status()).isEqualTo(ExecutionStatus.COMPLETED);
        assertThat(response.response()).isEqualTo("hello");
    }

    @Test
    void approvalRequiredStopsWithAwaitingApprovalFlag() throws Exception {
        Agent agent = activeAgent();
        AuthenticatedUser user = user();
        ToolDefinition tool = calculatorTool();
        when(agentToolAssignmentService.loadAssignedActiveTools(PROJECT_ID, AGENT_ID, ORG_ID))
                .thenReturn(List.of(tool));
        when(assignmentRepository.findByAgentIdAndToolIdAndProjectIdAndOrganizationId(
                        AGENT_ID, tool.getId(), PROJECT_ID, ORG_ID))
                .thenReturn(Optional.of(enabledAssignment()));
        when(executionRepository.findById(EXECUTION_ID)).thenReturn(Optional.of(runningExecution()));
        when(toolCallService.findByExecutionAndRuntimeCallId(EXECUTION_ID, "call-1"))
                .thenReturn(Optional.empty());

        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("operation", "ADD");
        arguments.put("left", 1);
        arguments.put("right", 2);
        when(agentRuntimeClient.execute(any(ExecutionRequest.class)))
                .thenReturn(RuntimeTurnResult.toolCalls(new RuntimeToolCallBatch(List.of(
                        new RuntimeToolCallRequest("call-1", "CALCULATOR", arguments)))));

        UUID toolCallId = UUID.randomUUID();
        ExecutionToolCall pending = new ExecutionToolCall(
                toolCallId,
                ORG_ID,
                PROJECT_ID,
                AGENT_ID,
                EXECUTION_ID,
                null,
                tool.getId(),
                "CALCULATOR",
                "call-1",
                1,
                ToolCallStatus.APPROVAL_REQUIRED,
                arguments.toString(),
                USER_ID,
                java.time.Instant.now());
        when(toolCallService.createRequestedToolCall(
                        any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(pending);
        when(executionMapper.toExecuteResponse(
                        any(AgentExecution.class), eq(null), eq(null), eq(true), eq(toolCallId)))
                .thenReturn(new ExecuteResponse(
                        EXECUTION_ID, ExecutionStatus.RUNNING, null, 0L, null, null, null, true, toolCallId));

        ExecuteResponse response = orchestrator.orchestrate(new OrchestrationRequest(
                EXECUTION_ID, agent, user, PROJECT_ID, AGENT_ID, "prompt", List.of(), null));

        assertThat(response.awaitingApproval()).isTrue();
        assertThat(response.pendingToolCallId()).isEqualTo(toolCallId);
        verify(lifecycleService, never()).completeIfRunning(eq(EXECUTION_ID), any(RuntimeFinalResponse.class));
    }

    @Test
    void completedDuplicateRuntimeCallIdReusesStoredResult() throws Exception {
        Agent agent = activeAgent();
        AuthenticatedUser user = user();
        ToolDefinition tool = calculatorTool();
        when(agentToolAssignmentService.loadAssignedActiveTools(PROJECT_ID, AGENT_ID, ORG_ID))
                .thenReturn(List.of(tool));

        ObjectNode output = objectMapper.createObjectNode().put("result", 3);
        ExecutionToolCall completedCall = new ExecutionToolCall(
                UUID.randomUUID(),
                ORG_ID,
                PROJECT_ID,
                AGENT_ID,
                EXECUTION_ID,
                null,
                tool.getId(),
                "CALCULATOR",
                "dup-1",
                1,
                ToolCallStatus.COMPLETED,
                "{}",
                USER_ID,
                java.time.Instant.now());
        completedCall.setOutputPayload(output.toString());

        when(executionRepository.findById(EXECUTION_ID)).thenReturn(Optional.of(runningExecution()));
        when(toolCallService.findByExecutionAndRuntimeCallId(EXECUTION_ID, "dup-1"))
                .thenReturn(Optional.of(completedCall));

        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("operation", "ADD");
        arguments.put("left", 1);
        arguments.put("right", 2);
        when(agentRuntimeClient.execute(any(ExecutionRequest.class)))
                .thenReturn(
                        RuntimeTurnResult.toolCalls(new RuntimeToolCallBatch(List.of(
                                new RuntimeToolCallRequest("dup-1", "CALCULATOR", arguments)))),
                        RuntimeTurnResult.finalResponse(new RuntimeFinalResponse("done", 1, 1, 2, 5L)));

        AgentExecution completed = runningExecution();
        completed.setStatus(ExecutionStatus.COMPLETED);
        when(lifecycleService.completeIfRunning(eq(EXECUTION_ID), any(RuntimeFinalResponse.class)))
                .thenReturn(completed);
        when(executionMapper.toExecuteResponse(completed, "done", "prompt"))
                .thenReturn(new ExecuteResponse(
                        EXECUTION_ID, ExecutionStatus.COMPLETED, "done", 5L, null, "prompt", null, null, null));

        ExecuteResponse response = orchestrator.orchestrate(new OrchestrationRequest(
                EXECUTION_ID,
                agent,
                user,
                PROJECT_ID,
                AGENT_ID,
                "prompt",
                List.of(new RuntimeMessage("USER", "test")),
                null));

        assertThat(response.status()).isEqualTo(ExecutionStatus.COMPLETED);
        verify(toolCallService, never())
                .createRequestedToolCall(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser(
                USER_ID, ORG_ID, "admin@nova.local", "Admin", List.of("USER"), List.of("TOOL_EXECUTE"), true);
    }

    private Agent activeAgent() {
        Agent agent = new Agent(
                AGENT_ID,
                ORG_ID,
                PROJECT_ID,
                "Demo",
                "desc",
                "system",
                "OPENAI",
                "gpt-4.1-mini",
                java.math.BigDecimal.valueOf(0.7),
                1024,
                AgentStatus.ACTIVE,
                AgentVisibility.PRIVATE,
                USER_ID,
                java.time.Instant.now());
        return agent;
    }

    private AgentExecution runningExecution() {
        AgentExecution execution = new AgentExecution(
                EXECUTION_ID,
                ORG_ID,
                PROJECT_ID,
                AGENT_ID,
                UUID.randomUUID(),
                null,
                "OPENAI",
                "gpt-4.1-mini",
                ExecutionStatus.RUNNING,
                USER_ID,
                java.time.Instant.now());
        return execution;
    }

    private ToolDefinition calculatorTool() throws Exception {
        return new ToolDefinition(
                UUID.randomUUID(),
                ORG_ID,
                PROJECT_ID,
                "CALCULATOR",
                "Calculator",
                "Adds",
                ToolType.BUILT_IN,
                "CALCULATOR",
                "{\"type\":\"object\",\"properties\":{\"operation\":{\"type\":\"string\",\"enum\":[\"ADD\",\"SUBTRACT\",\"MULTIPLY\",\"DIVIDE\"]},\"left\":{\"type\":\"number\"},\"right\":{\"type\":\"number\"}},\"required\":[\"operation\",\"left\",\"right\"],\"additionalProperties\":false}",
                null,
                ToolStatus.ACTIVE,
                true,
                5,
                5000,
                USER_ID,
                java.time.Instant.now());
    }

    private ai.nova.platform.tool.entity.AgentToolAssignment enabledAssignment() {
        return new ai.nova.platform.tool.entity.AgentToolAssignment(
                UUID.randomUUID(),
                ORG_ID,
                PROJECT_ID,
                AGENT_ID,
                UUID.randomUUID(),
                true,
                USER_ID,
                java.time.Instant.now());
    }
}
