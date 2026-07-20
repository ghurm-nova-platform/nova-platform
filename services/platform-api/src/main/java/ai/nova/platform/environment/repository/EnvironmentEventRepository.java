package ai.nova.platform.environment.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.environment.entity.EnvironmentEventEntity;

public interface EnvironmentEventRepository extends JpaRepository<EnvironmentEventEntity, UUID> {

    List<EnvironmentEventEntity> findByEnvironmentIdOrderByCreatedAtAsc(UUID environmentId);
}
