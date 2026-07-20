package ai.nova.platform.audit.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.audit.entity.AuditCorrelationEntity;

public interface AuditCorrelationRepository extends JpaRepository<AuditCorrelationEntity, UUID> {

    List<AuditCorrelationEntity> findByOrganizationIdAndCorrelationIdOrderByCreatedAtAscChainSequenceAsc(
            UUID organizationId, String correlationId);

    List<AuditCorrelationEntity> findByOrganizationIdAndRequestIdOrderByCreatedAtAscChainSequenceAsc(
            UUID organizationId, String requestId);

    List<AuditCorrelationEntity> findByOrganizationIdAndSessionIdOrderByCreatedAtAscChainSequenceAsc(
            UUID organizationId, UUID sessionId);
}
