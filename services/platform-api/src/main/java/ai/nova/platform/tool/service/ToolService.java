package ai.nova.platform.tool.service;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.project.Project;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.tool.config.ToolProperties;
import ai.nova.platform.tool.dto.ToolDtos.ToolCreateRequest;
import ai.nova.platform.tool.dto.ToolDtos.ToolResponse;
import ai.nova.platform.tool.dto.ToolDtos.ToolUpdateRequest;
import ai.nova.platform.tool.entity.ToolDefinition;
import ai.nova.platform.tool.entity.ToolStatus;
import ai.nova.platform.tool.entity.ToolType;
import ai.nova.platform.tool.executor.ToolExecutorRegistry;
import ai.nova.platform.tool.mapper.ToolMapper;
import ai.nova.platform.tool.repository.AgentToolAssignmentRepository;
import ai.nova.platform.tool.repository.ToolDefinitionRepository;
import ai.nova.platform.tool.security.ToolAuthorizationService;
import ai.nova.platform.tool.service.ToolAuditService.ToolDefinitionContext;
import ai.nova.platform.tool.validation.ToolSchemaValidator;
import ai.nova.platform.tool.validation.ToolValidationException;
import ai.nova.platform.web.error.ApiException;

@Service
public class ToolService {

    private static final Pattern TOOL_KEY_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    private final ToolDefinitionRepository toolRepository;
    private final AgentToolAssignmentRepository assignmentRepository;
    private final ProjectRepository projectRepository;
    private final ToolMapper toolMapper;
    private final ToolAuthorizationService authorizationService;
    private final ToolProperties toolProperties;
    private final ToolSchemaValidator schemaValidator;
    private final ToolExecutorRegistry executorRegistry;
    private final ToolAuditService auditService;
    private final ObjectMapper objectMapper;

