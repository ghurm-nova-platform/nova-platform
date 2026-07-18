package ai.nova.platform.knowledge.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import ai.nova.platform.knowledge.config.KnowledgeProperties;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.KnowledgeChunkResponse;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.KnowledgeDocumentResponse;
import ai.nova.platform.knowledge.entity.KnowledgeBase;
import ai.nova.platform.knowledge.entity.KnowledgeBaseStatus;
import ai.nova.platform.knowledge.entity.KnowledgeDocument;
import ai.nova.platform.knowledge.entity.KnowledgeDocumentStatus;
import ai.nova.platform.knowledge.entity.KnowledgeDocumentType;
import ai.nova.platform.knowledge.ingestion.DocumentIngestionService;
import ai.nova.platform.knowledge.mapper.KnowledgeMapper;
import ai.nova.platform.knowledge.repository.KnowledgeChunkRepository;
import ai.nova.platform.knowledge.repository.KnowledgeDocumentContentRepository;
import ai.nova.platform.knowledge.repository.KnowledgeDocumentRepository;
import ai.nova.platform.knowledge.security.KnowledgeAuthorizationService;
import ai.nova.platform.knowledge.validation.DocumentTypeResolver;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class KnowledgeDocumentService {

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeDocumentContentRepository contentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentIngestionService ingestionService;
    private final DocumentTypeResolver documentTypeResolver;
    private final KnowledgeProperties properties;
    private final KnowledgeMapper mapper;
    private final KnowledgeAuthorizationService authorizationService;
    private final ProjectRepository projectRepository;

    public KnowledgeDocumentService(
            KnowledgeDocumentRepository documentRepository,
            KnowledgeDocumentContentRepository contentRepository,
            KnowledgeChunkRepository chunkRepository,
            KnowledgeBaseService knowledgeBaseService,
            DocumentIngestionService ingestionService,
            DocumentTypeResolver documentTypeResolver,
            KnowledgeProperties properties,
            KnowledgeMapper mapper,
            KnowledgeAuthorizationService authorizationService,
            ProjectRepository projectRepository) {
        this.documentRepository = documentRepository;
        this.contentRepository = contentRepository;
        this.chunkRepository = chunkRepository;
        this.knowledgeBaseService = knowledgeBaseService;
        this.ingestionService = ingestionService;
        this.documentTypeResolver = documentTypeResolver;
        this.properties = properties;
        this.mapper = mapper;
        this.authorizationService = authorizationService;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public Page<KnowledgeDocumentResponse> list(
            UUID projectId,
            UUID knowledgeBaseId,
            KnowledgeDocumentStatus status,
            Pageable pageable,
            AuthenticatedUser user) {
        authorizationService.require(user, KnowledgeAuthorizationService.KNOWLEDGE_DOCUMENT_READ);
        knowledgeBaseService.requireKnowledgeBase(projectId, knowledgeBaseId, user.getOrganizationId());
        return documentRepository
                .search(knowledgeBaseId, projectId, user.getOrganizationId(), status, pageable)
                .map(mapper::toDocumentResponse);
    }

    @Transactional(readOnly = true)
    public KnowledgeDocumentResponse get(
            UUID projectId, UUID knowledgeBaseId, UUID documentId, AuthenticatedUser user) {
        authorizationService.require(user, KnowledgeAuthorizationService.KNOWLEDGE_DOCUMENT_READ);
        return mapper.toDocumentResponse(
                requireDocument(projectId, knowledgeBaseId, documentId, user.getOrganizationId()));
    }

    @Transactional(readOnly = true)
    public List<KnowledgeChunkResponse> listChunks(
            UUID projectId, UUID knowledgeBaseId, UUID documentId, AuthenticatedUser user) {
        authorizationService.require(user, KnowledgeAuthorizationService.KNOWLEDGE_DOCUMENT_READ);
        requireDocument(projectId, knowledgeBaseId, documentId, user.getOrganizationId());
        return chunkRepository
                .findByDocumentIdAndProjectIdAndOrganizationIdOrderByChunkIndexAsc(
                        documentId, projectId, user.getOrganizationId())
                .stream()
                .map(mapper::toChunkResponse)
                .toList();
    }

    public KnowledgeDocumentResponse upload(
            UUID projectId,
            UUID knowledgeBaseId,
            MultipartFile file,
            String documentKey,
            AuthenticatedUser user) {
        authorizationService.require(user, KnowledgeAuthorizationService.KNOWLEDGE_DOCUMENT_UPLOAD);
        requireProject(projectId, user.getOrganizationId());
        KnowledgeBase kb =
                knowledgeBaseService.requireKnowledgeBase(projectId, knowledgeBaseId, user.getOrganizationId());
        if (kb.getStatus() == KnowledgeBaseStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "KNOWLEDGE_ARCHIVED", "Knowledge base is archived");
        }

        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DOCUMENT_EMPTY", "Document content is empty");
        }
        if (file.getSize() > properties.getMaxFileBytes()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DOCUMENT_TOO_LARGE", "File exceeds maximum size");
        }

        String fileName = file.getOriginalFilename() == null ? "document.txt" : file.getOriginalFilename();
        String mediaType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        KnowledgeDocumentType type = documentTypeResolver.resolve(fileName, mediaType);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DOCUMENT_READ_FAILED", "Unable to read uploaded file");
        }
        if (bytes.length == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DOCUMENT_EMPTY", "Document content is empty");
        }

        String contentHash = sha256(bytes);
        synchronized (duplicateLock(knowledgeBaseId, contentHash)) {
            List<KnowledgeDocument> duplicates = documentRepository.findNonArchivedByKnowledgeBaseIdAndContentHash(
                    knowledgeBaseId, contentHash, KnowledgeDocumentStatus.ARCHIVED);
            if (!duplicates.isEmpty()) {
                throw new ApiException(
                        HttpStatus.CONFLICT, "DOCUMENT_DUPLICATE_CONTENT", "Duplicate document content");
            }

            String key = resolveDocumentKey(documentKey, fileName);
            if (documentRepository.existsByKnowledgeBaseIdAndDocumentKey(knowledgeBaseId, key)) {
                throw new ApiException(HttpStatus.CONFLICT, "DOCUMENT_KEY_EXISTS", "Document key already exists");
            }

            Instant now = Instant.now();
            KnowledgeDocument document = new KnowledgeDocument(
                    UUID.randomUUID(),
                    user.getOrganizationId(),
                    projectId,
                    knowledgeBaseId,
                    key,
                    fileName,
                    mediaType,
                    type,
                    KnowledgeDocumentStatus.UPLOADED,
                    contentHash,
                    (long) bytes.length,
                    user.getUserId(),
                    now);
            document = documentRepository.saveAndFlush(document);
            document = ingestionService.process(document, kb, bytes, user.getUserId());
            return mapper.toDocumentResponse(document);
        }
    }

    private static Object duplicateLock(UUID knowledgeBaseId, String contentHash) {
        return ("kb-content-lock:" + knowledgeBaseId + ":" + contentHash).intern();
    }

    public KnowledgeDocumentResponse reprocess(
            UUID projectId, UUID knowledgeBaseId, UUID documentId, AuthenticatedUser user) {
        authorizationService.require(user, KnowledgeAuthorizationService.KNOWLEDGE_DOCUMENT_REPROCESS);
        KnowledgeBase kb =
                knowledgeBaseService.requireKnowledgeBase(projectId, knowledgeBaseId, user.getOrganizationId());
        if (kb.getStatus() == KnowledgeBaseStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "KNOWLEDGE_ARCHIVED", "Knowledge base is archived");
        }
        KnowledgeDocument document =
                requireDocument(projectId, knowledgeBaseId, documentId, user.getOrganizationId());
        if (document.getStatus() == KnowledgeDocumentStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "DOCUMENT_ARCHIVED", "Document is archived");
        }

        var content = contentRepository
                .findByDocumentIdAndProjectIdAndOrganizationId(
                        documentId, projectId, user.getOrganizationId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT, "DOCUMENT_CONTENT_MISSING", "Document content is missing"));
        document = ingestionService.processFromExtractedText(
                document, kb, content.getExtractedText(), user.getUserId());
        return mapper.toDocumentResponse(document);
    }

    @Transactional
    public void archive(UUID projectId, UUID knowledgeBaseId, UUID documentId, AuthenticatedUser user) {
        authorizationService.require(user, KnowledgeAuthorizationService.KNOWLEDGE_DOCUMENT_ARCHIVE);
        KnowledgeDocument document =
                requireDocument(projectId, knowledgeBaseId, documentId, user.getOrganizationId());
        if (document.getStatus() == KnowledgeDocumentStatus.ARCHIVED) {
            return;
        }
        document.setStatus(KnowledgeDocumentStatus.ARCHIVED);
        document.setUpdatedBy(user.getUserId());
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);
    }

    private String resolveDocumentKey(String documentKey, String fileName) {
        String key;
        if (documentKey != null && !documentKey.isBlank()) {
            key = documentKey.trim();
        } else {
            String base = fileName;
            int dot = fileName.lastIndexOf('.');
            if (dot > 0) {
                base = fileName.substring(0, dot);
            }
            key = base.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]+", "_");
            if (key.isBlank()) {
                key = "DOC_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
            }
        }
        if (key.length() > properties.getMaxDocumentKeyLength()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "documentKey too long");
        }
        return key;
    }

    private KnowledgeDocument requireDocument(
            UUID projectId, UUID knowledgeBaseId, UUID documentId, UUID organizationId) {
        return documentRepository
                .findByIdAndKnowledgeBaseIdAndProjectIdAndOrganizationId(
                        documentId, knowledgeBaseId, projectId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "KNOWLEDGE_DOCUMENT_NOT_FOUND", "Document not found"));
    }

    private void requireProject(UUID projectId, UUID organizationId) {
        projectRepository
                .findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
