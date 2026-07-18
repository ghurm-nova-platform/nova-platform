package ai.nova.platform.modelcatalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.modelcatalog.entity.AiModelAlias;

public interface AiModelAliasRepository extends JpaRepository<AiModelAlias, UUID> {

    Optional<AiModelAlias> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<AiModelAlias> findByOrganizationIdAndNormalizedAlias(UUID organizationId, String normalizedAlias);

    List<AiModelAlias> findByModelIdAndOrganizationIdOrderByCreatedAtAsc(UUID modelId, UUID organizationId);

    boolean existsByOrganizationIdAndNormalizedAlias(UUID organizationId, String normalizedAlias);

    void deleteByModelIdAndOrganizationId(UUID modelId, UUID organizationId);
}
