package ai.nova.platform.project;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);

    boolean existsByOrganizationIdAndNameIgnoreCaseAndIdNot(UUID organizationId, String name, UUID id);

    Optional<Project> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("""
            SELECT p FROM Project p
            WHERE p.organizationId = :organizationId
              AND (
                   :search IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
              )
            """)
    Page<Project> searchByOrganization(
            @Param("organizationId") UUID organizationId,
            @Param("search") String search,
            Pageable pageable);
}
