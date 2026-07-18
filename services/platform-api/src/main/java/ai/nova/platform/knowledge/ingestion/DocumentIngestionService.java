package ai.nova.platform.knowledge.ingestion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.knowledge.chunking.ParagraphAwareTextChunker;
import ai.nova.platform.knowledge.chunking.TextChunk;
import ai.nova.platform.knowledge.embedding.EmbeddingCodec;
import ai.nova.platform.knowledge.embedding.EmbeddingProvider;
import ai.nova.platform.knowledge.embedding.EmbeddingProviderRegistry;
import ai.nova.platform.knowledge.entity.KnowledgeBase;
import ai.nova.platform.knowledge.entity.KnowledgeChunk;
import ai.nova.platform.knowledge.entity.KnowledgeDocument;
import ai.nova.platform.knowledge.entity.KnowledgeDocumentContent;
import ai.nova.platform.knowledge.entity.KnowledgeDocumentStatus;
import ai.nova.platform.knowledge.entity.KnowledgeEmbedding;
import ai.nova.platform.knowledge.extractor.DocumentTextExtractorRegistry;
import ai.nova.platform.knowledge.repository.KnowledgeChunkRepository;
import ai.nova.platform.knowledge.repository.KnowledgeDocumentContentRepository;
import ai.nova.platform.knowledge.repository.KnowledgeDocumentRepository;
import ai.nova.platform.knowledge.repository.KnowledgeEmbeddingRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeDocumentContentRepository contentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeEmbeddingRepository embeddingRepository;
    private final DocumentTextExtractorRegistry extractorRegistry;
    private final ParagraphAwareTextChunker chunker;
    private final EmbeddingProviderRegistry embeddingProviderRegistry;
    private final KnowledgeDocumentFailureService failureService;

    public DocumentIngestionService(
            KnowledgeDocumentRepository documentRepository,
            KnowledgeDocumentContentRepository contentRepository,
            KnowledgeChunkRepository chunkRepository,
            KnowledgeEmbeddingRepository embeddingRepository,
            DocumentTextExtractorRegistry extractorRegistry,
            ParagraphAwareTextChunker chunker,
            EmbeddingProviderRegistry embeddingProviderRegistry,
            KnowledgeDocumentFailureService failureService) {
        this.documentRepository = documentRepository;
        this.contentRepository = contentRepository;
        this.chunkRepository = chunkRepository;
        this.embeddingRepository = embeddingRepository;
        this.extractorRegistry = extractorRegistry;
        this.chunker = chunker;
        this.embeddingProviderRegistry = embeddingProviderRegistry;
        this.failureService = failureService;
    }

    @Transactional
    public KnowledgeDocument process(
            KnowledgeDocument document, KnowledgeBase knowledgeBase, byte[] rawBytes, UUID actorId) {
        KnowledgeDocument managed = reload(document.getId());
        managed.setStatus(KnowledgeDocumentStatus.PROCESSING);
        managed.setIngestionErrorCode(null);
        managed.setUpdatedBy(actorId);
        managed.setUpdatedAt(Instant.now());
        managed = documentRepository.saveAndFlush(managed);

        try {
            String extracted = extractorRegistry
                    .require(managed.getDocumentType())
                    .extract(rawBytes, managed.getFileName(), managed.getMediaType());
            return finishProcessing(managed, knowledgeBase, extracted, actorId);
        } catch (ApiException ex) {
            failureService.markFailed(managed.getId(), actorId, ex.getCode());
            throw ex;
        } catch (RuntimeException ex) {
            log.warn(
                    "Document ingestion failed for document {} with safeErrorCode={}",
                    managed.getId(),
                    "DOCUMENT_EXTRACTION_FAILED");
            failureService.markFailed(managed.getId(), actorId, "DOCUMENT_EXTRACTION_FAILED");
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "DOCUMENT_EXTRACTION_FAILED",
                    "Document ingestion failed");
        }
    }

    @Transactional
    public KnowledgeDocument processFromExtractedText(
            KnowledgeDocument document, KnowledgeBase knowledgeBase, String extractedText, UUID actorId) {
        KnowledgeDocument managed = reload(document.getId());
        managed.setStatus(KnowledgeDocumentStatus.PROCESSING);
        managed.setIngestionErrorCode(null);
        managed.setUpdatedBy(actorId);
        managed.setUpdatedAt(Instant.now());
        managed = documentRepository.saveAndFlush(managed);

        try {
            return finishProcessing(managed, knowledgeBase, extractedText, actorId);
        } catch (ApiException ex) {
            failureService.markFailed(managed.getId(), actorId, ex.getCode());
            throw ex;
        } catch (RuntimeException ex) {
            log.warn(
                    "Document reprocess failed for document {} with safeErrorCode={}",
                    managed.getId(),
                    "DOCUMENT_STORAGE_FAILED");
            failureService.markFailed(managed.getId(), actorId, "DOCUMENT_STORAGE_FAILED");
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "DOCUMENT_STORAGE_FAILED",
                    "Document ingestion failed");
        }
    }

    private KnowledgeDocument finishProcessing(
            KnowledgeDocument document, KnowledgeBase knowledgeBase, String extracted, UUID actorId) {
        KnowledgeDocumentContent content = contentRepository
                .findByDocumentIdAndProjectIdAndOrganizationId(
                        document.getId(), document.getProjectId(), document.getOrganizationId())
                .orElse(null);
        if (content == null) {
            content = new KnowledgeDocumentContent(
                    document.getId(),
                    document.getOrganizationId(),
                    document.getProjectId(),
                    document.getKnowledgeBaseId(),
                    extracted,
                    Instant.now());
        } else {
            content.setExtractedText(extracted);
        }
        contentRepository.saveAndFlush(content);

        embeddingRepository.deleteByDocumentIdAndProjectIdAndOrganizationId(
                document.getId(), document.getProjectId(), document.getOrganizationId());
        chunkRepository.deleteByDocumentIdAndProjectIdAndOrganizationId(
                document.getId(), document.getProjectId(), document.getOrganizationId());
        chunkRepository.flush();

        List<TextChunk> textChunks =
                chunker.chunk(extracted, knowledgeBase.getChunkSize(), knowledgeBase.getChunkOverlap());
        EmbeddingProvider provider = embeddingProviderRegistry.require(knowledgeBase.getEmbeddingProviderKey());
        if (!provider.model().equals(knowledgeBase.getEmbeddingModel())
                || provider.dimensions() != knowledgeBase.getEmbeddingDimensions()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EMBEDDING_PROVIDER_MISMATCH",
                    "Knowledge base embedding configuration does not match provider");
        }

        Instant now = Instant.now();
        List<KnowledgeChunk> chunks = new ArrayList<>();
        List<KnowledgeEmbedding> embeddings = new ArrayList<>();
        for (TextChunk textChunk : textChunks) {
            UUID chunkId = UUID.randomUUID();
            int start = Math.max(0, textChunk.characterStart());
            int end = Math.max(start + Math.max(1, textChunk.content().length()), textChunk.characterEnd());
            if (end <= start) {
                end = start + 1;
            }
            KnowledgeChunk chunk = new KnowledgeChunk(
                    chunkId,
                    document.getOrganizationId(),
                    document.getProjectId(),
                    document.getKnowledgeBaseId(),
                    document.getId(),
                    textChunk.index(),
                    textChunk.content(),
                    textChunk.contentHash(),
                    start,
                    end,
                    Math.max(1, textChunk.content().trim().split("\\s+").length),
                    null,
                    now);
            chunks.add(chunk);
            float[] vector = provider.embed(textChunk.content());
            embeddings.add(new KnowledgeEmbedding(
                    UUID.randomUUID(),
                    document.getOrganizationId(),
                    document.getProjectId(),
                    document.getKnowledgeBaseId(),
                    document.getId(),
                    chunkId,
                    provider.providerKey(),
                    provider.model(),
                    provider.dimensions(),
                    EmbeddingCodec.encode(vector),
                    EmbeddingCodec.hash(vector),
                    now));
        }
        if (!chunks.isEmpty()) {
            chunkRepository.saveAll(chunks);
            embeddingRepository.saveAll(embeddings);
            chunkRepository.flush();
        }

        KnowledgeDocument latest = reload(document.getId());
        latest.setExtractedCharacterCount(extracted.length());
        latest.setChunkCount(chunks.size());
        latest.setStatus(KnowledgeDocumentStatus.READY);
        latest.setProcessedAt(now);
        latest.setUpdatedBy(actorId);
        latest.setUpdatedAt(now);
        return documentRepository.saveAndFlush(latest);
    }

    private KnowledgeDocument reload(UUID documentId) {
        return documentRepository
                .findById(documentId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "KNOWLEDGE_DOCUMENT_NOT_FOUND", "Document not found"));
    }
}
