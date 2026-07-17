package ai.nova.platform.prompt.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.prompt.entity.PromptAuditLog;

public interface PromptAuditLogRepository extends JpaRepository<PromptAuditLog, UUID> {
}
