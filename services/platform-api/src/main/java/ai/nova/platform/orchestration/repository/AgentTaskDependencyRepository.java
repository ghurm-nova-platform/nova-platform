package ai.nova.platform.orchestration.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.orchestration.entity.AgentTaskDependency;
import ai.nova.platform.orchestration.entity.AgentTaskDependencyId;

public interface AgentTaskDependencyRepository
        extends JpaRepository<AgentTaskDependency, AgentTaskDependencyId> {

    List<AgentTaskDependency> findByRunId(UUID runId);

    List<AgentTaskDependency> findBySuccessorTaskId(UUID successorTaskId);

    List<AgentTaskDependency> findByPredecessorTaskId(UUID predecessorTaskId);

    boolean existsByPredecessorTaskIdAndSuccessorTaskId(UUID predecessorTaskId, UUID successorTaskId);

    void deleteByRunIdAndPredecessorTaskIdAndSuccessorTaskId(
            UUID runId, UUID predecessorTaskId, UUID successorTaskId);

    void deleteByPredecessorTaskIdOrSuccessorTaskId(UUID predecessorTaskId, UUID successorTaskId);
}
