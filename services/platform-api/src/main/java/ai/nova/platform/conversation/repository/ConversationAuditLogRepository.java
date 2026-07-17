package ai.nova.platform.conversation.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.conversation.entity.ConversationAuditLog;

public interface ConversationAuditLogRepository extends JpaRepository<ConversationAuditLog, UUID> {
}
