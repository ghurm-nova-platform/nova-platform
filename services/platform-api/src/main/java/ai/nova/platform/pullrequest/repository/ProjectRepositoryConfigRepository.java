package ai.nova.platform.pullrequest.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.pullrequest.entity.ProjectRepositoryConfigEntity;

public interface ProjectRepositoryConfigRepository extends JpaRepository<ProjectRepositoryConfigEntity, UUID> {

    Optional<ProjectRepositoryConfigEntity> findByOrganizationIdAndProjectId(
            UUID organizationId, UUID projectId);
}
