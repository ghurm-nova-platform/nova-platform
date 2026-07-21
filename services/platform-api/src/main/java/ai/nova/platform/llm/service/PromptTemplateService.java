package ai.nova.platform.llm.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.llm.dto.LlmDtos.CreatePromptRequest;
import ai.nova.platform.llm.dto.LlmDtos.PromptView;
import ai.nova.platform.llm.dto.LlmDtos.RenderPromptResponse;
import ai.nova.platform.llm.dto.LlmDtos.UpdatePromptRequest;
import ai.nova.platform.llm.entity.LlmPromptTemplateEntity;
import ai.nova.platform.llm.error.LlmErrorCodes;
import ai.nova.platform.llm.mapper.LlmMapper;
import ai.nova.platform.llm.repository.LlmPromptTemplateRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class PromptTemplateService {

    private static final Pattern VAR = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)\\}\\}");

    private final LlmPromptTemplateRepository repository;
    private final LlmMapper mapper;
    private final LlmAuditService auditService;

    public PromptTemplateService(
            LlmPromptTemplateRepository repository, LlmMapper mapper, LlmAuditService auditService) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PromptView> list(UUID organizationId) {
        return repository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(mapper::toPromptView)
                .toList();
    }

    @Transactional(readOnly = true)
    public PromptView get(UUID organizationId, UUID promptId) {
        return mapper.toPromptView(require(organizationId, promptId));
    }

    @Transactional
    public PromptView create(CreatePromptRequest request, AuthenticatedUser user) {
        if (repository.findByOrganizationIdAndCode(user.getOrganizationId(), request.code()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, LlmErrorCodes.CONFLICT, "Prompt code already exists");
        }
        Instant now = Instant.now();
        LlmPromptTemplateEntity entity = new LlmPromptTemplateEntity(
                UUID.randomUUID(),
                user.getOrganizationId(),
                request.code().trim(),
                request.name().trim(),
                request.category(),
                request.userPromptTemplate(),
                now);
        entity.setSystemPrompt(request.systemPrompt());
        entity.setAssistantPromptTemplate(request.assistantPromptTemplate());
        if (request.variablesJson() != null) {
            entity.setVariablesJson(request.variablesJson());
        }
        repository.save(entity);
        auditService.record(
                user, entity.getId(), entity.getCode(), AuditAction.CREATE, AuditResult.SUCCESS, Map.of());
        return mapper.toPromptView(entity);
    }

    @Transactional
    public PromptView update(UUID promptId, UpdatePromptRequest request, AuthenticatedUser user) {
        LlmPromptTemplateEntity entity = require(user.getOrganizationId(), promptId);
        if (request.name() != null) {
            entity.setName(request.name());
        }
        if (request.category() != null) {
            entity.setCategory(request.category());
        }
        if (request.systemPrompt() != null) {
            entity.setSystemPrompt(request.systemPrompt());
        }
        if (request.userPromptTemplate() != null) {
            entity.setUserPromptTemplate(request.userPromptTemplate());
        }
        if (request.assistantPromptTemplate() != null) {
            entity.setAssistantPromptTemplate(request.assistantPromptTemplate());
        }
        if (request.variablesJson() != null) {
            entity.setVariablesJson(request.variablesJson());
        }
        if (request.enabled() != null) {
            entity.setEnabled(request.enabled());
        }
        entity.setTemplateVersion(entity.getTemplateVersion() + 1);
        entity.touch(Instant.now());
        auditService.record(
                user, entity.getId(), entity.getCode(), AuditAction.UPDATE, AuditResult.SUCCESS, Map.of());
        return mapper.toPromptView(entity);
    }

    @Transactional
    public void delete(UUID promptId, AuthenticatedUser user) {
        LlmPromptTemplateEntity entity = require(user.getOrganizationId(), promptId);
        repository.delete(entity);
        auditService.record(
                user, entity.getId(), entity.getCode(), AuditAction.DELETE, AuditResult.SUCCESS, Map.of());
    }

    @Transactional(readOnly = true)
    public RenderPromptResponse render(UUID promptId, Map<String, String> variables, AuthenticatedUser user) {
        LlmPromptTemplateEntity entity = require(user.getOrganizationId(), promptId);
        Map<String, String> vars = variables == null ? Map.of() : variables;
        return new RenderPromptResponse(
                render(entity.getSystemPrompt(), vars),
                render(entity.getUserPromptTemplate(), vars),
                render(entity.getAssistantPromptTemplate(), vars));
    }

    static String render(String template, Map<String, String> variables) {
        if (template == null) {
            return null;
        }
        Matcher matcher = VAR.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = variables.getOrDefault(key, "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private LlmPromptTemplateEntity require(UUID organizationId, UUID promptId) {
        return repository
                .findByIdAndOrganizationId(promptId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, LlmErrorCodes.NOT_FOUND, "Prompt not found"));
    }
}
