package ai.nova.platform.pullrequest.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.pullrequest.entity.RemotePushEntity;

public interface RemotePushRepository extends JpaRepository<RemotePushEntity, UUID> {

    Optional<RemotePushEntity> findFirstByPullRequestOperationIdOrderByStartedAtDesc(UUID pullRequestOperationId);

    List<RemotePushEntity> findByPullRequestOperationIdOrderByStartedAtAsc(UUID pullRequestOperationId);
}
