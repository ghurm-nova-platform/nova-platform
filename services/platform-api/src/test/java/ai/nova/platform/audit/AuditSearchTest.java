package ai.nova.platform.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.audit.dto.AuditDtos.AuditSearchRequest;
import ai.nova.platform.audit.dto.AuditDtos.AuditSearchResponse;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.service.AuditService;
import ai.nova.platform.audit.support.AuditTestFixture;

@SpringBootTest(properties = "nova.audit.enabled=true")
class AuditSearchTest {

    @Autowired
    private AuditService auditService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void searchByEntityTypeAndProject() {
        UUID entityId = UUID.randomUUID();
        auditService.record(AuditTestFixture.sampleEvent(entityId, AuditAction.CREATE));

        AuditSearchResponse response = auditService.search(
                new AuditSearchRequest(
                        null,
                        null,
                        AuditTestFixture.PROJECT_ID,
                        null,
                        AuditEntityType.ENVIRONMENT,
                        entityId,
                        AuditAction.CREATE,
                        null,
                        null,
                        null,
                        null,
                        0,
                        20),
                AuditTestFixture.auditAdminUser());

        assertThat(response.events()).isNotEmpty();
        assertThat(response.events().get(0).entityId()).isEqualTo(entityId);
    }
}
