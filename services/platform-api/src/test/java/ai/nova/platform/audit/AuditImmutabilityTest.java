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
import ai.nova.platform.audit.dto.AuditDtos.RecordAuditEventRequest;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.repository.AuditEventRepository;
import ai.nova.platform.audit.service.AuditService;
import ai.nova.platform.audit.support.AuditTestFixture;

@SpringBootTest(properties = {"nova.audit.enabled=true", "nova.audit.immutable=true"})
class AuditImmutabilityTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditEventRepository eventRepository;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void duplicateFingerprintDoesNotCreateSecondRow() {
        UUID entityId = UUID.randomUUID();
        RecordAuditEventRequest request = AuditTestFixture.sampleEvent(entityId, AuditAction.CREATE);
        var first = auditService.record(request);
        var second = auditService.record(request);
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(eventRepository.findAll().stream()
                        .filter(e -> e.getOrganizationId().equals(AuditTestFixture.ORG_ID)
                                && entityId.equals(e.getEntityId()))
                        .count())
                .isEqualTo(1);
    }
}
