package ai.nova.platform.modelgateway.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.modelgateway.entity.AiModel;
import ai.nova.platform.modelgateway.entity.AiModelStatus;

public interface AiModelRepository extends JpaRepository<AiModel, UUID> {

    Optional<AiModel> findByIdAndProviderIdAndOrganizationId(UUID id, UUID providerId, UUID organizationId);

    boolean existsByProviderIdAndModelKey(UUID providerId, String modelKey);

    @Query("""
            SELECT m FROM AiModel m
            WHERE m.organizationId = :organizationId
              AND m.providerId = :providerId
              AND (:status IS NULL OR m.status = :status)
              AND (:search IS NULL OR LOWER(m.displayName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(m.modelKey) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<AiModel> search(
            @Param("organizationId") UUID organizationId,
            @Param("providerId") UUID providerId,
            @Param("status") AiModelStatus status,
            @Param("search") String search,
            Pageable pageable);

    List<AiModel> findByIdInAndOrganizationId(List<UUID> ids, UUID organizationId);
}
