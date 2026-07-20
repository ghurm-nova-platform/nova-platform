package ai.nova.platform.environment.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.environment.entity.EnvironmentVariableMetadataEntity;

public interface EnvironmentVariableMetadataRepository extends JpaRepository<EnvironmentVariableMetadataEntity, UUID> {

    List<EnvironmentVariableMetadataEntity> findByEnvironmentIdOrderByVariableNameAsc(UUID environmentId);

    void deleteByEnvironmentId(UUID environmentId);
}
