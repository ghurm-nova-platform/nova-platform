package ai.nova.platform.identity.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.identity.entity.IdentityLoginHistoryEntity;
import ai.nova.platform.identity.entity.LoginResult;

public interface IdentityLoginHistoryRepository extends JpaRepository<IdentityLoginHistoryEntity, UUID> {

    List<IdentityLoginHistoryEntity> findTop50ByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<IdentityLoginHistoryEntity> findTop20ByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    long countByOrganizationIdAndResultAndCreatedAtAfter(
            UUID organizationId, LoginResult result, Instant since);
}
