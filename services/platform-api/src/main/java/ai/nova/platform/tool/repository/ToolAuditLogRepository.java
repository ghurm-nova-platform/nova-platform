package ai.nova.platform.tool.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.tool.entity.ToolAuditLog;

public interface ToolAuditLogRepository extends JpaRepository<ToolAuditLog, UUID> {
}
