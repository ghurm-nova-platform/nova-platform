package ai.nova.platform.knowledge.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.knowledge.entity.AgentKnowledgeAssignment;

public interface AgentKnowledgeAssignmentRepository extends JpaRepository<AgentKnowledgeAssignment, UUID> {

    List<AgentKnowledgeAssignment> findByAgentIdAndProjectIdAndOrganizationIdOrderByCreatedAtAsc(
            UUID agentId, UUID projectId, UUID organizationId);

    List<AgentKnowledgeAssignment> findByAgentIdAndProjectIdAndOrganizationIdAndEnabledTrueOrderByCreatedAtAsc(
            UUID agentId, UUID projectId, UUID organizationId);

    Optional<AgentKnowledgeAssignment> findByAgentIdAndKnowledgeBaseIdAndProjectIdAndOrganizationId(
            UUID agentId, UUID knowledgeBaseId, UUID projectId, UUID organizationId);

    List<AgentKnowledgeAssignment> findByKnowledgeBaseIdAndProjectIdAndOrganizationId(
            UUID knowledgeBaseId, UUID projectId, UUID organizationId);

    boolean existsByAgentIdAndProjectIdAndOrganizationIdAndEnabledTrue(
            UUID agentId, UUID projectId, UUID organizationId);
}
