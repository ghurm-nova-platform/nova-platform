package ai.nova.platform.identity.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.identity.entity.IdentityPermissionEntity;

public interface IdentityPermissionRepository extends JpaRepository<IdentityPermissionEntity, UUID> {

    List<IdentityPermissionEntity> findByOrganizationIdOrderByCodeAsc(UUID organizationId);

    Optional<IdentityPermissionEntity> findByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
}
