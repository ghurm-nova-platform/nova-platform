package ai.nova.platform.pullrequest.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.pullrequest.entity.PullRequestRecordEntity;

public interface PullRequestRecordRepository extends JpaRepository<PullRequestRecordEntity, UUID> {

    Optional<PullRequestRecordEntity> findFirstByPullRequestOperationIdOrderByCreatedAtDesc(
            UUID pullRequestOperationId);
}
