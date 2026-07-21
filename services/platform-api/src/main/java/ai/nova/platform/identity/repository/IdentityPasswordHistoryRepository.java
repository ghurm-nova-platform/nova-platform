package ai.nova.platform.identity.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.identity.entity.IdentityPasswordHistoryEntity;

public interface IdentityPasswordHistoryRepository extends JpaRepository<IdentityPasswordHistoryEntity, UUID> {

    List<IdentityPasswordHistoryEntity> findTop10ByIdentityUserIdOrderByCreatedAtDesc(UUID identityUserId);
}
