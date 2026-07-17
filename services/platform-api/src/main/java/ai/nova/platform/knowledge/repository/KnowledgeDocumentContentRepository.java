package ai.nova.platform.knowledge.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.knowledge.entity.KnowledgeDocumentContent;

public interface KnowledgeDocumentContentRepository extends JpaRepository<KnowledgeDocumentContent, UUID> {

    Optional<KnowledgeDocumentContent> findByDocumentIdAndProjectIdAndOrganizationId(
            UUID documentId, UUID projectId, UUID organizationId);
}
