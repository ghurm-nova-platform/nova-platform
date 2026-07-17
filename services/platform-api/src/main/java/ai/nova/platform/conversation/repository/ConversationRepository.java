package ai.nova.platform.conversation.repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.conversation.entity.Conversation;
import ai.nova.platform.conversation.entity.ConversationStatus;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByIdAndProjectIdAndOrganizationId(
            UUID id, UUID projectId, UUID organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c FROM Conversation c
            WHERE c.id = :id
              AND c.projectId = :projectId
              AND c.organizationId = :organizationId
            """)
    Optional<Conversation> findForUpdate(
            @Param("id") UUID id,
            @Param("projectId") UUID projectId,
            @Param("organizationId") UUID organizationId);

    @Query("""
            SELECT c FROM Conversation c
            WHERE c.organizationId = :organizationId
              AND c.projectId = :projectId
              AND (:agentId IS NULL OR c.agentId = :agentId)
              AND (:status IS NULL OR c.status = :status)
              AND (
                   :search IS NULL
                   OR LOWER(COALESCE(c.title, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
              )
            """)
    Page<Conversation> search(
            @Param("organizationId") UUID organizationId,
            @Param("projectId") UUID projectId,
            @Param("agentId") UUID agentId,
            @Param("status") ConversationStatus status,
            @Param("search") String search,
            Pageable pageable);
}