    public ToolService(
            ToolDefinitionRepository toolRepository,
            AgentToolAssignmentRepository assignmentRepository,
            ProjectRepository projectRepository,
            ToolMapper toolMapper,
            ToolAuthorizationService authorizationService,
            ToolProperties toolProperties,
            ToolSchemaValidator schemaValidator,
            ToolExecutorRegistry executorRegistry,
            ToolAuditService auditService,
            ObjectMapper objectMapper) {
        this.toolRepository = toolRepository;
        this.assignmentRepository = assignmentRepository;
        this.projectRepository = projectRepository;
        this.toolMapper = toolMapper;
        this.authorizationService = authorizationService;
        this.toolProperties = toolProperties;
        this.schemaValidator = schemaValidator;
        this.executorRegistry = executorRegistry;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<ToolResponse> list(
            UUID projectId,
            AuthenticatedUser user,
            String search,
            ToolStatus status,
            ToolType type,
            Pageable pageable) {
        authorizationService.require(user, ToolAuthorizationService.TOOL_READ);
        requireProjectInOrganization(projectId, user.getOrganizationId());
        return toolRepository
                .search(user.getOrganizationId(), projectId, normalize(search), status, type, pageable)
                .map(toolMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ToolResponse get(UUID projectId, UUID toolId, AuthenticatedUser user) {
        authorizationService.require(user, ToolAuthorizationService.TOOL_READ);
        return toolMapper.toResponse(requireTool(projectId, toolId, user.getOrganizationId()));
    }

    @Transactional
    public ToolResponse create(UUID projectId, ToolCreateRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ToolAuthorizationService.TOOL_CREATE);
        Project project = requireProjectInOrganization(projectId, user.getOrganizationId());

        String toolKey = request.toolKey().trim();
        validateToolKey(toolKey);
        if (toolRepository.existsByProjectIdAndToolKeyIgnoreCase(projectId, toolKey)) {
            throw new ApiException(HttpStatus.CONFLICT, "TOOL_KEY_EXISTS", "Tool key already exists in this project");
        }

        String executorKey = request.executorKey().trim();
        requireRegisteredExecutor(executorKey);
        validateSchemas(request.inputSchema(), request.outputSchema());

        int maxExecutionSeconds = request.maxExecutionSeconds() != null
                ? request.maxExecutionSeconds()
                : toolProperties.getDefaultTimeoutSeconds();
        int maxOutputCharacters = request.maxOutputCharacters() != null
                ? request.maxOutputCharacters()
                : toolProperties.getMaxOutputCharacters();
        validateLimits(maxExecutionSeconds, maxOutputCharacters);

        Instant now = Instant.now();
        ToolDefinition tool = new ToolDefinition(
                UUID.randomUUID(),
                user.getOrganizationId(),
                project.getId(),
                toolKey,
                request.name().trim(),
                trimToNull(request.description()),
                ToolType.BUILT_IN,
                executorKey,
                request.inputSchema().trim(),
                trimToNull(request.outputSchema()),
                ToolStatus.DRAFT,
                request.requiresApproval(),
                maxExecutionSeconds,
                maxOutputCharacters,
                user.getUserId(),
                now);
        ToolDefinition saved = toolRepository.save(tool);
        auditService.toolCreated(toAuditContext(saved), user.getUserId());
        return toolMapper.toResponse(saved);
    }

    @Transactional
    public ToolResponse update(UUID projectId, UUID toolId, ToolUpdateRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ToolAuthorizationService.TOOL_UPDATE);
        ToolDefinition tool = requireTool(projectId, toolId, user.getOrganizationId());
        assertVersion(tool, request.version());

        if (tool.getStatus() == ToolStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "TOOL_ARCHIVED", "Archived tools cannot be updated");
        }

        if (tool.getStatus() == ToolStatus.ACTIVE && request.executorKey() != null) {
            String requestedExecutor = request.executorKey().trim();
            if (!requestedExecutor.equals(tool.getExecutorKey())) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "TOOL_EXECUTOR_LOCKED",
                        "Active tools cannot change executorKey");
            }
        }

        validateSchemas(request.inputSchema(), request.outputSchema());

        if (request.executorKey() != null && tool.getStatus() != ToolStatus.ACTIVE) {
            String executorKey = request.executorKey().trim();
            requireRegisteredExecutor(executorKey);
            tool.setExecutorKey(executorKey);
        }

        int maxExecutionSeconds = request.maxExecutionSeconds() != null
                ? request.maxExecutionSeconds()
                : tool.getMaxExecutionSeconds();
        int maxOutputCharacters = request.maxOutputCharacters() != null
                ? request.maxOutputCharacters()
                : tool.getMaxOutputCharacters();
        validateLimits(maxExecutionSeconds, maxOutputCharacters);

        tool.setName(request.name().trim());
        tool.setDescription(trimToNull(request.description()));
        tool.setInputSchema(request.inputSchema().trim());
        tool.setOutputSchema(trimToNull(request.outputSchema()));
        tool.setRequiresApproval(request.requiresApproval());
        tool.setMaxExecutionSeconds(maxExecutionSeconds);
        tool.setMaxOutputCharacters(maxOutputCharacters);
        tool.setUpdatedBy(user.getUserId());
        tool.setUpdatedAt(Instant.now());

        ToolDefinition saved = saveWithOptimisticLock(tool);
        auditService.toolUpdated(toAuditContext(saved), user.getUserId());
        return toolMapper.toResponse(saved);
    }

    @Transactional
    public ToolResponse activate(UUID projectId, UUID toolId, AuthenticatedUser user) {
        authorizationService.require(user, ToolAuthorizationService.TOOL_ACTIVATE);
        ToolDefinition tool = requireTool(projectId, toolId, user.getOrganizationId());

        if (tool.getStatus() == ToolStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "TOOL_ARCHIVED", "Archived tools cannot be activated");
        }
        if (tool.getStatus() == ToolStatus.ACTIVE) {
            return toolMapper.toResponse(tool);
        }

        requireRegisteredExecutor(tool.getExecutorKey());
        validateSchemas(tool.getInputSchema(), tool.getOutputSchema());

        tool.setStatus(ToolStatus.ACTIVE);
        tool.setUpdatedBy(user.getUserId());
        tool.setUpdatedAt(Instant.now());
        ToolDefinition saved = saveWithOptimisticLock(tool);
        auditService.toolActivated(toAuditContext(saved), user.getUserId());
        return toolMapper.toResponse(saved);
    }

    @Transactional
    public void archive(UUID projectId, UUID toolId, AuthenticatedUser user) {
        authorizationService.require(user, ToolAuthorizationService.TOOL_ARCHIVE);
        ToolDefinition tool = requireTool(projectId, toolId, user.getOrganizationId());

        if (tool.getStatus() == ToolStatus.ARCHIVED) {
            return;
        }

        tool.setStatus(ToolStatus.ARCHIVED);
        tool.setUpdatedBy(user.getUserId());
        tool.setUpdatedAt(Instant.now());
        saveWithOptimisticLock(tool);
        assignmentRepository.disableAllByToolId(toolId, projectId, user.getOrganizationId(), user.getUserId(), Instant.now());
        auditService.toolArchived(toAuditContext(tool), user.getUserId());
    }

    public ToolDefinition requireActiveTool(UUID projectId, UUID toolId, UUID organizationId) {
        ToolDefinition tool = requireTool(projectId, toolId, organizationId);
        if (tool.getStatus() != ToolStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "TOOL_NOT_ACTIVE", "Tool is not active");
        }
        return tool;
    }

    private ToolDefinition requireTool(UUID projectId, UUID toolId, UUID organizationId) {
        requireProjectInOrganization(projectId, organizationId);
        return toolRepository
                .findByIdAndProjectIdAndOrganizationId(toolId, projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TOOL_NOT_FOUND", "Tool not found"));
    }

    private Project requireProjectInOrganization(UUID projectId, UUID organizationId) {
        return projectRepository
                .findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));
    }

    private void validateToolKey(String toolKey) {
        if (!TOOL_KEY_PATTERN.matcher(toolKey).matches()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "TOOL_KEY_INVALID", "toolKey must be uppercase snake case");
        }
    }

    private void requireRegisteredExecutor(String executorKey) {
        if (!executorRegistry.isRegistered(executorKey)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "TOOL_EXECUTOR_UNKNOWN", "Unknown executor key: " + executorKey);
        }
    }

    private void validateSchemas(String inputSchema, String outputSchema) {
        schemaValidator.validate(inputSchema, toolProperties.getMaxSchemaCharacters());
        try {
            JsonNode inputNode = objectMapper.readTree(inputSchema);
            schemaValidator.validateStructure(inputNode);
            if (StringUtils.hasText(outputSchema)) {
                schemaValidator.validate(outputSchema, toolProperties.getMaxSchemaCharacters());
                schemaValidator.validateStructure(objectMapper.readTree(outputSchema));
            }
        } catch (ToolValidationException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage());
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TOOL_SCHEMA_INVALID", "Invalid JSON schema");
        }
    }

    private void validateLimits(int maxExecutionSeconds, int maxOutputCharacters) {
        if (maxExecutionSeconds < 1 || maxExecutionSeconds > toolProperties.getMaximumTimeoutSeconds()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "TOOL_TIMEOUT_INVALID",
                    "maxExecutionSeconds must be between 1 and " + toolProperties.getMaximumTimeoutSeconds());
        }
        if (maxOutputCharacters < 1 || maxOutputCharacters > toolProperties.getMaxOutputCharacters()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "TOOL_OUTPUT_LIMIT_INVALID",
                    "maxOutputCharacters must be between 1 and " + toolProperties.getMaxOutputCharacters());
        }
    }

    private void assertVersion(ToolDefinition tool, Integer version) {
        if (version == null || !version.equals(tool.getVersion())) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_CONFLICT", "Tool was modified by another request");
        }
    }

    private ToolDefinition saveWithOptimisticLock(ToolDefinition tool) {
        try {
            return toolRepository.saveAndFlush(tool);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_CONFLICT", "Tool was modified by another request");
        }
    }

    private ToolDefinitionContext toAuditContext(ToolDefinition tool) {
        return new ToolDefinitionContext(
                tool.getOrganizationId(),
                tool.getProjectId(),
                tool.getId(),
                tool.getToolKey(),
                tool.getStatus().name());
    }

    private String normalize(String search) {
        return StringUtils.hasText(search) ? search.trim() : null;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
