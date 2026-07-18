package ai.nova.platform.orchestration.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class AgentTaskDependencyId implements Serializable {

    private UUID predecessorTaskId;
    private UUID successorTaskId;

    public AgentTaskDependencyId() {
    }

    public AgentTaskDependencyId(UUID predecessorTaskId, UUID successorTaskId) {
        this.predecessorTaskId = predecessorTaskId;
        this.successorTaskId = successorTaskId;
    }

    public UUID getPredecessorTaskId() { return predecessorTaskId; }
    public UUID getSuccessorTaskId() { return successorTaskId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AgentTaskDependencyId that)) {
            return false;
        }
        return Objects.equals(predecessorTaskId, that.predecessorTaskId)
                && Objects.equals(successorTaskId, that.successorTaskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predecessorTaskId, successorTaskId);
    }
}
