package ai.nova.platform.orchestration.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.orchestration.entity.AgentTaskAttempt;

public interface AgentTaskAttemptRepository extends JpaRepository<AgentTaskAttempt, UUID> {

    List<AgentTaskAttempt> findByTaskIdAndOrganizationIdOrderByAttemptNumberAsc(UUID taskId, UUID organizationId);

    Optional<AgentTaskAttempt> findByTaskIdAndAttemptNumberAndOrganizationId(
            UUID taskId, int attemptNumber, UUID organizationId);

    boolean existsByTaskId(UUID taskId);

    long countByTaskId(UUID taskId);
}
