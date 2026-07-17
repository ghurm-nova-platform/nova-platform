package ai.nova.platform.prompt.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ai.nova.platform.agent.entity.AgentStatus;
import ai.nova.platform.agent.repository.AgentRepository;
import ai.nova.platform.project.Project;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.prompt.dto.PromptDtos.PromptCompareRequest;
import ai.nova.platform.prompt.dto.PromptDtos.PromptCompareResponse;
import ai.nova.platform.prompt.dto.PromptDtos.PromptCreateRequest;
import ai.nova.platform.prompt.dto.PromptDtos.PromptPreviewRequest;
import ai.nova.platform.prompt.dto.PromptDtos.PromptPreviewResponse;
import ai.nova.platform.prompt.dto.PromptDtos.PromptPublishRequest;
import ai.nova.platform.prompt.dto.PromptDtos.PromptResponse;
import ai.nova.platform.prompt.dto.PromptDtos.PromptRollbackRequest;
import ai.nova.platform.prompt.dto.PromptDtos.PromptUpdateRequest;
import ai.nova.platform.prompt.dto.PromptDtos.PromptValidateRequest;
import ai.nova.platform.prompt.dto.PromptDtos.PromptValidateResponse;
import ai.nova.platform.prompt.dto.PromptDtos.PromptVariableRequest;
import ai.nova.platform.prompt.dto.PromptDtos.PromptVersionCreateRequest;
import ai.nova.platform.prompt.dto.PromptDtos.PromptVersionResponse;
import ai.nova.platform.prompt.dto.PromptDtos.PromptVersionUpdateRequest;
import ai.nova.platform.prompt.entity.Prompt;
import ai.nova.platform.prompt.entity.PromptAuditAction;
import ai.nova.platform.prompt.entity.PromptAuditLog;
import ai.nova.platform.prompt.entity.PromptStatus;
import ai.nova.platform.prompt.entity.PromptTag;
import ai.nova.platform.prompt.entity.PromptVariable;
import ai.nova.platform.prompt.entity.PromptVersion;
import ai.nova.platform.prompt.entity.PromptVersionStatus;
import ai.nova.platform.prompt.entity.PromptType;
import ai.nova.platform.prompt.mapper.PromptMapper;
import ai.nova.platform.prompt.parser.PromptVariableParser;
import ai.nova.platform.prompt.parser.PromptVariableParser.VariableDefinition;
import ai.nova.platform.prompt.repository.PromptAuditLogRepository;
import ai.nova.platform.prompt.repository.PromptRepository;
import ai.nova.platform.prompt.repository.PromptTagRepository;
import ai.nova.platform.prompt.repository.PromptVariableRepository;
import ai.nova.platform.prompt.repository.PromptVersionRepository;
import ai.nova.platform.prompt.security.PromptAuthorizationService;
import ai.nova.platform.prompt.validation.PromptProperties;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.correlation.CorrelationIdFilter;
import ai.nova.platform.web.error.ApiException;

@Service
public class PromptService {

    private final PromptRepository promptRepository;
    private final PromptVersionRepository versionRepository;
    private final PromptVariableRepository variableRepository;
    private final PromptTagRepository tagRepository;
    private final PromptAuditLogRepository auditLogRepository;
    private final ProjectRepository projectRepository;
    private final AgentRepository agentRepository;
    private final PromptMapper promptMapper;
    private final PromptAuthorizationService authorizationService;
    private final PromptVariableParser variableParser;
    private final PromptProperties promptProperties;

    public PromptService(
            PromptRepository promptRepository,
            PromptVersionRepository versionRepository,
            PromptVariableRepository variableRepository,
            PromptTagRepository tagRepository,
            PromptAuditLogRepository auditLogRepository,
            ProjectRepository projectRepository,
            AgentRepository agentRepository,
            PromptMapper promptMapper,
            PromptAuthorizationService authorizationService,
            PromptVariableParser variableParser,
            PromptProperties promptProperties) {
        this.promptRepository = promptRepository;
        this.versionRepository = versionRepository;
        this.variableRepository = variableRepository;
        this.tagRepository = tagRepository;
        this.auditLogRepository = auditLogRepository;
        this.projectRepository = projectRepository;
        this.agentRepository = agentRepository;
        this.promptMapper = promptMapper;
        this.authorizationService = authorizationService;
        this.variableParser = variableParser;
        this.promptProperties = promptProperties;
    }

