package ai.nova.platform.knowledge.engine.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.CreateDocumentRequest;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.DocumentDetail;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.DocumentSummary;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.ExportPayload;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.ImportDocumentRequest;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.KnowledgeEngineConfigResponse;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.MemoryDocument;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.RelateDocumentRequest;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.RelationView;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.SearchResult;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.UpdateDocumentRequest;
import ai.nova.platform.knowledge.engine.entity.Category;
import ai.nova.platform.knowledge.engine.entity.KnowledgeType;
import ai.nova.platform.knowledge.engine.entity.Visibility;
import ai.nova.platform.knowledge.engine.service.KnowledgeImportExportService;
import ai.nova.platform.knowledge.engine.service.KnowledgeMemoryService;
import ai.nova.platform.knowledge.engine.service.KnowledgeSearchService;
import ai.nova.platform.knowledge.engine.service.KnowledgeService;
import ai.nova.platform.security.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/knowledge")
@Validated
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final KnowledgeSearchService searchService;
    private final KnowledgeMemoryService memoryService;
    private final KnowledgeImportExportService importExportService;

    public KnowledgeController(
            KnowledgeService knowledgeService,
            KnowledgeSearchService searchService,
            KnowledgeMemoryService memoryService,
            KnowledgeImportExportService importExportService) {
        this.knowledgeService = knowledgeService;
        this.searchService = searchService;
        this.memoryService = memoryService;
        this.importExportService = importExportService;
    }

    @GetMapping("/config")
    public KnowledgeEngineConfigResponse config(@AuthenticationPrincipal AuthenticatedUser user) {
        return knowledgeService.config(user);
    }

    @GetMapping
    public List<DocumentSummary> list(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return knowledgeService.list(projectId, user);
    }

    @GetMapping("/search")
    public List<SearchResult> search(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "category", required = false) Category category,
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @RequestParam(value = "authorId", required = false) UUID authorId,
            @RequestParam(value = "visibility", required = false) Visibility visibility,
            @RequestParam(value = "knowledgeType", required = false) KnowledgeType knowledgeType,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant fromDate,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant toDate,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return searchService.search(
                user, query, tag, category, projectId, authorId, visibility, knowledgeType, fromDate, toDate);
    }

    @GetMapping("/memory")
    public List<MemoryDocument> memory(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @RequestParam(value = "types", required = false) List<KnowledgeType> knowledgeTypes,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return memoryService.getRelevantDocuments(user, projectId, knowledgeTypes, limit);
    }

    @GetMapping("/categories")
    public List<String> categories(@AuthenticationPrincipal AuthenticatedUser user) {
        return knowledgeService.categories(user);
    }

    @GetMapping("/tags")
    public List<String> tags(@AuthenticationPrincipal AuthenticatedUser user) {
        return knowledgeService.tags(user);
    }

    @GetMapping("/project/{projectId}")
    public List<DocumentSummary> listByProject(
            @PathVariable UUID projectId, @AuthenticationPrincipal AuthenticatedUser user) {
        return knowledgeService.listByProject(projectId, user);
    }

    @GetMapping("/{id}")
    public DocumentDetail get(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return knowledgeService.get(id, user);
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(
            @PathVariable UUID id,
            @RequestParam(value = "format", defaultValue = "markdown") String format,
            @AuthenticationPrincipal AuthenticatedUser user) {
        DocumentDetail document = knowledgeService.get(id, user);
        ExportPayload payload = importExportService.exportDocument(document, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + payload.fileName() + "\"")
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .body(payload.content());
    }

    @GetMapping("/{id}/relations")
    public List<RelationView> relations(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return knowledgeService.relations(id, user);
    }

    @PostMapping({"", "/"})
    public DocumentDetail create(
            @Valid @RequestBody CreateDocumentRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return knowledgeService.create(request, user);
    }

    @PostMapping("/import")
    public DocumentDetail importDocument(
            @Valid @RequestBody ImportDocumentRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return importExportService.importDocument(request, user);
    }

    @PostMapping("/{id}/archive")
    public DocumentDetail archive(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return knowledgeService.archive(id, user);
    }

    @PostMapping("/{id}/restore")
    public DocumentDetail restore(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return knowledgeService.restore(id, user);
    }

    @PostMapping("/{id}/relate")
    public List<RelationView> relate(
            @PathVariable UUID id,
            @Valid @RequestBody RelateDocumentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return knowledgeService.relate(id, request, user);
    }

    @PutMapping("/{id}")
    public DocumentDetail update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDocumentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return knowledgeService.update(id, request, user);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        knowledgeService.delete(id, user);
    }
}
