package ai.nova.platform.git.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.git.entity.GitCommitEntity;

public interface GitCommitRepository extends JpaRepository<GitCommitEntity, UUID> {

    List<GitCommitEntity> findByGitOperationIdOrderByCreatedAtAsc(UUID gitOperationId);
}
