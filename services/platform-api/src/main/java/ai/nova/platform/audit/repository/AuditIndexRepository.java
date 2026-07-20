package ai.nova.platform.audit.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.audit.entity.AuditIndexEntity;

public interface AuditIndexRepository extends JpaRepository<AuditIndexEntity, UUID> {
}
