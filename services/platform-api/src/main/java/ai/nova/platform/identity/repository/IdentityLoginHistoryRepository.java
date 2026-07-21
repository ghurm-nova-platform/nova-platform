package ai.nova.platform.identity.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.identity.entity.IdentityLoginHistoryEntity;

public interface IdentityLoginHistoryRepository extends JpaRepository<IdentityLoginHistoryEntity, UUID> {

    List<IdentityLoginHistoryEntity> findTop50ByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
