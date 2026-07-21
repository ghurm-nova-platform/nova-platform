package ai.nova.platform.audit.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.audit.entity.AuditEntityEntity;
import ai.nova.platform.audit.entity.AuditEntityType;

public interface AuditEntityRepository extends JpaRepository<AuditEntityEntity, UUID> {

    Optional<AuditEntityEntity> findByOrganizationIdAndEntityTypeAndEntityId(
            UUID organizationId, AuditEntityType entityType, UUID entityId);
}
