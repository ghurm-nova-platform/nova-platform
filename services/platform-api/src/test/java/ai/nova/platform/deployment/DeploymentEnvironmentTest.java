package ai.nova.platform.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.deployment.entity.EnvironmentType;
import ai.nova.platform.deployment.repository.DeploymentEnvironmentRepository;
import ai.nova.platform.deployment.service.DeploymentObservationService;
import ai.nova.platform.deployment.support.DeploymentTestFixture;

@SpringBootTest(properties = "nova.deployment.enabled=true")
class DeploymentEnvironmentTest {

    @Autowired
    private DeploymentEnvironmentRepository environmentRepository;

    @Autowired
    private DeploymentObservationService observationService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).createOrUpdateAgentDefinition(
                any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void seedsStandardEnvironments() {
        assertThat(environmentRepository.findByActiveTrueOrderBySortOrderAsc())
                .extracting(e -> e.getEnvironmentType())
                .contains(
                        EnvironmentType.DEVELOPMENT,
                        EnvironmentType.TESTING,
                        EnvironmentType.QA,
                        EnvironmentType.STAGING,
                        EnvironmentType.PRODUCTION,
                        EnvironmentType.CUSTOM);
    }

    @Test
    void listEnvironmentsViaService() {
        var envs = observationService.listEnvironments(DeploymentTestFixture.deploymentAdminUser());
        assertThat(envs).isNotEmpty();
        assertThat(envs.stream().map(e -> e.code())).contains("STAGING", "PRODUCTION");
    }
}
