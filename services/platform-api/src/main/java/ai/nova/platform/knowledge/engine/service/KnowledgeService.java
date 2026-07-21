package ai.nova.platform.knowledge.engine.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSource;
import ai.nova.platform.audit.service.AuditRecordingSupport;
import ai.nova.platform.knowledge.engine.config.KnowledgeEngineProperties;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.AttachmentView;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.CreateDocumentRequest;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.DocumentDetail;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.DocumentSummary;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.KnowledgeEngineConfigResponse;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.RelateDocumentRequest;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.RelationView;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.UpdateDocumentRequest;
import ai.nova.platform.knowledge.engine.entity.Category;
import ai.nova.platform.knowledge.engine.entity.ContentFormat;
import ai.nova.platform.knowledge.engine.entity.DocumentStatus;
import ai.nova.platform.knowledge.engine.entity.KnowledgeAccessLogEntity;
import ai.nova.platform.knowledge.engine.entity.KnowledgeAttachmentEntity;
import ai.nova.platform.knowledge.engine.entity.KnowledgeDocumentEntity;
import ai.nova.platform.knowledge.engine.entity.KnowledgeRelationEntity;
import ai.nova.platform.knowledge.engine.entity.KnowledgeTagEntity;
import ai.nova.platform.knowledge.engine.entity.KnowledgeType;
import ai.nova.platform.knowledge.engine.entity.RelationType;
import ai.nova.platform.knowledge.engine.entity.Visibility;
import ai.nova.platform.knowledge.engine.repository.KnowledgeEngineAccessLogRepository;
import ai.nova.platform.knowledge.engine.repository.KnowledgeEngineAttachmentRepository;
import ai.nova.platform.knowledge.engine.repository.KnowledgeEngineDocumentRepository;
import ai.nova.platform.knowledge.engine.repository.KnowledgeEngineRelationRepository;
import ai.nova.platform.knowledge.engine.repository.KnowledgeEngineTagRepository;
import ai.nova.platform.knowledge.engine.security.KnowledgeEngineAuthorizationService;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class KnowledgeService {

    private final KnowledgeEngineProperties properties;
    private final KnowledgeEngineAuthorizationService authorizationService;
    private final KnowledgeVisibilityService visibilityService;
    private final KnowledgeIndexService indexService;
    private final KnowledgeSearchService searchService;
    private final KnowledgeEngineDocumentRepository documentRepository;
    private final KnowledgeEngineTagRepository tagRepository;
    private final KnowledgeEngineRelationRepository relationRepository;
    private final KnowledgeEngineAttachmentRepository attachmentRepository;
    private final KnowledgeEngineAccessLogRepository accessLogRepository;
    private final ProjectRepository projectRepository;
    private final AuditRecordingSupport auditRecordingSupport;

    public KnowledgeService(
            KnowledgeEngineProperties properties,
            KnowledgeEngineAuthorizationService authorizationService,
            KnowledgeVisibilityService visibilityService,
            KnowledgeIndexService indexService,
            KnowledgeSearchService searchService,
            KnowledgeEngineDocumentRepository documentRepository,
            KnowledgeEngineTagRepository tagRepository,
            KnowledgeEngineRelationRepository relationRepository,
            KnowledgeEngineAttachmentRepository attachmentRepository,
            KnowledgeEngineAccessLogRepository accessLogRepository,
            ProjectRepository projectRepository,
            AuditRecordingSupport auditRecordingSupport) {
        this.properties = properties;
        this.authorizationService = authorizationService;
        this.visibilityService = visibilityService;
        this.indexService = indexService;
        this.searchService = searchService;
        this.documentRepository = documentRepository;
        this.tagRepository = tagRepository;
        this.relationRepository = relationRepository;
        this.attachmentRepository = attachmentRepository;
        this.accessLogRepository = accessLogRepository;
        this.projectRepository = projectRepository;
        this.auditRecordingSupport = auditRecordingSupport;
    }

    @Transactional(readOnly = true)
    public KnowledgeEngineConfigResponse config(AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        return new KnowledgeEngineConfigResponse(
                properties.isEnabled(),
                properties.getCache().isEnabled(),
                properties.getCache().getTtlSeconds(),
                properties.getChunkSize(),
                properties.getChunkOverlap());
    }

    @Transactional(readOnly = true)
    public List<DocumentSummary> list(UUID projectId, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        validateProjectScope(user, projectId);
        List<KnowledgeDocumentEntity> documents = projectId == null
                ? documentRepository.findByOrganizationIdAndStatusNotOrderByUpdatedAtDesc(
                        user.getOrganizationId(), DocumentStatus.DELETED)
                : documentRepository.findByOrganizationIdAndProjectIdAndStatusNotOrderByUpdatedAtDesc(
                        user.getOrganizationId(), projectId, DocumentStatus.DELETED);
        return documents.stream()
                .filter(doc -> visibilityService.canRead(user, doc))
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentSummary> listByProject(UUID projectId, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        validateProject(user, projectId);
        return list(projectId, user);
    }

    @Transactional(readOnly = true)
    public DocumentDetail get(UUID id, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        KnowledgeDocumentEntity document = requireReadableDocument(id, user);
        recordAccess(document, user, "READ");
        audit(user, document, AuditAction.ACCESS, AuditResult.SUCCESS, Map.of("action", "READ"));
        return toDetail(document);
    }

    @Transactional
    public DocumentDetail create(CreateDocumentRequest request, AuthenticatedUser user) {
        authorizationService.requireWrite(user);
        requireEnabled();
        validateProjectScope(user, request.projectId());
        Instant now = Instant.now();
        Visibility visibility = request.visibility() == null ? Visibility.ORGANIZATION : request.visibility();
        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity(
                UUID.randomUUID(),
                user.getOrganizationId(),
                request.projectId(),
                request.title(),
                request.summary(),
                request.content(),
                request.contentFormat(),
                request.knowledgeType(),
                request.category(),
                DocumentStatus.ACTIVE,
                visibility,
                user.getUserId(),
                now);
        applyTags(document, request.tags(), user.getOrganizationId(), now);
        documentRepository.save(document);
        indexService.reindexDocument(document.getId(), document.getOrganizationId(), document.getContent(), now);
        searchService.invalidateOrganization(user.getOrganizationId());
        audit(user, document, AuditAction.CREATE, AuditResult.SUCCESS, Map.of());
        return toDetail(document);
    }

    @Transactional
    public DocumentDetail update(UUID id, UpdateDocumentRequest request, AuthenticatedUser user) {
        authorizationService.requireWrite(user);
        requireEnabled();
        KnowledgeDocumentEntity document = requireWritableDocument(id, user);
        if (request.projectId() != null) {
            validateProject(user, request.projectId());
            document.setProjectId(request.projectId());
        }
        if (request.title() != null) {
            document.setTitle(request.title());
        }
        if (request.summary() != null) {
            document.setSummary(request.summary());
        }
        if (request.content() != null) {
            document.setContent(request.content());
        }
        if (request.contentFormat() != null) {
            document.setContentFormat(request.contentFormat());
        }
        if (request.knowledgeType() != null) {
            document.setKnowledgeType(request.knowledgeType());
        }
        if (request.category() != null) {
            document.setCategory(request.category());
        }
        if (request.visibility() != null) {
            document.setVisibility(request.visibility());
        }
        if (request.tags() != null) {
            applyTags(document, request.tags(), user.getOrganizationId(), Instant.now());
        }
        Instant now = Instant.now();
        document.setVersion(document.getVersion() + 1);
        document.setUpdatedAt(now);
        documentRepository.save(document);
        indexService.reindexDocument(document.getId(), document.getOrganizationId(), document.getContent(), now);
        searchService.invalidateOrganization(user.getOrganizationId());
        audit(user, document, AuditAction.UPDATE, AuditResult.SUCCESS, Map.of());
        return toDetail(document);
    }

    @Transactional
    public DocumentDetail archive(UUID id, AuthenticatedUser user) {
        authorizationService.requireAdmin(user);
        requireEnabled();
        KnowledgeDocumentEntity document = requireDocument(id, user.getOrganizationId());
        document.setStatus(DocumentStatus.ARCHIVED);
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);
        searchService.invalidateOrganization(user.getOrganizationId());
        audit(user, document, AuditAction.ARCHIVE, AuditResult.SUCCESS, Map.of());
        return toDetail(document);
    }

    @Transactional
    public DocumentDetail restore(UUID id, AuthenticatedUser user) {
        authorizationService.requireAdmin(user);
        requireEnabled();
        KnowledgeDocumentEntity document = requireDocument(id, user.getOrganizationId());
        if (document.getStatus() != DocumentStatus.ARCHIVED) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "KNOWLEDGE_NOT_ARCHIVED", "Only archived documents can be restored");
        }
        document.setStatus(DocumentStatus.ACTIVE);
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);
        searchService.invalidateOrganization(user.getOrganizationId());
        audit(user, document, AuditAction.UPDATE, AuditResult.SUCCESS, Map.of("restored", true));
        return toDetail(document);
    }

    @Transactional
    public void delete(UUID id, AuthenticatedUser user) {
        authorizationService.requireAdmin(user);
        requireEnabled();
        KnowledgeDocumentEntity document = requireDocument(id, user.getOrganizationId());
        document.setStatus(DocumentStatus.DELETED);
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);
        searchService.invalidateOrganization(user.getOrganizationId());
        audit(user, document, AuditAction.DELETE, AuditResult.SUCCESS, Map.of());
    }

    @Transactional
    public List<RelationView> relate(UUID id, RelateDocumentRequest request, AuthenticatedUser user) {
        authorizationService.requireWrite(user);
        requireEnabled();
        KnowledgeDocumentEntity source = requireWritableDocument(id, user);
        if (request.targetDocumentId() != null) {
            requireDocument(request.targetDocumentId(), user.getOrganizationId());
        }
        Instant now = Instant.now();
        KnowledgeRelationEntity relation = new KnowledgeRelationEntity(
                UUID.randomUUID(),
                user.getOrganizationId(),
                source.getId(),
                request.targetDocumentId(),
                request.relationType(),
                request.targetRefId(),
                request.targetRefType(),
                now);
        relationRepository.save(relation);
        audit(user, source, AuditAction.UPDATE, AuditResult.SUCCESS, Map.of("relationType", request.relationType()));
        return relationsFor(source.getId(), user.getOrganizationId());
    }

    @Transactional(readOnly = true)
    public List<RelationView> relations(UUID id, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        requireReadableDocument(id, user);
        return relationsFor(id, user.getOrganizationId());
    }

    @Transactional(readOnly = true)
    public List<String> categories(AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        return java.util.Arrays.stream(Category.values()).map(Enum::name).toList();
    }

    @Transactional(readOnly = true)
    public List<String> tags(AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        return tagRepository.findByOrganizationIdOrderByNameAsc(user.getOrganizationId()).stream()
                .map(KnowledgeTagEntity::getName)
                .toList();
    }

    private List<RelationView> relationsFor(UUID documentId, UUID organizationId) {
        return relationRepository.findByOrganizationIdAndSourceDocumentId(organizationId, documentId).stream()
                .map(this::toRelationView)
                .toList();
    }

    private KnowledgeDocumentEntity requireDocument(UUID id, UUID organizationId) {
        return documentRepository
                .findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "KNOWLEDGE_NOT_FOUND", "Document not found"));
    }

    private KnowledgeDocumentEntity requireReadableDocument(UUID id, AuthenticatedUser user) {
        KnowledgeDocumentEntity document = requireDocument(id, user.getOrganizationId());
        if (document.getStatus() == DocumentStatus.DELETED) {
            throw new ApiException(HttpStatus.NOT_FOUND, "KNOWLEDGE_NOT_FOUND", "Document not found");
        }
        if (!visibilityService.canRead(user, document)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Document is not visible to this user");
        }
        return document;
    }

    private KnowledgeDocumentEntity requireWritableDocument(UUID id, AuthenticatedUser user) {
        KnowledgeDocumentEntity document = requireReadableDocument(id, user);
        if (document.getStatus() == DocumentStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "KNOWLEDGE_ARCHIVED", "Archived documents cannot be modified");
        }
        if (document.getVisibility() == Visibility.PRIVATE
                && !user.getUserId().equals(document.getAuthorId())
                && !user.getRoles().contains("ORG_ADMIN")
                && !user.hasPermission(KnowledgeEngineAuthorizationService.KNOWLEDGE_ADMIN)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the author can modify private documents");
        }
        return document;
    }

    private void validateProject(AuthenticatedUser user, UUID projectId) {
        projectRepository
                .findByIdAndOrganizationId(projectId, user.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));
    }

    private void validateProjectScope(AuthenticatedUser user, UUID projectId) {
        if (projectId != null) {
            validateProject(user, projectId);
        }
    }

    private void applyTags(
            KnowledgeDocumentEntity document, List<String> tagNames, UUID organizationId, Instant now) {
        if (tagNames == null) {
            return;
        }
        Set<KnowledgeTagEntity> tags = new HashSet<>();
        for (String tagName : tagNames) {
            if (tagName == null || tagName.isBlank()) {
                continue;
            }
            String normalized = tagName.trim();
            KnowledgeTagEntity tag = tagRepository
                    .findByOrganizationIdAndNameIgnoreCase(organizationId, normalized)
                    .orElseGet(() -> tagRepository.save(new KnowledgeTagEntity(UUID.randomUUID(), organizationId, normalized, now)));
            tags.add(tag);
        }
        document.setTags(tags);
    }

    private void recordAccess(KnowledgeDocumentEntity document, AuthenticatedUser user, String action) {
        accessLogRepository.save(new KnowledgeAccessLogEntity(
                UUID.randomUUID(),
                document.getId(),
                document.getOrganizationId(),
                user.getUserId(),
                action,
                Instant.now()));
    }

    private DocumentSummary toSummary(KnowledgeDocumentEntity document) {
        return new DocumentSummary(
                document.getId(),
                document.getOrganizationId(),
                document.getProjectId(),
                document.getTitle(),
                document.getSummary(),
                document.getContentFormat(),
                document.getKnowledgeType(),
                document.getCategory(),
                document.getStatus(),
                document.getVisibility(),
                document.getAuthorId(),
                document.getVersion(),
                tagNames(document),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }

    private DocumentDetail toDetail(KnowledgeDocumentEntity document) {
        List<KnowledgeAttachmentEntity> attachments =
                attachmentRepository.findByDocumentIdOrderByCreatedAtAsc(document.getId());
        return new DocumentDetail(
                document.getId(),
                document.getOrganizationId(),
                document.getProjectId(),
                document.getTitle(),
                document.getSummary(),
                document.getContent(),
                document.getContentFormat(),
                document.getKnowledgeType(),
                document.getCategory(),
                document.getStatus(),
                document.getVisibility(),
                document.getAuthorId(),
                document.getVersion(),
                tagNames(document),
                relationsFor(document.getId(), document.getOrganizationId()),
                attachments.stream().map(this::toAttachmentView).toList(),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }

    private RelationView toRelationView(KnowledgeRelationEntity relation) {
        return new RelationView(
                relation.getId(),
                relation.getRelationType(),
                relation.getTargetDocumentId(),
                relation.getTargetRefId(),
                relation.getTargetRefType(),
                relation.getCreatedAt());
    }

    private AttachmentView toAttachmentView(KnowledgeAttachmentEntity attachment) {
        return new AttachmentView(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getStorageRef(),
                attachment.getSizeBytes(),
                attachment.getCreatedAt());
    }

    private List<String> tagNames(KnowledgeDocumentEntity document) {
        return document.getTags().stream().map(KnowledgeTagEntity::getName).sorted().toList();
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE, "KNOWLEDGE_ENGINE_DISABLED", "Knowledge engine is disabled");
        }
    }

    private void audit(
            AuthenticatedUser user,
            KnowledgeDocumentEntity document,
            AuditAction action,
            AuditResult result,
            Map<String, Object> details) {
        Map<String, Object> payload = new LinkedHashMap<>(details);
        auditRecordingSupport.recordDomainEvent(
                user,
                document.getProjectId(),
                AuditEntityType.KNOWLEDGE,
                document.getId(),
                document.getTitle(),
                action,
                result,
                AuditSource.KNOWLEDGE,
                payload);
    }
}
