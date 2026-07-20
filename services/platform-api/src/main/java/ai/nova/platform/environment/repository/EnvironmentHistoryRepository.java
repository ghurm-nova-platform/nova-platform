package ai.nova.platform.environment.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.environment.entity.EnvironmentHistoryEntity;

public interface EnvironmentHistoryRepository extends JpaRepository<EnvironmentHistoryEntity, UUID> {

    List<EnvironmentHistoryEntity> findByEnvironmentIdOrderByCreatedAtDesc(UUID environmentId);
}
