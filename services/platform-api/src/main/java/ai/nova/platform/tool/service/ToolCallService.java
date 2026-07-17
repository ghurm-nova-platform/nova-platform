package ai.nova.platform.tool.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ai.nova.platform.execution.entity.AgentExecution;
import ai.nova.platform.execution.entity.ExecutionStatus;
import ai.nova.platform.execution.repository.AgentExecutionRepository;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.tool.config.ToolProperties;
import ai.nova.platform.tool.dto.ToolDtos.ExecutionContinueResponse;
import ai.nova.platform.tool.dto.ToolDtos.ToolCallApproveRequest;
import ai.nova.platform.tool.dto.ToolDtos.ToolCallRejectRequest;
import ai.nova.platform.tool.dto.ToolDtos.ToolCallResponse;
import ai.nova.platform.tool.entity.ExecutionToolCall;
import ai.nova.platform.tool.entity.ToolCallStatus;
import ai.nova.platform.tool.entity.ToolDefinition;
import ai.nova.platform.tool.mapper.ToolMapper;
import ai.nova.platform.tool.repository.ExecutionToolCallRepository;
import ai.nova.platform.tool.security.ToolAuthorizationService;
import ai.nova.platform.tool.service.ToolAuditService.ToolCallContext;
import ai.nova.platform.web.error.ApiException;

@Service
public class ToolCallService {

    private final ExecutionToolCallRepository toolCallRepository;
    private final AgentExecutionRepository executionRepository;
    private final ProjectRepository projectRepository;
    private final ToolMapper toolMapper;
    private final ToolAuthorizationService authorizationService;
    private final ToolAuditService auditService;
    private final ToolProperties toolProperties;

    public ToolCallService(
            ExecutionToolCallRepository toolCallRepository,
            AgentExecutionRepository executionRepository,
            ProjectRepository projectRepository,
            ToolMapper toolMapper,
            ToolAuthorizationService authorizationService,
            ToolAuditService auditService,
            ToolProperties toolProperties) {
        this.toolCallRepository = toolCallRepository;
        this.executionRepository = executionRepository;
        this.projectRepository = projectRepository;
        this.toolMapper = toolMapper;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.toolProperties = toolProperties;
    }

    @Transactional(readOnly = true)
    public List<ToolCallResponse> list(UUID projectId, UUID executionId, AuthenticatedUser user) {
        authorizationService.require(user, ToolAuthorizationService.TOOL_CALL_READ);
        requireExecution(projectId, executionId, user.getOrganizationId());
        return toolCallRepository
                .findByExecutionIdAndProjectIdAndOrganizationIdOrderBySequenceNumberAsc(
                        executionId, projectId, user.getOrganizationId())
                .stream()
                .map(toolMapper::toToolCallResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ToolCallResponse get(UUID projectId, UUID executionId, UUID toolCallId, AuthenticatedUser user) {
        authorizationService.require(user, ToolAuthorizationService.TOOL_CALL_READ);
        requireExecution(projectId, executionId, user.getOrganizationId());
        ExecutionToolCall toolCall = requireToolCall(projectId, executionId, toolCallId, user.getOrganizationId());
        return toolMapper.toToolCallResponse(toolCall);
    }

    @Transactional
    public ToolCallResponse approve(
            UUID projectId,
            UUID executionId,
            UUID toolCallId,
            ToolCallApproveRequest request,
            AuthenticatedUser user) {
        authorizationService.require(user, ToolAuthorizationService.TOOL_CALL_APPROVE);
        AgentExecution execution = requireExecution(projectId, executionId, user.getOrganizationId());
        requireExecutionRunning(execution);
        ExecutionToolCall toolCall = requireToolCall(projectId, executionId, toolCallId, user.getOrganizationId());

        if (toolCall.getStatus() != ToolCallStatus.APPROVAL_REQUIRED) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "TOOL_CALL_NOT_PENDING", "Tool call is not awaiting approval");
        }

        Instant now = Instant.now();
        toolCall.setStatus(ToolCallStatus.APPROVED);
        toolCall.setApprovedBy(user.getUserId());
        toolCall.setApprovedAt(now);
        ExecutionToolCall saved = toolCallRepository.save(toolCall);

        auditService.toolCallApproved(toAuditContext(saved), user.getUserId());
        return toolMapper.toToolCallResponse(saved);
    }

