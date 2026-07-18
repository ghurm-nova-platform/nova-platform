package ai.nova.platform.knowledge.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import ai.nova.platform.knowledge.dto.KnowledgeDtos.CreateKnowledgeBaseRequest;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.EmbeddingProvidersResponse;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.KnowledgeBaseResponse;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.UpdateKnowledgeBaseRequest;
import ai.nova.platform.knowledge.entity.KnowledgeBaseStatus;
import ai.nova.platform.knowledge.service.KnowledgeBaseService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/projects/{projectId}/knowledge-bases")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @GetMapping
    public Page<KnowledgeBaseResponse> list(
            @PathVariable UUID projectId,
            @RequestParam(required = false) KnowledgeBaseStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return knowledgeBaseService.list(projectId, status, search, pageable, user);
    }

    @GetMapping("/providers")
    public EmbeddingProvidersResponse providers(
            @PathVariable UUID projectId, @AuthenticationPrincipal AuthenticatedUser user) {
        return knowledgeBaseService.listProviders(projectId, user);
    }

    @GetMapping("/{id}")
    public KnowledgeBaseResponse get(
            @PathVariable UUID projectId,
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return knowledgeBaseService.get(projectId, id, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public KnowledgeBaseResponse create(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateKnowledgeBaseRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return knowledgeBaseService.create(projectId, request, user);
    }

    @PutMapping("/{id}")
    public KnowledgeBaseResponse update(
            @PathVariable UUID projectId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateKnowledgeBaseRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return knowledgeBaseService.update(projectId, id, request, user);
    }

    @PostMapping("/{id}/activate")
    public KnowledgeBaseResponse activate(
            @PathVariable UUID projectId,
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return knowledgeBaseService.activate(projectId, id, user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(
            @PathVariable UUID projectId,
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        knowledgeBaseService.archive(projectId, id, user);
    }
}
