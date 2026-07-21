package ai.nova.platform.knowledge.engine.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.knowledge.engine.entity.KnowledgeAccessLogEntity;

public interface KnowledgeEngineAccessLogRepository extends JpaRepository<KnowledgeAccessLogEntity, UUID> {

    List<KnowledgeAccessLogEntity> findByDocumentIdOrderByCreatedAtDesc(UUID documentId);
}

