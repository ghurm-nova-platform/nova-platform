package ai.nova.platform.knowledge.engine.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.knowledge.engine.entity.KnowledgeAttachmentEntity;

public interface KnowledgeEngineAttachmentRepository extends JpaRepository<KnowledgeAttachmentEntity, UUID> {

    List<KnowledgeAttachmentEntity> findByDocumentIdOrderByCreatedAtAsc(UUID documentId);
}

