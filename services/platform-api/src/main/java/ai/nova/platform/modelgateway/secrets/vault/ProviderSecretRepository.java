package ai.nova.platform.modelgateway.secrets.vault;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.modelgateway.entity.AiProviderType;

public interface ProviderSecretRepository extends JpaRepository<ProviderSecret, UUID> {

    Optional<ProviderSecret> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndSecretKey(UUID organizationId, String secretKey);

    @Query("""
            SELECT s FROM ProviderSecret s
            WHERE s.organizationId = :organizationId
              AND (:status IS NULL OR s.status = :status)
              AND (:providerType IS NULL OR s.providerType = :providerType)
              AND (:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(s.secretKey) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<ProviderSecret> search(
            @Param("organizationId") UUID organizationId,
            @Param("status") ProviderSecretStatus status,
            @Param("providerType") AiProviderType providerType,
            @Param("search") String search,
            Pageable pageable);
}
