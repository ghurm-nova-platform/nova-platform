package ai.nova.platform.knowledge.engine.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.knowledge.engine.entity.KnowledgeChunkEntity;

public interface KnowledgeEngineChunkRepository extends JpaRepository<KnowledgeChunkEntity, UUID> {

    List<KnowledgeChunkEntity> findByDocumentIdOrderByChunkNumberAsc(UUID documentId);

    void deleteByDocumentId(UUID documentId);
}

