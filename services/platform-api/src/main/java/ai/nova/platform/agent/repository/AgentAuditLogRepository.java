package ai.nova.platform.agent.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.agent.entity.AgentAuditLog;

public interface AgentAuditLogRepository extends JpaRepository<AgentAuditLog, UUID> {
}
