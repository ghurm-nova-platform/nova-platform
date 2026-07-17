package ai.nova.platform.knowledge.mapper;

import org.springframework.stereotype.Component;

import ai.nova.platform.knowledge.dto.KnowledgeDtos.AgentKnowledgeAssignmentResponse;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.KnowledgeBaseResponse;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.KnowledgeChunkResponse;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.KnowledgeDocumentResponse;
import ai.nova.platform.knowledge.entity.AgentKnowledgeAssignment;
import ai.nova.platform.knowledge.entity.KnowledgeBase;
import ai.nova.platform.knowledge.entity.KnowledgeChunk;
import ai.nova.platform.knowledge.entity.KnowledgeDocument;

@Component
public class KnowledgeMapper {

    public KnowledgeBaseResponse toBaseResponse(KnowledgeBase kb) {
        return new KnowledgeBaseResponse(
                kb.getId(),
                kb.getProjectId(),
                kb.getKnowledgeKey(),
                kb.getName(),
                kb.getDescription(),
                kb.getStatus(),
                kb.getEmbeddingProviderKey(),
                kb.getEmbeddingModel(),
                kb.getEmbeddingDimensions(),
                kb.getChunkSize(),
                kb.getChunkOverlap(),
                kb.getDefaultTopK(),
                kb.getMinimumScore(),
                kb.getCreatedAt(),
                kb.getUpdatedAt(),
                kb.getVersion());
    }

    public KnowledgeDocumentResponse toDocumentResponse(KnowledgeDocument document) {
        return new KnowledgeDocumentResponse(
                document.getId(),
                document.getKnowledgeBaseId(),
                document.getDocumentKey(),
                document.getFileName(),
                document.getMediaType(),
                document.getDocumentType(),
                document.getStatus(),
                document.getContentHash(),
                document.getFileSizeBytes(),
                document.getExtractedCharacterCount(),
                document.getChunkCount(),
                document.getIngestionErrorCode(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                document.getProcessedAt(),
                document.getVersion());
    }

    public KnowledgeChunkResponse toChunkResponse(KnowledgeChunk chunk) {
        return new KnowledgeChunkResponse(
                chunk.getId(),
                chunk.getDocumentId(),
                chunk.getChunkIndex(),
                chunk.getContent(),
                chunk.getContentHash(),
                chunk.getCharacterStart(),
                chunk.getCharacterEnd(),
                chunk.getTokenEstimate());
    }

    public AgentKnowledgeAssignmentResponse toAssignmentResponse(
            AgentKnowledgeAssignment assignment, KnowledgeBase kb) {
        return new AgentKnowledgeAssignmentResponse(
                assignment.getId(),
                assignment.getAgentId(),
                assignment.getKnowledgeBaseId(),
                kb.getName(),
                kb.getKnowledgeKey(),
                kb.getStatus(),
                assignment.isEnabled(),
                assignment.getTopKOverride(),
                assignment.getMinimumScoreOverride(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt(),
                assignment.getVersion());
    }
}
