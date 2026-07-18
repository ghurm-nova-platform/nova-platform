package ai.nova.platform.git.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.git.entity.GitBranchEntity;

public interface GitBranchRepository extends JpaRepository<GitBranchEntity, UUID> {

    List<GitBranchEntity> findByGitOperationIdOrderByCreatedAtAsc(UUID gitOperationId);
}
