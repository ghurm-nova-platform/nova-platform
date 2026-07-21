package ai.nova.platform.audit.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import ai.nova.platform.audit.entity.AuditEventEntity;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID>, JpaSpecificationExecutor<AuditEventEntity> {

    Optional<AuditEventEntity> findByOrganizationIdAndEventFingerprint(UUID organizationId, String eventFingerprint);

    Optional<AuditEventEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<AuditEventEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    List<AuditEventEntity> findByOrganizationIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
            UUID organizationId, ai.nova.platform.audit.entity.AuditEntityType entityType, UUID entityId);
}
