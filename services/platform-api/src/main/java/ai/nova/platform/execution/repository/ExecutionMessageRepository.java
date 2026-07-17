package ai.nova.platform.execution.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.execution.entity.ExecutionMessage;

public interface ExecutionMessageRepository extends JpaRepository<ExecutionMessage, UUID> {

    List<ExecutionMessage> findByExecutionIdOrderByCreatedAtAsc(UUID executionId);
}
