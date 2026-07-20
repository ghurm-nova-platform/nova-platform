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
import ai.nova.platform.audit.dto.AuditDtos.AuditCorrelationView;
import ai.nova.platform.audit.dto.AuditDtos.AuditHistoryResponse;
import ai.nova.platform.audit.dto.AuditDtos.RecordAuditEventRequest;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.service.AuditCorrelationService;
import ai.nova.platform.audit.service.AuditService;
import ai.nova.platform.audit.support.AuditTestFixture;

@SpringBootTest(properties = "nova.audit.enabled=true")
class AuditCorrelationTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditCorrelationService correlationService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void entityHistoryAndCorrelationChain() {
        UUID entityId = UUID.randomUUID();
        String correlationId = "corr-" + UUID.randomUUID();
        RecordAuditEventRequest first = new RecordAuditEventRequest(
                AuditTestFixture.ORG_ID,
                AuditTestFixture.PROJECT_ID,
                AuditTestFixture.USER_ID,
                "Nova Admin",
                null,
                AuditEntityType.ENVIRONMENT,
                entityId,
                "history-env",
                AuditAction.CREATE,
                ai.nova.platform.audit.entity.AuditResult.SUCCESS,
                ai.nova.platform.audit.entity.AuditSeverity.MEDIUM,
                ai.nova.platform.audit.entity.AuditSource.ENVIRONMENT_MANAGEMENT,
                correlationId,
                "req-1",
                null,
                null,
                java.util.Map.of());
        RecordAuditEventRequest second = new RecordAuditEventRequest(
                AuditTestFixture.ORG_ID,
                AuditTestFixture.PROJECT_ID,
                AuditTestFixture.USER_ID,
                "Nova Admin",
                null,
                AuditEntityType.ENVIRONMENT,
                entityId,
                "history-env",
                AuditAction.UPDATE,
                ai.nova.platform.audit.entity.AuditResult.SUCCESS,
                ai.nova.platform.audit.entity.AuditSeverity.MEDIUM,
                ai.nova.platform.audit.entity.AuditSource.ENVIRONMENT_MANAGEMENT,
                correlationId,
                "req-2",
                null,
                null,
                java.util.Map.of());

        auditService.record(first);
        auditService.record(second);

        AuditHistoryResponse history =
                correlationService.entityHistory(AuditEntityType.ENVIRONMENT, entityId, AuditTestFixture.auditAdminUser());
        assertThat(history.events()).hasSizeGreaterThanOrEqualTo(2);

        AuditCorrelationView chain = correlationService.byCorrelationId(correlationId, AuditTestFixture.auditAdminUser());
        assertThat(chain.events()).hasSizeGreaterThanOrEqualTo(2);
    }
}