    @Transactional(readOnly = true)
    public Page<PromptResponse> list(
            UUID projectId,
            AuthenticatedUser user,
            String search,
            PromptStatus status,
            PromptType type,
            String tag,
            Pageable pageable) {
        authorizationService.require(user, PromptAuthorizationService.PROMPT_READ);
        requireProjectInOrganization(projectId, user.getOrganizationId());
        return promptRepository
                .search(
                        user.getOrganizationId(),
                        projectId,
                        normalize(search),
                        status,
                        type,
                        normalize(tag),
                        pageable)
                .map(this::toPromptResponse);
    }

    @Transactional(readOnly = true)
    public PromptResponse get(UUID projectId, UUID promptId, AuthenticatedUser user) {
        authorizationService.require(user, PromptAuthorizationService.PROMPT_READ);
        Prompt prompt = requirePrompt(projectId, promptId, user.getOrganizationId());
        return toPromptResponse(prompt);
    }

    @Transactional
    public PromptResponse create(UUID projectId, PromptCreateRequest request, AuthenticatedUser user) {
        authorizationService.require(user, PromptAuthorizationService.PROMPT_CREATE);
        Project project = requireProjectInOrganization(projectId, user.getOrganizationId());

        String name = request.name().trim();
        if (promptRepository.existsByProjectIdAndNameIgnoreCase(projectId, name)) {
            throw new ApiException(HttpStatus.CONFLICT, "PROMPT_NAME_EXISTS", "Prompt name already exists in this project");
        }

        validateContentLength(request.content());
        List<String> tags = normalizeTags(request.tags());

        Instant now = Instant.now();
        UUID promptId = UUID.randomUUID();
        Prompt prompt = new Prompt(
                promptId,
                user.getOrganizationId(),
                project.getId(),
                name,
                trimToNull(request.description()),
                request.promptType(),
                PromptStatus.DRAFT,
                user.getUserId(),
                now);

        UUID versionId = UUID.randomUUID();
        PromptVersion version = new PromptVersion(
                versionId,
                promptId,
                user.getOrganizationId(),
                project.getId(),
                1,
                request.content().trim(),
                trimToNull(request.changeSummary()),
                PromptVersionStatus.DRAFT,
                user.getUserId(),
                now);

        validateVersionContent(version.getContent(), request.variables());

        Prompt savedPrompt = promptRepository.save(prompt);
        versionRepository.save(version);
        prompt.setCurrentDraftVersionId(versionId);
        savedPrompt = savePromptWithOptimisticLock(prompt);
        saveVariables(versionId, request.variables(), now);
        saveTags(promptId, tags, now);

        writeAudit(savedPrompt, versionId, PromptAuditAction.CREATED, null, name, user.getUserId());
        writeAudit(savedPrompt, versionId, PromptAuditAction.DRAFT_CREATED, null, "1", user.getUserId());

        return toPromptResponse(savedPrompt);
    }

    @Transactional
    public PromptResponse update(UUID projectId, UUID promptId, PromptUpdateRequest request, AuthenticatedUser user) {
        authorizationService.require(user, PromptAuthorizationService.PROMPT_UPDATE);
        Prompt prompt = requirePrompt(projectId, promptId, user.getOrganizationId());
        assertVersion(prompt, request.version());

        if (prompt.getStatus() == PromptStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "PROMPT_ARCHIVED", "Archived prompts cannot be updated");
        }

        String name = request.name().trim();
        if (promptRepository.existsByProjectIdAndNameIgnoreCaseAndIdNot(projectId, name, promptId)) {
            throw new ApiException(HttpStatus.CONFLICT, "PROMPT_NAME_EXISTS", "Prompt name already exists in this project");
        }

