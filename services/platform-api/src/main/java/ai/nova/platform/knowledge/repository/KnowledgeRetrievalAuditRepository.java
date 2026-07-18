package ai.nova.platform.knowledge.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.knowledge.entity.KnowledgeRetrievalAudit;

public interface KnowledgeRetrievalAuditRepository extends JpaRepository<KnowledgeRetrievalAudit, UUID> {
}
