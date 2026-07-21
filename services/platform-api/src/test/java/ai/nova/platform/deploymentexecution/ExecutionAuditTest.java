package ai.nova.platform.deploymentexecution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditSource;
import ai.nova.platform.audit.repository.AuditEventRepository;
import ai.nova.platform.deploymentexecution.dto.ExecutionDtos.CreateExecutionRequest;
import ai.nova.platform.deploymentexecution.entity.ExecutionProviderCode;
import ai.nova.platform.deploymentexecution.service.DeploymentExecutionService;
import ai.nova.platform.deploymentexecution.support.ExecutionSeedSupport;
import ai.nova.platform.deploymentexecution.support.ExecutionTestFixture;
import ai.nova.platform.security.AuthenticatedUser;

@SpringBootTest(
        properties = {
            "nova.execution.enabled=true",
            "nova.audit.enabled=true",
            "nova.rollback.enabled=true",
            "nova.deployment.enabled=true",
            "nova.release.enabled=true"
        })
class ExecutionAuditTest {

    @Autowired
    private DeploymentExecutionService deploymentExecutionService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private ExecutionSeedSupport seedSupport;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    private AuthenticatedUser user;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> null)
                .when(agentRuntimeClient)
                .createOrUpdateAgentDefinition(any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        user = ExecutionTestFixture.executionAdminUser();
    }

    @Test
    void startPublishesDeploymentExecutionAuditEvents() throws Exception {
        var ctx = seedSupport.seedExecutionReadyContext();
        var created = deploymentExecutionService.create(
                new CreateExecutionRequest(
                        ctx.releaseId(), ExecutionTestFixture.STAGING_ENVIRONMENT_ID, null, ExecutionProviderCode.LOCAL, null),
                user);
        deploymentExecutionService.start(created.id(), user);

        ExecutionTestFixture.awaitStatus(
                () -> deploymentExecutionService.get(created.id(), user).status(),
                ai.nova.platform.deploymentexecution.entity.ExecutionStatus.COMPLETED,
                10_000);

        var events = auditEventRepository.findAll().stream()
                .filter(e -> e.getSource() == AuditSource.DEPLOYMENT_EXECUTION)
                .filter(e -> created.id().equals(e.getEntityId()))
                .toList();
        assertThat(events.stream().map(e -> e.getAction()).toList())
                .contains(AuditAction.QUEUE, AuditAction.START, AuditAction.VERIFY, AuditAction.COMPLETE);
    }
}
