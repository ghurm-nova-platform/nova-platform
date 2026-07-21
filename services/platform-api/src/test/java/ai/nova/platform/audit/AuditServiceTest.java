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
import ai.nova.platform.audit.dto.AuditDtos.AuditEvent;
import ai.nova.platform.audit.dto.AuditDtos.RecordAuditEventRequest;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.service.AuditService;
import ai.nova.platform.audit.support.AuditTestFixture;

@SpringBootTest(properties = {"nova.audit.enabled=true", "nova.environment.enabled=true"})
class AuditServiceTest {

    @Autowired
    private AuditService auditService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null)
                .when(agentRuntimeClient)
                .createOrUpdateAgentDefinition(any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void recordAndRetrieveById() {
        UUID entityId = UUID.randomUUID();
        RecordAuditEventRequest request = AuditTestFixture.sampleEvent(entityId, AuditAction.CREATE);
        AuditEvent recorded = auditService.record(request);
        assertThat(recorded).isNotNull();
        assertThat(recorded.id()).isNotNull();
        assertThat(recorded.entityId()).isEqualTo(entityId);

        AuditEvent loaded = auditService.get(recorded.id(), AuditTestFixture.auditAdminUser());
        assertThat(loaded.action()).isEqualTo(AuditAction.CREATE);
        assertThat(loaded.correlationId()).isEqualTo(request.correlationId());
    }

    @Test
    void idempotentDuplicateFingerprintReturnsExisting() {
        UUID entityId = UUID.randomUUID();
        RecordAuditEventRequest request = AuditTestFixture.sampleEvent(entityId, AuditAction.UPDATE);
        AuditEvent first = auditService.record(request);
        AuditEvent second = auditService.record(request);
        assertThat(second.id()).isEqualTo(first.id());
    }
}
