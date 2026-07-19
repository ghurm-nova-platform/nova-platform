package ai.nova.platform.pullrequest.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.pullrequest.entity.PullRequestOperationEntity;
import ai.nova.platform.pullrequest.entity.PullRequestStatus;

public interface PullRequestOperationRepository extends JpaRepository<PullRequestOperationEntity, UUID> {

    Optional<PullRequestOperationEntity> findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID taskId, UUID organizationId);

    List<PullRequestOperationEntity> findByTaskIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID taskId, UUID organizationId);

    Optional<PullRequestOperationEntity> findFirstByGitOperationIdAndStatus(
            UUID gitOperationId, PullRequestStatus status);
}
