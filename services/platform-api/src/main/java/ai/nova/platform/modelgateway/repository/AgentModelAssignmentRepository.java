package ai.nova.platform.modelgateway.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.modelgateway.entity.AgentModelAssignment;
import ai.nova.platform.modelgateway.entity.AssignmentRole;

public interface AgentModelAssignmentRepository extends JpaRepository<AgentModelAssignment, UUID> {

    List<AgentModelAssignment> findByAgentIdAndProjectIdAndOrganizationIdOrderByAssignmentRoleAscPriorityAscModelIdAsc(
            UUID agentId, UUID projectId, UUID organizationId);

    List<AgentModelAssignment> findByAgentIdAndProjectIdAndOrganizationIdAndEnabledTrueOrderByAssignmentRoleAscPriorityAscModelIdAsc(
            UUID agentId, UUID projectId, UUID organizationId);

    Optional<AgentModelAssignment> findByIdAndAgentIdAndProjectIdAndOrganizationId(
            UUID id, UUID agentId, UUID projectId, UUID organizationId);

    Optional<AgentModelAssignment> findByAgentIdAndModelIdAndProjectIdAndOrganizationId(
            UUID agentId, UUID modelId, UUID projectId, UUID organizationId);

    boolean existsByAgentIdAndAssignmentRoleAndPriorityAndProjectIdAndOrganizationId(
            UUID agentId, AssignmentRole role, Integer priority, UUID projectId, UUID organizationId);
}
