package ai.nova.platform.knowledge.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import ai.nova.platform.knowledge.entity.KnowledgeBaseStatus;
import ai.nova.platform.knowledge.entity.KnowledgeDocumentStatus;
import ai.nova.platform.knowledge.entity.KnowledgeDocumentType;

public final class KnowledgeDtos {

    private KnowledgeDtos() {
    }

    public record CreateKnowledgeBaseRequest(
            @NotBlank @Size(max = 100) String knowledgeKey,
            @NotBlank @Size(max = 255) String name,
            @Size(max = 2000) String description,
            @NotBlank String embeddingProviderKey,
            @Min(100) @Max(5000) Integer chunkSize,
            @Min(0) Integer chunkOverlap,
            @Min(1) @Max(20) Integer defaultTopK,
            @DecimalMin("-1.0") @DecimalMax("1.0") BigDecimal minimumScore) {
    }

    public record UpdateKnowledgeBaseRequest(
            @NotNull Integer version,
            @NotBlank @Size(max = 255) String name,
            @Size(max = 2000) String description,
            @Min(100) @Max(5000) Integer chunkSize,
            @Min(0) Integer chunkOverlap,
            @Min(1) @Max(20) Integer defaultTopK,
            @DecimalMin("-1.0") @DecimalMax("1.0") BigDecimal minimumScore) {
    }

    public record KnowledgeBaseResponse(
            UUID id,
            UUID projectId,
            String knowledgeKey,
            String name,
            String description,
            KnowledgeBaseStatus status,
            String embeddingProviderKey,
            String embeddingModel,
            int embeddingDimensions,
            int chunkSize,
            int chunkOverlap,
            int defaultTopK,
            BigDecimal minimumScore,
            Instant createdAt,
            Instant updatedAt,
            int version) {
    }

    public record KnowledgeDocumentResponse(
            UUID id,
            UUID knowledgeBaseId,
            String documentKey,
            String fileName,
            String mediaType,
            KnowledgeDocumentType documentType,
            KnowledgeDocumentStatus status,
            String contentHash,
            long fileSizeBytes,
            Integer extractedCharacterCount,
            int chunkCount,
            String ingestionErrorCode,
            Instant createdAt,
            Instant updatedAt,
            Instant processedAt,
            int version) {
    }

    public record KnowledgeChunkResponse(
            UUID id,
            UUID documentId,
            int chunkIndex,
            String content,
            String contentHash,
            int characterStart,
            int characterEnd,
            Integer tokenEstimate) {
    }

    public record EmbeddingProviderResponse(
            String providerKey,
            String model,
            int dimensions) {
    }

    public record EmbeddingProvidersResponse(List<EmbeddingProviderResponse> providers) {
    }

    public record AgentKnowledgeAssignRequest(
            @NotNull UUID knowledgeBaseId,
            Integer topKOverride,
            BigDecimal minimumScoreOverride) {
    }

    public record AgentKnowledgeUpdateRequest(
            @NotNull Integer version,
            Boolean enabled,
            Integer topKOverride,
            BigDecimal minimumScoreOverride) {
    }

    public record AgentKnowledgeAssignmentResponse(
            UUID id,
            UUID agentId,
            UUID knowledgeBaseId,
            String knowledgeBaseName,
            String knowledgeBaseKey,
            KnowledgeBaseStatus knowledgeBaseStatus,
            boolean enabled,
            Integer topKOverride,
            BigDecimal minimumScoreOverride,
            Instant createdAt,
            Instant updatedAt,
            int version) {
    }

    public record KnowledgeCitationResponse(
            String label,
            UUID knowledgeBaseId,
            String knowledgeBaseName,
            UUID documentId,
            String documentName,
            int chunkIndex,
            double score) {
    }
}
