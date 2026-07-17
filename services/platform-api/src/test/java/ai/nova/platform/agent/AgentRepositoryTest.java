package ai.nova.platform.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import ai.nova.platform.agent.entity.AgentStatus;
import ai.nova.platform.agent.repository.AgentRepository;

@SpringBootTest
class AgentRepositoryTest {

    @Autowired
    private AgentRepository agentRepository;

    @Test
    void findsSeededAgentByProjectAndStatus() {
        UUID organizationId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID projectId = UUID.fromString("55555555-5555-5555-5555-555555555501");

        assertThat(agentRepository.existsByProjectIdAndNameIgnoreCase(projectId, "Demo Code Reviewer")).isTrue();
        assertThat(agentRepository
                        .search(organizationId, projectId, "Demo", AgentStatus.ACTIVE, PageRequest.of(0, 10))
                        .getContent())
                .isNotEmpty();
    }
}
