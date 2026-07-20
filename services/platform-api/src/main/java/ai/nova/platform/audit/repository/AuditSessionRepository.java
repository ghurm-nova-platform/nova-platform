package ai.nova.platform.audit.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.audit.entity.AuditSessionEntity;

public interface AuditSessionRepository extends JpaRepository<AuditSessionEntity, UUID> {

    Optional<AuditSessionEntity> findBySessionId(UUID sessionId);
}
