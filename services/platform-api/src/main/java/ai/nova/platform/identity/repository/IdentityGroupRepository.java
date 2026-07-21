package ai.nova.platform.identity.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.identity.entity.IdentityGroupEntity;

public interface IdentityGroupRepository extends JpaRepository<IdentityGroupEntity, UUID> {

    List<IdentityGroupEntity> findByOrganizationIdOrderByNameAsc(UUID organizationId);
}
