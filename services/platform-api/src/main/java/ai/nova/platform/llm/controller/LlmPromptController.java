package ai.nova.platform.llm.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.llm.dto.LlmDtos.CreatePromptRequest;
import ai.nova.platform.llm.dto.LlmDtos.PromptView;
import ai.nova.platform.llm.dto.LlmDtos.RenderPromptRequest;
import ai.nova.platform.llm.dto.LlmDtos.RenderPromptResponse;
import ai.nova.platform.llm.dto.LlmDtos.UpdatePromptRequest;
import ai.nova.platform.llm.service.LlmAuthorizationService;
import ai.nova.platform.llm.service.PromptTemplateService;
import ai.nova.platform.security.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/llm/prompts")
public class LlmPromptController {

    private final PromptTemplateService promptTemplateService;
    private final LlmAuthorizationService authorizationService;

    public LlmPromptController(
            PromptTemplateService promptTemplateService, LlmAuthorizationService authorizationService) {
        this.promptTemplateService = promptTemplateService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<PromptView> list(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return promptTemplateService.list(user.getOrganizationId());
    }

    @GetMapping("/{id}")
    public PromptView get(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return promptTemplateService.get(user.getOrganizationId(), id);
    }

    @PostMapping
    public PromptView create(
            @Valid @RequestBody CreatePromptRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requirePromptAdmin(user);
        return promptTemplateService.create(request, user);
    }

    @PutMapping("/{id}")
    public PromptView update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePromptRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requirePromptAdmin(user);
        return promptTemplateService.update(id, request, user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requirePromptAdmin(user);
        promptTemplateService.delete(id, user);
    }

    @PostMapping("/{id}/render")
    public RenderPromptResponse render(
            @PathVariable UUID id,
            @RequestBody(required = false) RenderPromptRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return promptTemplateService.render(id, request == null ? null : request.variables(), user);
    }
}
