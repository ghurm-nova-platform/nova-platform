package ai.nova.platform.identity.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.identity.entity.IdentityRoleEntity;

public interface IdentityRoleRepository extends JpaRepository<IdentityRoleEntity, UUID> {

    List<IdentityRoleEntity> findByOrganizationIdOrderByCodeAsc(UUID organizationId);

    Optional<IdentityRoleEntity> findByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
}