        List<String> newTags = normalizeTags(request.tags());
        List<String> oldTags = loadTagNames(promptId);
        String oldName = prompt.getName();

        replaceTags(promptId, newTags, Instant.now());

        prompt.setName(name);
        prompt.setDescription(trimToNull(request.description()));
        prompt.setPromptType(request.promptType());
        prompt.setUpdatedBy(user.getUserId());
        prompt.setUpdatedAt(Instant.now());

        auditTagChanges(prompt, oldTags, newTags, user.getUserId());

        Prompt saved = savePromptWithOptimisticLock(prompt);
        if (!oldName.equals(name)) {
            writeAudit(saved, null, PromptAuditAction.UPDATED, oldName, name, user.getUserId());
        } else {
            writeAudit(saved, null, PromptAuditAction.UPDATED, null, name, user.getUserId());
        }
        return toPromptResponse(saved);
    }

    @Transactional
    public void archive(UUID projectId, UUID promptId, AuthenticatedUser user) {
        authorizationService.require(user, PromptAuthorizationService.PROMPT_ARCHIVE);
        Prompt prompt = requirePrompt(projectId, promptId, user.getOrganizationId());

        if (prompt.getStatus() == PromptStatus.ARCHIVED) {
            return;
        }

        if (agentRepository.existsByPromptIdAndStatus(promptId, AgentStatus.ACTIVE)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "PROMPT_IN_USE",
                    "Active agents reference this prompt");
        }

        PromptStatus oldStatus = prompt.getStatus();
        prompt.setStatus(PromptStatus.ARCHIVED);
        prompt.setUpdatedBy(user.getUserId());
        prompt.setUpdatedAt(Instant.now());
        Prompt saved = savePromptWithOptimisticLock(prompt);
        writeAudit(saved, null, PromptAuditAction.ARCHIVED, oldStatus.name(), PromptStatus.ARCHIVED.name(), user.getUserId());
    }

    @Transactional(readOnly = true)
    public List<PromptVersionResponse> listVersions(UUID projectId, UUID promptId, AuthenticatedUser user) {
        authorizationService.require(user, PromptAuthorizationService.PROMPT_READ);
        requirePrompt(projectId, promptId, user.getOrganizationId());
        return versionRepository.findByPromptIdOrderByVersionNumberDesc(promptId).stream()
                .map(version -> promptMapper.toVersionResponse(version, loadVariables(version.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PromptVersionResponse getVersion(
            UUID projectId, UUID promptId, UUID versionId, AuthenticatedUser user) {
        authorizationService.require(user, PromptAuthorizationService.PROMPT_READ);
        PromptVersion version = requireVersion(projectId, promptId, versionId, user.getOrganizationId());
        return promptMapper.toVersionResponse(version, loadVariables(version.getId()));
    }

    @Transactional
    public PromptVersionResponse createVersion(
            UUID projectId,
            UUID promptId,
            PromptVersionCreateRequest request,
            AuthenticatedUser user) {
        authorizationService.require(user, PromptAuthorizationService.PROMPT_UPDATE);
        Prompt prompt = requirePrompt(projectId, promptId, user.getOrganizationId());

        if (prompt.getStatus() == PromptStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "PROMPT_ARCHIVED", "Archived prompts cannot be versioned");
        }

        if (prompt.getCurrentDraftVersionId() != null) {
            PromptVersion existingDraft = versionRepository
                    .findById(prompt.getCurrentDraftVersionId())
                    .orElse(null);
            if (existingDraft != null && existingDraft.getStatus() == PromptVersionStatus.DRAFT) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "NO_DRAFT_VERSION",
                        "A draft version already exists; update the current draft instead");
            }
        }

        Instant now = Instant.now();
        int nextNumber = versionRepository.findMaxVersionNumber(promptId) + 1;
        UUID versionId = UUID.randomUUID();

        String content;
        String changeSummary = trimToNull(request.changeSummary());
        List<PromptVariableRequest> variables;

        if (prompt.getPublishedVersionId() != null) {
            PromptVersion published = versionRepository
                    .findById(prompt.getPublishedVersionId())
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND, "PROMPT_VERSION_NOT_FOUND", "Published version not found"));
            content = published.getContent();
            if (changeSummary == null) {
                changeSummary = "Draft from published version " + published.getVersionNumber();
            }
            variables = loadVariables(published.getId()).stream()
                    .map(v -> new PromptVariableRequest(
                            v.getName(),
                            v.getDescription(),
                            v.getDataType(),
                            v.isRequired(),
                            v.getDefaultValue(),
                            v.getSampleValue()))
                    .toList();
        } else {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "NO_PUBLISHED_VERSION",
                    "Cannot create a new draft without a published version or initial draft workflow");
        }

        PromptVersion version = new PromptVersion(
                versionId,
                promptId,
                prompt.getOrganizationId(),
                prompt.getProjectId(),
                nextNumber,
                content,
                changeSummary,
                PromptVersionStatus.DRAFT,
                user.getUserId(),
                now);

        prompt.setUpdatedBy(user.getUserId());
        prompt.setUpdatedAt(now);
        versionRepository.save(version);
        prompt.setCurrentDraftVersionId(versionId);
        savePromptWithOptimisticLock(prompt);
        saveVariables(versionId, variables, now);

        writeAudit(prompt, versionId, PromptAuditAction.DRAFT_CREATED, null, String.valueOf(nextNumber), user.getUserId());
        return promptMapper.toVersionResponse(version, loadVariables(versionId));
    }

    @Transactional
    public PromptVersionResponse updateVersion(
            UUID projectId,
            UUID promptId,
            UUID versionId,
            PromptVersionUpdateRequest request,
            AuthenticatedUser user) {
        authorizationService.require(user, PromptAuthorizationService.PROMPT_UPDATE);
        Prompt prompt = requirePrompt(projectId, promptId, user.getOrganizationId());
        PromptVersion version = requireVersion(projectId, promptId, versionId, user.getOrganizationId());

        if (version.getStatus() != PromptVersionStatus.DRAFT) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "PROMPT_VERSION_IMMUTABLE",
                    "Only draft versions can be updated");
        }

        validateContentLength(request.content());
        validateVersionContent(request.content(), request.variables());

        String oldContent = version.getContent();
        version.setContent(request.content().trim());
        version.setChangeSummary(trimToNull(request.changeSummary()));

        variableRepository.deleteByPromptVersionId(versionId);
        variableRepository.flush();

        versionRepository.saveAndFlush(version);
        saveVariables(versionId, request.variables(), Instant.now());

        prompt.setUpdatedBy(user.getUserId());
        prompt.setUpdatedAt(Instant.now());
        savePromptWithOptimisticLock(prompt);

        writeAudit(prompt, versionId, PromptAuditAction.VERSION_UPDATED, oldContent, version.getContent(), user.getUserId());
        return promptMapper.toVersionResponse(version, loadVariables(versionId));
    }

    @Transactional
    public PromptVersionResponse publish(
            UUID projectId,
            UUID promptId,
            UUID versionId,
            PromptPublishRequest request,
            AuthenticatedUser user) {
        authorizationService.require(user, PromptAuthorizationService.PROMPT_PUBLISH);
        Prompt prompt = requirePrompt(projectId, promptId, user.getOrganizationId());
        PromptVersion version = requireVersion(projectId, promptId, versionId, user.getOrganizationId());

        if (version.getStatus() != PromptVersionStatus.DRAFT) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "PROMPT_VERSION_IMMUTABLE",
                    "Only draft versions can be published");
        }

        List<PromptVariable> variables = loadVariables(versionId);
        List<PromptVariableRequest> variableRequests = variables.stream()
                .map(v -> new PromptVariableRequest(
                        v.getName(),
                        v.getDescription(),
                        v.getDataType(),
                        v.isRequired(),
                        v.getDefaultValue(),
                        v.getSampleValue()))
                .toList();
        validateVersionContent(version.getContent(), variableRequests);

        Instant now = Instant.now();
        if (prompt.getPublishedVersionId() != null) {
            versionRepository
                    .findById(prompt.getPublishedVersionId())
                    .ifPresent(previous -> {
                        previous.setStatus(PromptVersionStatus.ARCHIVED);
                        versionRepository.save(previous);
                    });
        }

        version.setStatus(PromptVersionStatus.PUBLISHED);
        version.setPublishedBy(user.getUserId());
        version.setPublishedAt(now);
        if (StringUtils.hasText(request != null ? request.reason() : null)) {
            version.setChangeSummary(trimToNull(request.reason()));
        }
        versionRepository.save(version);

        prompt.setPublishedVersionId(versionId);
        prompt.setStatus(PromptStatus.PUBLISHED);
        if (versionId.equals(prompt.getCurrentDraftVersionId())) {
            prompt.setCurrentDraftVersionId(null);
        }
        prompt.setUpdatedBy(user.getUserId());
        prompt.setUpdatedAt(now);
        savePromptWithOptimisticLock(prompt);

        writeAudit(prompt, versionId, PromptAuditAction.PUBLISHED, null, String.valueOf(version.getVersionNumber()), user.getUserId());
        return promptMapper.toVersionResponse(version, variables);
    }

    @Transactional
    public PromptVersionResponse rollback(
            UUID projectId,
            UUID promptId,
            PromptRollbackRequest request,
            AuthenticatedUser user) {
        authorizationService.require(user, PromptAuthorizationService.PROMPT_PUBLISH);
        Prompt prompt = requirePrompt(projectId, promptId, user.getOrganizationId());

        if (prompt.getCurrentDraftVersionId() != null) {
            PromptVersion existingDraft = versionRepository
                    .findById(prompt.getCurrentDraftVersionId())
                    .orElse(null);
            if (existingDraft != null && existingDraft.getStatus() == PromptVersionStatus.DRAFT) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "NO_DRAFT_VERSION",
                        "A draft version already exists; update the current draft instead");
            }
        }

        PromptVersion source = requireVersion(
                projectId, promptId, request.sourceVersionId(), user.getOrganizationId());

        Instant now = Instant.now();
        int nextNumber = versionRepository.findMaxVersionNumber(promptId) + 1;
        UUID versionId = UUID.randomUUID();

        String changeSummary = StringUtils.hasText(request.reason())
                ? request.reason().trim()
                : "Rollback from version " + source.getVersionNumber();

        PromptVersion draft = new PromptVersion(
                versionId,
                promptId,
                prompt.getOrganizationId(),
                prompt.getProjectId(),
                nextNumber,
                source.getContent(),
                changeSummary,
                PromptVersionStatus.DRAFT,
                user.getUserId(),
                now);

        List<PromptVariableRequest> variables = loadVariables(source.getId()).stream()
                .map(v -> new PromptVariableRequest(
                        v.getName(),
                        v.getDescription(),
                        v.getDataType(),
                        v.isRequired(),
                        v.getDefaultValue(),
                        v.getSampleValue()))
                .toList();

        prompt.setUpdatedBy(user.getUserId());
        prompt.setUpdatedAt(now);
        versionRepository.save(draft);
        prompt.setCurrentDraftVersionId(versionId);
        savePromptWithOptimisticLock(prompt);
        saveVariables(versionId, variables, now);

        writeAudit(
                prompt,
                versionId,
                PromptAuditAction.ROLLED_BACK,
                String.valueOf(source.getVersionNumber()),
                String.valueOf(nextNumber),
                user.getUserId());

        return promptMapper.toVersionResponse(draft, loadVariables(versionId));
    }

    @Transactional(readOnly = true)
    public PromptCompareResponse compare(
            UUID projectId, UUID promptId, PromptCompareRequest request, AuthenticatedUser user) {
        authorizationService.require(user, PromptAuthorizationService.PROMPT_COMPARE);
        requirePrompt(projectId, promptId, user.getOrganizationId());

        PromptVersion left = requireVersion(projectId, promptId, request.leftVersionId(), user.getOrganizationId());
        PromptVersion right = requireVersion(projectId, promptId, request.rightVersionId(), user.getOrganizationId());

        return new PromptCompareResponse(
                left.getId(),
                right.getId(),
                left.getContent(),
                right.getContent(),
                PromptDiffUtil.lineDiff(left.getContent(), right.getContent()));
    }

    @Transactional(readOnly = true)
    public PromptValidateResponse validate(PromptValidateRequest request, AuthenticatedUser user) {
        authorizationService.require(user, PromptAuthorizationService.PROMPT_READ);
        validateContentLength(request.content());
        var result = variableParser.validate(request.content(), toDefinitions(request.variables()));
        return new PromptValidateResponse(
                result.valid(), result.detectedVariables(), result.errors(), result.warnings());
    }

    @Transactional(readOnly = true)
    public PromptPreviewResponse preview(PromptPreviewRequest request, AuthenticatedUser user) {
        authorizationService.require(user, PromptAuthorizationService.PROMPT_PREVIEW);
        validateContentLength(request.content());
        var result = variableParser.preview(
                request.content(), request.values(), toDefinitions(request.variables()));
        return new PromptPreviewResponse(
                result.renderedContent(),
                result.errors(),
                result.warnings(),
                result.missingRequiredVariables());
    }

    public void requirePublishedPromptReference(
            UUID organizationId, UUID projectId, UUID promptId, UUID promptVersionId) {
        Prompt prompt = promptRepository
                .findByIdAndProjectIdAndOrganizationId(promptId, projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROMPT_NOT_FOUND", "Prompt not found"));

        if (prompt.getStatus() == PromptStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "PROMPT_ARCHIVED", "Prompt is archived");
        }

        PromptVersion version = versionRepository
                .findByIdAndPromptIdAndOrganizationIdAndProjectId(
                        promptVersionId, promptId, organizationId, projectId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "PROMPT_VERSION_NOT_FOUND", "Prompt version not found"));

        if (version.getStatus() != PromptVersionStatus.PUBLISHED) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PROMPT_VERSION_NOT_PUBLISHED",
                    "Only published prompt versions can be referenced by agents");
        }
    }

    private PromptResponse toPromptResponse(Prompt prompt) {
        Integer draftNumber = null;
        if (prompt.getCurrentDraftVersionId() != null) {
            draftNumber = versionRepository
                    .findById(prompt.getCurrentDraftVersionId())
                    .map(PromptVersion::getVersionNumber)
                    .orElse(null);
        }
        Integer publishedNumber = null;
        if (prompt.getPublishedVersionId() != null) {
            publishedNumber = versionRepository
                    .findById(prompt.getPublishedVersionId())
                    .map(PromptVersion::getVersionNumber)
                    .orElse(null);
        }
        return promptMapper.toResponse(prompt, loadTagNames(prompt.getId()), draftNumber, publishedNumber);
    }

    private Prompt requirePrompt(UUID projectId, UUID promptId, UUID organizationId) {
        requireProjectInOrganization(projectId, organizationId);
        return promptRepository
                .findByIdAndProjectIdAndOrganizationId(promptId, projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROMPT_NOT_FOUND", "Prompt not found"));
    }

    private PromptVersion requireVersion(
            UUID projectId, UUID promptId, UUID versionId, UUID organizationId) {
        return versionRepository
                .findByIdAndPromptIdAndOrganizationIdAndProjectId(
                        versionId, promptId, organizationId, projectId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "PROMPT_VERSION_NOT_FOUND", "Prompt version not found"));
    }

    private Project requireProjectInOrganization(UUID projectId, UUID organizationId) {
        return projectRepository
                .findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));
    }

    private void assertVersion(Prompt prompt, Integer version) {
        if (version == null || !version.equals(prompt.getVersion())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "OPTIMISTIC_LOCK_CONFLICT",
                    "Prompt was modified by another request");
        }
    }

    private Prompt savePromptWithOptimisticLock(Prompt prompt) {
        try {
            return promptRepository.saveAndFlush(prompt);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "OPTIMISTIC_LOCK_CONFLICT",
                    "Prompt was modified by another request");
        }
    }

    private void validateContentLength(String content) {
        if (content != null && content.length() > promptProperties.getMaxContentLength()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PROMPT_CONTENT_TOO_LONG",
                    "Prompt content exceeds maximum length of " + promptProperties.getMaxContentLength());
        }
    }

    private void validateVersionContent(String content, List<PromptVariableRequest> variables) {
        var validation = variableParser.validate(content, toDefinitions(variables));
        if (!validation.valid()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PROMPT_VALIDATION_FAILED",
                    String.join("; ", validation.errors()));
        }
    }

    private List<VariableDefinition> toDefinitions(List<PromptVariableRequest> variables) {
        if (variables == null) {
            return List.of();
        }
        return variables.stream()
                .map(v -> new VariableDefinition(v.name(), v.required(), v.defaultValue()))
                .toList();
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        if (tags.size() > promptProperties.getMaxTags()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PROMPT_TOO_MANY_TAGS",
                    "Prompt cannot have more than " + promptProperties.getMaxTags() + " tags");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            if (!StringUtils.hasText(tag)) {
                continue;
            }
            String trimmed = tag.trim();
            if (trimmed.length() > promptProperties.getMaxTagLength()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "PROMPT_TAG_TOO_LONG",
                        "Tag exceeds maximum length of " + promptProperties.getMaxTagLength());
            }
            normalized.add(trimmed);
        }
        return List.copyOf(normalized);
    }

    private void saveTags(UUID promptId, List<String> tags, Instant now) {
        for (String tag : tags) {
            tagRepository.save(new PromptTag(UUID.randomUUID(), promptId, tag, now));
        }
    }

    private void replaceTags(UUID promptId, List<String> tags, Instant now) {
        tagRepository.deleteByPromptId(promptId);
        tagRepository.flush();
        saveTags(promptId, tags, now);
    }

    private List<String> loadTagNames(UUID promptId) {
        return tagRepository.findByPromptIdOrderByTagNameAsc(promptId).stream()
                .map(PromptTag::getTagName)
                .toList();
    }

    private List<PromptVariable> loadVariables(UUID versionId) {
        return variableRepository.findByPromptVersionIdOrderByNameAsc(versionId);
    }

    private void saveVariables(UUID versionId, List<PromptVariableRequest> variables, Instant now) {
        if (variables == null) {
            return;
        }
        Set<String> names = new HashSet<>();
        for (PromptVariableRequest variable : variables) {
            String name = variable.name().trim();
            if (!names.add(name)) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "PROMPT_VALIDATION_FAILED",
                        "Duplicate variable definition: " + name);
            }
            variableRepository.save(new PromptVariable(
                    UUID.randomUUID(),
                    versionId,
                    name,
                    trimToNull(variable.description()),
                    variable.dataType(),
                    variable.required(),
                    trimToNull(variable.defaultValue()),
                    trimToNull(variable.sampleValue()),
                    now));
        }
    }

    private void auditTagChanges(Prompt prompt, List<String> oldTags, List<String> newTags, UUID userId) {
        Set<String> oldSet = new HashSet<>(oldTags);
        Set<String> newSet = new HashSet<>(newTags);
        for (String tag : newTags) {
            if (!oldSet.contains(tag)) {
                writeAudit(prompt, null, PromptAuditAction.TAG_ADDED, null, tag, userId);
            }
        }
        for (String tag : oldTags) {
            if (!newSet.contains(tag)) {
                writeAudit(prompt, null, PromptAuditAction.TAG_REMOVED, tag, null, userId);
            }
        }
    }

    private void writeAudit(
            Prompt prompt,
            UUID versionId,
            PromptAuditAction action,
            String oldValue,
            String newValue,
            UUID performedBy) {
        auditLogRepository.save(new PromptAuditLog(
                UUID.randomUUID(),
                prompt.getId(),
                versionId,
                prompt.getOrganizationId(),
                prompt.getProjectId(),
                action,
                oldValue,
                newValue,
                performedBy,
                Instant.now(),
                MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)));
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
