package ai.nova.platform.organization;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, UUID id);

    @Query("""
            SELECT o FROM Organization o
            WHERE (:search IS NULL
                   OR LOWER(o.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(o.slug) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<Organization> search(@Param("search") String search, Pageable pageable);

    @Query("""
            SELECT o FROM Organization o
            WHERE o.id = :organizationId
              AND (:search IS NULL
                   OR LOWER(o.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(o.slug) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<Organization> searchForMember(
            @Param("organizationId") UUID organizationId,
            @Param("search") String search,
            Pageable pageable);
}
