package ai.nova.platform.knowledge.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import ai.nova.platform.knowledge.chunking.ParagraphAwareTextChunker;
import ai.nova.platform.knowledge.embedding.EmbeddingProviderRegistry;
import ai.nova.platform.knowledge.entity.KnowledgeBase;
import ai.nova.platform.knowledge.entity.KnowledgeBaseStatus;
import ai.nova.platform.knowledge.entity.KnowledgeDocument;
import ai.nova.platform.knowledge.entity.KnowledgeDocumentStatus;
import ai.nova.platform.knowledge.entity.KnowledgeDocumentType;
import ai.nova.platform.knowledge.extractor.DocumentTextExtractor;
import ai.nova.platform.knowledge.extractor.DocumentTextExtractorRegistry;
import ai.nova.platform.knowledge.repository.KnowledgeChunkRepository;
import ai.nova.platform.knowledge.repository.KnowledgeDocumentContentRepository;
import ai.nova.platform.knowledge.repository.KnowledgeDocumentRepository;
import ai.nova.platform.knowledge.repository.KnowledgeEmbeddingRepository;
import ai.nova.platform.web.error.ApiException;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServicePrivacyTest {

    private static final UUID DOC_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PROJECT_ID = UUID.fromString("55555555-5555-5555-5555-555555555501");
    private static final UUID KB_ID = UUID.fromString("88888888-8888-8888-8888-888888888801");
    private static final UUID ACTOR_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");
    private static final String SECRET_CONTENT = "TOP_SECRET_DOCUMENT_BODY_SHOULD_NOT_LEAK";
    private static final String INTERNAL_EXCEPTION_MESSAGE =
            "Extractor blew up while reading: " + SECRET_CONTENT;

    @Mock
    private KnowledgeDocumentRepository documentRepository;
    @Mock
    private KnowledgeDocumentContentRepository contentRepository;
    @Mock
    private KnowledgeChunkRepository chunkRepository;
    @Mock
    private KnowledgeEmbeddingRepository embeddingRepository;
    @Mock
    private DocumentTextExtractorRegistry extractorRegistry;
    @Mock
    private DocumentTextExtractor extractor;
    @Mock
    private ParagraphAwareTextChunker chunker;
    @Mock
    private EmbeddingProviderRegistry embeddingProviderRegistry;
    @Mock
    private KnowledgeDocumentFailureService failureService;

    private DocumentIngestionService service;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        service = new DocumentIngestionService(
                documentRepository,
                contentRepository,
                chunkRepository,
                embeddingRepository,
                extractorRegistry,
                chunker,
                embeddingProviderRegistry,
                failureService);
        logger = (Logger) LoggerFactory.getLogger(DocumentIngestionService.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.WARN);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Test
    void processDoesNotExposeExceptionDetailsOrDocumentContent() {
        KnowledgeDocument document = document();
        when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(document));
        when(documentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(extractorRegistry.require(KnowledgeDocumentType.TEXT)).thenReturn(extractor);
        when(extractor.extract(any(), anyString(), anyString()))
                .thenThrow(new RuntimeException(INTERNAL_EXCEPTION_MESSAGE));

        assertThatThrownBy(() -> service.process(document, knowledgeBase(), SECRET_CONTENT.getBytes(), ACTOR_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getCode()).isEqualTo("DOCUMENT_EXTRACTION_FAILED");
                    assertThat(apiEx.getMessage()).isEqualTo("Document ingestion failed");
                    assertThat(apiEx.getMessage()).doesNotContain(SECRET_CONTENT);
                    assertThat(apiEx.getMessage()).doesNotContain(INTERNAL_EXCEPTION_MESSAGE);
                    assertThat(apiEx.getMessage()).doesNotContain("RuntimeException");
                });

        verify(failureService).markFailed(eq(DOC_ID), eq(ACTOR_ID), eq("DOCUMENT_EXTRACTION_FAILED"));

        assertThat(listAppender.list).isNotEmpty();
        for (ILoggingEvent event : listAppender.list) {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).contains(DOC_ID.toString());
            assertThat(event.getFormattedMessage()).contains("DOCUMENT_EXTRACTION_FAILED");
            assertThat(event.getFormattedMessage()).doesNotContain(SECRET_CONTENT);
            assertThat(event.getFormattedMessage()).doesNotContain(INTERNAL_EXCEPTION_MESSAGE);
            assertThat(event.getThrowableProxy()).isNull();
        }
    }

    @Test
    void processFromExtractedTextDoesNotExposeExceptionDetails() {
        KnowledgeDocument document = document();
        when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(document));
        when(documentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(contentRepository.findByDocumentIdAndProjectIdAndOrganizationId(any(), any(), any()))
                .thenThrow(new RuntimeException("DB failed for content: " + SECRET_CONTENT));

        assertThatThrownBy(
                        () -> service.processFromExtractedText(document, knowledgeBase(), SECRET_CONTENT, ACTOR_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getCode()).isEqualTo("DOCUMENT_STORAGE_FAILED");
                    assertThat(apiEx.getMessage()).isEqualTo("Document ingestion failed");
                    assertThat(apiEx.getMessage()).doesNotContain(SECRET_CONTENT);
                });

        for (ILoggingEvent event : listAppender.list) {
            assertThat(event.getFormattedMessage()).doesNotContain(SECRET_CONTENT);
            assertThat(event.getThrowableProxy()).isNull();
        }
    }

    private static KnowledgeDocument document() {
        KnowledgeDocument document = new KnowledgeDocument(
                DOC_ID,
                ORG_ID,
                PROJECT_ID,
                KB_ID,
                "DOC_KEY",
                "notes.txt",
                "text/plain",
                KnowledgeDocumentType.TEXT,
                KnowledgeDocumentStatus.UPLOADED,
                "abc",
                12L,
                ACTOR_ID,
                Instant.now());
        return document;
    }

    private static KnowledgeBase knowledgeBase() {
        return new KnowledgeBase(
                KB_ID,
                ORG_ID,
                PROJECT_ID,
                "PRODUCT_DOCUMENTATION",
                "Product documentation",
                "demo",
                KnowledgeBaseStatus.ACTIVE,
                "DETERMINISTIC_LOCAL",
                "deterministic-v1",
                64,
                1000,
                150,
                5,
                null,
                ACTOR_ID,
                Instant.now());
    }
}