    @Transactional
    public ToolCallResponse reject(
            UUID projectId,
            UUID executionId,
            UUID toolCallId,
            ToolCallRejectRequest request,
            AuthenticatedUser user) {
        authorizationService.require(user, ToolAuthorizationService.TOOL_CALL_APPROVE);
        AgentExecution execution = requireExecution(projectId, executionId, user.getOrganizationId());
        requireExecutionRunning(execution);
        ExecutionToolCall toolCall = requireToolCall(projectId, executionId, toolCallId, user.getOrganizationId());

        if (toolCall.getStatus() != ToolCallStatus.APPROVAL_REQUIRED) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "TOOL_CALL_NOT_PENDING", "Tool call is not awaiting approval");
        }

        Instant now = Instant.now();
        toolCall.setStatus(ToolCallStatus.REJECTED);
        toolCall.setErrorCode(request.reasonCode().trim());
        toolCall.setApprovedBy(user.getUserId());
        toolCall.setApprovedAt(now);
        toolCall.setCompletedAt(now);
        ExecutionToolCall saved = toolCallRepository.save(toolCall);

        auditService.toolCallRejected(toAuditContext(saved), request.reasonCode().trim(), user.getUserId());
        return toolMapper.toToolCallResponse(saved);
    }

    /**
     * Hook point for ToolCallingOrchestrator to resume execution after approvals complete.
     */
    @Transactional(readOnly = true)
    public ExecutionContinueResponse continueExecution(UUID projectId, UUID executionId, AuthenticatedUser user) {
        authorizationService.require(user, ToolAuthorizationService.TOOL_EXECUTE);
        requireExecution(projectId, executionId, user.getOrganizationId());

        long pendingApprovals = toolCallRepository.countByExecutionIdAndStatusIn(
                executionId, List.of(ToolCallStatus.APPROVAL_REQUIRED));
        if (pendingApprovals > 0) {
            return new ExecutionContinueResponse(executionId, false, "Pending tool call approvals");
        }

        long blockingCalls = toolCallRepository.countByExecutionIdAndStatusIn(
                executionId,
                List.of(ToolCallStatus.REQUESTED, ToolCallStatus.APPROVED, ToolCallStatus.RUNNING));
        if (blockingCalls > 0) {
            return new ExecutionContinueResponse(executionId, false, "Tool calls still in progress");
        }

        return new ExecutionContinueResponse(executionId, true, "Ready to continue execution");
    }

    /**
     * Creates a tool call record in REQUESTED or APPROVAL_REQUIRED state.
     * Intended for use by ToolCallingOrchestrator.
     */
    @Transactional
    public ExecutionToolCall createRequestedToolCall(
            UUID organizationId,
            UUID projectId,
            UUID agentId,
            UUID executionId,
            UUID conversationId,
            ToolDefinition tool,
            String runtimeCallId,
            String inputPayload,
            UUID createdBy) {
        if (!toolProperties.isEnabled()) {
            throw new ApiException(HttpStatus.CONFLICT, "TOOLS_DISABLED", "Tool calling is disabled");
        }
        if (inputPayload != null && inputPayload.length() > toolProperties.getMaxInputCharacters()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TOOL_INPUT_TOO_LONG", "Tool input exceeds maximum length");
        }

        int nextSequence = toolCallRepository.findMaxSequenceNumber(executionId) + 1;
        if (nextSequence > toolProperties.getMaxToolCallsPerExecution()) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "TOOL_CALL_LIMIT_EXCEEDED", "Maximum tool calls per execution exceeded");
        }

        ToolCallStatus initialStatus =
                tool.isRequiresApproval() ? ToolCallStatus.APPROVAL_REQUIRED : ToolCallStatus.REQUESTED;
        Instant now = Instant.now();
        ExecutionToolCall toolCall = new ExecutionToolCall(
                UUID.randomUUID(),
                organizationId,
                projectId,
                agentId,
                executionId,
                conversationId,
                tool.getId(),
                tool.getToolKey(),
                runtimeCallId,
                nextSequence,
                initialStatus,
                inputPayload,
                createdBy,
                now);
        ExecutionToolCall saved = toolCallRepository.save(toolCall);
        auditService.toolCallRequested(toAuditContext(saved), createdBy);
        return saved;
    }

    @Transactional
    public ExecutionToolCall markRunning(UUID toolCallId, UUID organizationId, UUID projectId, UUID performedBy) {
        ExecutionToolCall toolCall = requireToolCallById(toolCallId, projectId, organizationId);
        if (toolCall.getStatus() != ToolCallStatus.REQUESTED && toolCall.getStatus() != ToolCallStatus.APPROVED) {
            throw new ApiException(HttpStatus.CONFLICT, "TOOL_CALL_INVALID_STATE", "Tool call cannot be started");
        }
        toolCall.setStatus(ToolCallStatus.RUNNING);
        toolCall.setStartedAt(Instant.now());
        ExecutionToolCall saved = toolCallRepository.save(toolCall);
        auditService.toolCallStarted(toAuditContext(saved), performedBy);
        return saved;
    }

    @Transactional
    public ExecutionToolCall complete(
            UUID toolCallId,
            UUID organizationId,
            UUID projectId,
            String outputPayload,
            long durationMs,
            UUID performedBy) {
        ExecutionToolCall toolCall = requireToolCallById(toolCallId, projectId, organizationId);
        if (toolCall.getStatus() != ToolCallStatus.RUNNING) {
            throw new ApiException(HttpStatus.CONFLICT, "TOOL_CALL_INVALID_STATE", "Tool call is not running");
        }
        Instant now = Instant.now();
        toolCall.setStatus(ToolCallStatus.COMPLETED);
        toolCall.setOutputPayload(outputPayload);
        toolCall.setCompletedAt(now);
        toolCall.setDurationMs(durationMs);
        ExecutionToolCall saved = toolCallRepository.save(toolCall);
        auditService.toolCallCompleted(toAuditContext(saved), durationMs, performedBy);
        return saved;
    }

    @Transactional
    public ExecutionToolCall fail(
            UUID toolCallId,
            UUID organizationId,
            UUID projectId,
            String errorCode,
            long durationMs,
            UUID performedBy) {
        ExecutionToolCall toolCall = requireToolCallById(toolCallId, projectId, organizationId);
        if (toolCall.getStatus() != ToolCallStatus.RUNNING && toolCall.getStatus() != ToolCallStatus.REQUESTED
                && toolCall.getStatus() != ToolCallStatus.APPROVED) {
            throw new ApiException(HttpStatus.CONFLICT, "TOOL_CALL_INVALID_STATE", "Tool call cannot be failed");
        }
        Instant now = Instant.now();
        toolCall.setStatus(ToolCallStatus.FAILED);
        toolCall.setErrorCode(sanitizeErrorCode(errorCode));
        toolCall.setCompletedAt(now);
        toolCall.setDurationMs(durationMs);
        ExecutionToolCall saved = toolCallRepository.save(toolCall);
        auditService.toolCallFailed(toAuditContext(saved), sanitizeErrorCode(errorCode), durationMs, performedBy);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<ExecutionToolCall> findByExecutionAndRuntimeCallId(UUID executionId, String runtimeCallId) {
        return toolCallRepository.findByExecutionIdAndRuntimeCallId(executionId, runtimeCallId);
    }

    @Transactional(readOnly = true)
    public List<ExecutionToolCall> findByExecutionAndStatuses(
            UUID executionId, UUID projectId, UUID organizationId, List<ToolCallStatus> statuses) {
        return toolCallRepository
                .findByExecutionIdAndProjectIdAndOrganizationIdOrderBySequenceNumberAsc(
                        executionId, projectId, organizationId)
                .stream()
                .filter(call -> statuses.contains(call.getStatus()))
                .toList();
    }

    private void requireExecutionRunning(AgentExecution execution) {
        if (execution.getStatus() == ExecutionStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "EXECUTION_CANCELLED", "Execution was cancelled");
        }
    }

    private AgentExecution requireExecution(UUID projectId, UUID executionId, UUID organizationId) {
        requireProject(projectId, organizationId);
        return executionRepository
                .findByIdAndProjectIdAndOrganizationId(executionId, projectId, organizationId)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND, "EXECUTION_NOT_FOUND", "Execution not found"));
    }

    private ExecutionToolCall requireToolCall(
            UUID projectId, UUID executionId, UUID toolCallId, UUID organizationId) {
        ExecutionToolCall toolCall = requireToolCallById(toolCallId, projectId, organizationId);
        if (!toolCall.getExecutionId().equals(executionId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "TOOL_CALL_NOT_FOUND", "Tool call not found");
        }
        return toolCall;
    }

    private ExecutionToolCall requireToolCallById(UUID toolCallId, UUID projectId, UUID organizationId) {
        return toolCallRepository
                .findByIdAndProjectIdAndOrganizationId(toolCallId, projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TOOL_CALL_NOT_FOUND", "Tool call not found"));
    }

    private void requireProject(UUID projectId, UUID organizationId) {
        projectRepository
                .findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));
    }

    private ToolCallContext toAuditContext(ExecutionToolCall toolCall) {
        return new ToolCallContext(
                toolCall.getOrganizationId(),
                toolCall.getProjectId(),
                toolCall.getToolId(),
                toolCall.getAgentId(),
                toolCall.getExecutionId(),
                toolCall.getId(),
                toolCall.getToolKey(),
                toolCall.getRuntimeCallId(),
                toolCall.getSequenceNumber(),
                toolCall.getStatus().name());
    }

    private String sanitizeErrorCode(String errorCode) {
        if (!StringUtils.hasText(errorCode)) {
            return "TOOL_EXECUTION_FAILED";
        }
        String trimmed = errorCode.trim();
        if (trimmed.length() > 100) {
            return trimmed.substring(0, 100);
        }
        return trimmed;
    }
}
