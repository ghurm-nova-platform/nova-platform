package ai.nova.platform.prompt.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
import ai.nova.platform.prompt.dto.PromptDtos.PromptVersionCreateRequest;
import ai.nova.platform.prompt.dto.PromptDtos.PromptVersionResponse;
import ai.nova.platform.prompt.dto.PromptDtos.PromptVersionUpdateRequest;
import ai.nova.platform.prompt.entity.PromptStatus;
import ai.nova.platform.prompt.entity.PromptType;
import ai.nova.platform.prompt.service.PromptService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/projects/{projectId}/prompts")
public class PromptController {

    private final PromptService promptService;

    public PromptController(PromptService promptService) {
        this.promptService = promptService;
    }

    @GetMapping
    public Page<PromptResponse> list(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) PromptStatus status,
            @RequestParam(required = false) PromptType type,
            @RequestParam(required = false) String tag,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return promptService.list(projectId, user, search, status, type, tag, pageable);
    }

    @GetMapping("/{promptId}")
    public PromptResponse get(
            @PathVariable UUID projectId,
            @PathVariable UUID promptId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return promptService.get(projectId, promptId, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromptResponse create(
            @PathVariable UUID projectId,
            @Valid @RequestBody PromptCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return promptService.create(projectId, request, user);
    }

    @PutMapping("/{promptId}")
    public PromptResponse update(
            @PathVariable UUID projectId,
            @PathVariable UUID promptId,
            @Valid @RequestBody PromptUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return promptService.update(projectId, promptId, request, user);
    }

    @DeleteMapping("/{promptId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(
            @PathVariable UUID projectId,
            @PathVariable UUID promptId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        promptService.archive(projectId, promptId, user);
    }

    @GetMapping("/{promptId}/versions")
    public List<PromptVersionResponse> listVersions(
            @PathVariable UUID projectId,
            @PathVariable UUID promptId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return promptService.listVersions(projectId, promptId, user);
    }

    @GetMapping("/{promptId}/versions/{versionId}")
    public PromptVersionResponse getVersion(
            @PathVariable UUID projectId,
            @PathVariable UUID promptId,
            @PathVariable UUID versionId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return promptService.getVersion(projectId, promptId, versionId, user);
    }

    @PostMapping("/{promptId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public PromptVersionResponse createVersion(
            @PathVariable UUID projectId,
            @PathVariable UUID promptId,
            @RequestBody(required = false) PromptVersionCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        PromptVersionCreateRequest body = request != null ? request : new PromptVersionCreateRequest(null);
        return promptService.createVersion(projectId, promptId, body, user);
    }

    @PutMapping("/{promptId}/versions/{versionId}")
    public PromptVersionResponse updateVersion(
            @PathVariable UUID projectId,
            @PathVariable UUID promptId,
            @PathVariable UUID versionId,
            @Valid @RequestBody PromptVersionUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return promptService.updateVersion(projectId, promptId, versionId, request, user);
    }

    @PostMapping("/{promptId}/versions/{versionId}/publish")
    public PromptVersionResponse publish(
            @PathVariable UUID projectId,
            @PathVariable UUID promptId,
            @PathVariable UUID versionId,
            @RequestBody(required = false) PromptPublishRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return promptService.publish(projectId, promptId, versionId, request, user);
    }

    @PostMapping("/{promptId}/rollback")
    @ResponseStatus(HttpStatus.CREATED)
    public PromptVersionResponse rollback(
            @PathVariable UUID projectId,
            @PathVariable UUID promptId,
            @Valid @RequestBody PromptRollbackRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return promptService.rollback(projectId, promptId, request, user);
    }

    @PostMapping("/{promptId}/compare")
    public PromptCompareResponse compare(
            @PathVariable UUID projectId,
            @PathVariable UUID promptId,
            @Valid @RequestBody PromptCompareRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return promptService.compare(projectId, promptId, request, user);
    }

    @PostMapping("/validate")
    public PromptValidateResponse validate(
            @PathVariable UUID projectId,
            @Valid @RequestBody PromptValidateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return promptService.validate(projectId, request, user);
    }

    @PostMapping("/preview")
    public PromptPreviewResponse preview(
            @PathVariable UUID projectId,
            @Valid @RequestBody PromptPreviewRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return promptService.preview(projectId, request, user);
    }
}
