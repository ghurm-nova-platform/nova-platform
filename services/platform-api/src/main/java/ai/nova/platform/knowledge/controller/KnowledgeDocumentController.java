package ai.nova.platform.knowledge.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ai.nova.platform.knowledge.dto.KnowledgeDtos.KnowledgeChunkResponse;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.KnowledgeDocumentResponse;
import ai.nova.platform.knowledge.entity.KnowledgeDocumentStatus;
import ai.nova.platform.knowledge.service.KnowledgeDocumentService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/projects/{projectId}/knowledge-bases/{knowledgeBaseId}/documents")
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService documentService;

    public KnowledgeDocumentController(KnowledgeDocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public Page<KnowledgeDocumentResponse> list(
            @PathVariable UUID projectId,
            @PathVariable UUID knowledgeBaseId,
            @RequestParam(required = false) KnowledgeDocumentStatus status,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return documentService.list(projectId, knowledgeBaseId, status, pageable, user);
    }

    @GetMapping("/{documentId}")
    public KnowledgeDocumentResponse get(
            @PathVariable UUID projectId,
            @PathVariable UUID knowledgeBaseId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return documentService.get(projectId, knowledgeBaseId, documentId, user);
    }

    @GetMapping("/{documentId}/chunks")
    public List<KnowledgeChunkResponse> chunks(
            @PathVariable UUID projectId,
            @PathVariable UUID knowledgeBaseId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return documentService.listChunks(projectId, knowledgeBaseId, documentId, user);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public KnowledgeDocumentResponse upload(
            @PathVariable UUID projectId,
            @PathVariable UUID knowledgeBaseId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String documentKey,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return documentService.upload(projectId, knowledgeBaseId, file, documentKey, user);
    }

    @PostMapping("/{documentId}/reprocess")
    public KnowledgeDocumentResponse reprocess(
            @PathVariable UUID projectId,
            @PathVariable UUID knowledgeBaseId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return documentService.reprocess(projectId, knowledgeBaseId, documentId, user);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(
            @PathVariable UUID projectId,
            @PathVariable UUID knowledgeBaseId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        documentService.archive(projectId, knowledgeBaseId, documentId, user);
    }
}
