package ai.nova.platform.knowledge.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.nova.platform.agent.runtime.RuntimeKnowledgeChunk;
import ai.nova.platform.agent.runtime.RuntimeKnowledgeCitation;
import ai.nova.platform.agent.runtime.RuntimeKnowledgeContext;
import ai.nova.platform.knowledge.config.KnowledgeProperties;
import ai.nova.platform.knowledge.entity.ExecutionKnowledgeSnapshot;
import ai.nova.platform.knowledge.repository.ExecutionKnowledgeSnapshotRepository;

/**
 * Persists a bounded knowledge snapshot for an execution so tool-approval
 * continuation can restore the original grounding context without re-retrieval.
 * Never stores embeddings.
 */
@Service
public class ExecutionKnowledgeSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionKnowledgeSnapshotService.class);

    private final ExecutionKnowledgeSnapshotRepository repository;
    private final KnowledgeProperties properties;
    private final ObjectMapper objectMapper;

    public ExecutionKnowledgeSnapshotService(
            ExecutionKnowledgeSnapshotRepository repository,
            KnowledgeProperties properties,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void saveIfAbsent(
            UUID executionId,
            UUID organizationId,
            UUID projectId,
            RuntimeKnowledgeContext context) {
        if (context == null || context.isEmpty()) {
            return;
        }
        if (repository.existsById(executionId)) {
            return;
        }
        RuntimeKnowledgeContext bounded = bound(context);
        try {
            String json = serialize(bounded);
            int totalCharacters = bounded.chunks().stream().mapToInt(chunk -> chunk.content().length()).sum();
            repository.save(new ExecutionKnowledgeSnapshot(
                    executionId,
                    organizationId,
                    projectId,
                    json,
                    bounded.citations().size(),
                    totalCharacters,
                    Instant.now()));
        } catch (DataIntegrityViolationException ex) {
            log.debug("Knowledge snapshot already exists for execution {}", executionId);
        } catch (JsonProcessingException ex) {
            log.warn(
                    "Failed to persist knowledge snapshot for execution {} with safeErrorCode=KNOWLEDGE_SNAPSHOT_FAILED",
                    executionId);
        }
    }

    @Transactional(readOnly = true)
    public RuntimeKnowledgeContext load(UUID executionId, UUID projectId, UUID organizationId) {
        Optional<ExecutionKnowledgeSnapshot> found =
                repository.findByExecutionIdAndProjectIdAndOrganizationId(executionId, projectId, organizationId);
        if (found.isEmpty()) {
            return RuntimeKnowledgeContext.empty();
        }
        try {
            return deserialize(found.get().getSnapshotJson());
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            log.warn(
                    "Failed to load knowledge snapshot for execution {} with safeErrorCode=KNOWLEDGE_SNAPSHOT_INVALID",
                    executionId);
            return RuntimeKnowledgeContext.empty();
        }
    }

    private RuntimeKnowledgeContext bound(RuntimeKnowledgeContext context) {
        int remaining = properties.getMaxRetrievedCharacters();
        List<RuntimeKnowledgeChunk> chunks = new ArrayList<>();
        List<RuntimeKnowledgeCitation> citations = new ArrayList<>();
        for (int i = 0; i < context.chunks().size(); i++) {
            RuntimeKnowledgeChunk chunk = context.chunks().get(i);
            if (remaining <= 0) {
                break;
            }
            String content = chunk.content();
            if (content.length() > remaining) {
                content = content.substring(0, remaining);
            }
            remaining -= content.length();
            chunks.add(new RuntimeKnowledgeChunk(
                    chunk.label(),
                    chunk.chunkId(),
                    chunk.knowledgeBaseId(),
                    chunk.documentId(),
                    chunk.chunkIndex(),
                    content,
                    chunk.score()));
            if (i < context.citations().size()) {
                citations.add(context.citations().get(i));
            }
        }
        return new RuntimeKnowledgeContext(citations, chunks);
    }

    private String serialize(RuntimeKnowledgeContext context) throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode citations = root.putArray("citations");
        for (RuntimeKnowledgeCitation citation : context.citations()) {
            ObjectNode node = citations.addObject();
            node.put("label", citation.label());
            node.put("knowledgeBaseId", citation.knowledgeBaseId().toString());
            node.put("knowledgeBaseName", citation.knowledgeBaseName());
            node.put("documentId", citation.documentId().toString());
            node.put("documentName", citation.documentName());
            node.put("chunkIndex", citation.chunkIndex());
            node.put("score", citation.score());
        }
        ArrayNode chunks = root.putArray("chunks");
        for (RuntimeKnowledgeChunk chunk : context.chunks()) {
            ObjectNode node = chunks.addObject();
            node.put("label", chunk.label());
            node.put("chunkId", chunk.chunkId().toString());
            node.put("knowledgeBaseId", chunk.knowledgeBaseId().toString());
            node.put("documentId", chunk.documentId().toString());
            node.put("chunkIndex", chunk.chunkIndex());
            node.put("content", chunk.content());
            node.put("score", chunk.score());
        }
        return objectMapper.writeValueAsString(root);
    }

    private RuntimeKnowledgeContext deserialize(String json) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(json);
        List<RuntimeKnowledgeCitation> citations = new ArrayList<>();
        for (JsonNode node : root.path("citations")) {
            citations.add(new RuntimeKnowledgeCitation(
                    node.path("label").asText(),
                    UUID.fromString(node.path("knowledgeBaseId").asText()),
                    node.path("knowledgeBaseName").asText(),
                    UUID.fromString(node.path("documentId").asText()),
                    node.path("documentName").asText(),
                    node.path("chunkIndex").asInt(),
                    node.path("score").asDouble()));
        }
        List<RuntimeKnowledgeChunk> chunks = new ArrayList<>();
        for (JsonNode node : root.path("chunks")) {
            chunks.add(new RuntimeKnowledgeChunk(
                    node.path("label").asText(),
                    UUID.fromString(node.path("chunkId").asText()),
                    UUID.fromString(node.path("knowledgeBaseId").asText()),
                    UUID.fromString(node.path("documentId").asText()),
                    node.path("chunkIndex").asInt(),
                    node.path("content").asText(),
                    node.path("score").asDouble()));
        }
        return new RuntimeKnowledgeContext(citations, chunks);
    }
}
