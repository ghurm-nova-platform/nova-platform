package ai.nova.platform.modelgateway.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.modelgateway.entity.AiProvider;
import ai.nova.platform.modelgateway.entity.AiProviderStatus;

public interface AiProviderRepository extends JpaRepository<AiProvider, UUID> {

    Optional<AiProvider> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndProviderKey(UUID organizationId, String providerKey);

    @Query("""
            SELECT p FROM AiProvider p
            WHERE p.organizationId = :organizationId
              AND (:status IS NULL OR p.status = :status)
              AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(p.providerKey) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<AiProvider> search(
            @Param("organizationId") UUID organizationId,
            @Param("status") AiProviderStatus status,
            @Param("search") String search,
            Pageable pageable);
}
