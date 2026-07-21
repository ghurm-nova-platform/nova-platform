package ai.nova.platform.knowledge.engine.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import ai.nova.platform.knowledge.engine.entity.Category;
import ai.nova.platform.knowledge.engine.entity.ContentFormat;
import ai.nova.platform.knowledge.engine.entity.DocumentStatus;
import ai.nova.platform.knowledge.engine.entity.KnowledgeType;
import ai.nova.platform.knowledge.engine.entity.RelationType;
import ai.nova.platform.knowledge.engine.entity.Visibility;

public final class KnowledgeEngineDtos {

    private KnowledgeEngineDtos() {
    }

    public record KnowledgeEngineConfigResponse(
            boolean enabled,
            boolean cacheEnabled,
            int cacheTtlSeconds,
            int chunkSize,
            int chunkOverlap) {
    }

    public record CreateDocumentRequest(
            UUID projectId,
            @NotBlank @Size(max = 500) String title,
            @Size(max = 2000) String summary,
            @NotBlank String content,
            @NotNull ContentFormat contentFormat,
            @NotNull KnowledgeType knowledgeType,
            @NotNull Category category,
            Visibility visibility,
            List<@NotBlank @Size(max = 100) String> tags) {
    }

    public record UpdateDocumentRequest(
            UUID projectId,
            @Size(max = 500) String title,
            @Size(max = 2000) String summary,
            String content,
            ContentFormat contentFormat,
            KnowledgeType knowledgeType,
            Category category,
            Visibility visibility,
            List<@NotBlank @Size(max = 100) String> tags) {
    }

    public record ImportDocumentRequest(
            UUID projectId,
            @NotBlank @Size(max = 500) String title,
            @Size(max = 2000) String summary,
            @NotBlank String content,
            ContentFormat contentFormat,
            KnowledgeType knowledgeType,
            Category category,
            Visibility visibility,
            List<@NotBlank @Size(max = 100) String> tags,
            String importFormat) {
    }

    public record RelateDocumentRequest(
            @NotNull RelationType relationType,
            UUID targetDocumentId,
            UUID targetRefId,
            String targetRefType) {
    }

    public record DocumentSummary(
            UUID id,
            UUID organizationId,
            UUID projectId,
            String title,
            String summary,
            ContentFormat contentFormat,
            KnowledgeType knowledgeType,
            Category category,
            DocumentStatus status,
            Visibility visibility,
            UUID authorId,
            int version,
            List<String> tags,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record DocumentDetail(
            UUID id,
            UUID organizationId,
            UUID projectId,
            String title,
            String summary,
            String content,
            ContentFormat contentFormat,
            KnowledgeType knowledgeType,
            Category category,
            DocumentStatus status,
            Visibility visibility,
            UUID authorId,
            int version,
            List<String> tags,
            List<RelationView> relations,
            List<AttachmentView> attachments,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record RelationView(
            UUID id,
            RelationType relationType,
            UUID targetDocumentId,
            UUID targetRefId,
            String targetRefType,
            Instant createdAt) {
    }

    public record AttachmentView(
            UUID id,
            String fileName,
            String contentType,
            String storageRef,
            long sizeBytes,
            Instant createdAt) {
    }

    public record SearchResult(
            UUID id,
            String title,
            String summary,
            KnowledgeType knowledgeType,
            Category category,
            Visibility visibility,
            UUID projectId,
            UUID authorId,
            List<String> tags,
            String matchedSnippet,
            Instant updatedAt) {
    }

    public record MemoryDocument(
            UUID id,
            String title,
            String summary,
            KnowledgeType knowledgeType,
            Category category,
            UUID projectId,
            Instant updatedAt) {
    }

    public record ExportPayload(byte[] content, String contentType, String fileName) {
    }
}
