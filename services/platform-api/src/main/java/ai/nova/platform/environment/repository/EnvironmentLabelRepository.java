package ai.nova.platform.environment.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.environment.entity.EnvironmentLabelEntity;

public interface EnvironmentLabelRepository extends JpaRepository<EnvironmentLabelEntity, UUID> {

    List<EnvironmentLabelEntity> findByEnvironmentIdOrderByLabelKeyAsc(UUID environmentId);

    void deleteByEnvironmentId(UUID environmentId);
}
