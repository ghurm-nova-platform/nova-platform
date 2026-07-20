package ai.nova.platform.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.audit.dto.AuditDtos.RecordAuditEventRequest;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSeverity;
import ai.nova.platform.audit.entity.AuditSource;
import ai.nova.platform.audit.repository.AuditEventRepository;
import ai.nova.platform.audit.service.AuditPublisher;
import ai.nova.platform.audit.support.AuditTestFixture;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.CreateRunRequest;
import ai.nova.platform.orchestration.entity.ExecutionMode;
import ai.nova.platform.orchestration.entity.FailurePolicy;
import ai.nova.platform.orchestration.service.OrchestrationRunService;
import ai.nova.platform.release.dto.ReleaseDtos.CreateReleaseRequest;
import ai.nova.platform.release.entity.VersionBump;
import ai.nova.platform.release.service.ReleaseManagerService;

@SpringBootTest(properties = {"nova.audit.enabled=true", "nova.release.enabled=true"})
class AuditSubsystemCoverageTest {

    @Autowired
    private AuditPublisher auditPublisher;

    @Autowired
    private AuditEventRepository eventRepository;

    @Autowired
    private ReleaseManagerService releaseManagerService;

    @Autowired
    private OrchestrationRunService orchestrationRunService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void allNewAuditActionsAndSourcesPersistViaPublisher() {
        AuditAction[] newActions = {
            AuditAction.START,
            AuditAction.COMPLETE,
            AuditAction.FAIL,
            AuditAction.PREPARE,
            AuditAction.READY,
            AuditAction.PUBLISH
        };
        AuditSource[] newSources = {
            AuditSource.ORCHESTRATION,
            AuditSource.PLANNER,
            AuditSource.CODING,
            AuditSource.REVIEW,
            AuditSource.TESTING,
            AuditSource.PATCH,
            AuditSource.GIT_INTEGRATION,
            AuditSource.PULL_REQUEST,
            AuditSource.CI_OBSERVATION,
            AuditSource.REPAIR,
            AuditSource.APPROVAL_GATE
        };

        long before = eventRepository.count();

        for (AuditAction action : newActions) {
            auditPublisher.record(sampleRequest(action, AuditSource.ORCHESTRATION));
        }
        for (AuditSource source : newSources) {
            auditPublisher.record(sampleRequest(AuditAction.CREATE, source));
        }

        long inserted = eventRepository.count() - before;
        assertThat(inserted).isEqualTo(newActions.length + newSources.length);
    }

    @Test
    void releaseCreatePublishesAuditEvent() {
        UUID mergeId = UUID.randomUUID();
        long before = countBySource(AuditSource.RELEASE_MANAGER);

        releaseManagerService.create(
                new CreateReleaseRequest(
                        AuditTestFixture.PROJECT_ID,
                        "audit-release-" + mergeId,
                        "coverage test",
                        VersionBump.PATCH,
                        "9.9." + Math.abs(mergeId.hashCode() % 1000),
                        java.util.List.of(mergeId),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of("sha-" + mergeId),
                        java.util.List.of()),
                AuditTestFixture.auditAdminUser());

        assertThat(countBySource(AuditSource.RELEASE_MANAGER)).isGreaterThan(before);
    }

    @Test
    void orchestrationCreatePublishesAuditEvent() {
        long before = countBySource(AuditSource.ORCHESTRATION);

        orchestrationRunService.create(
                new CreateRunRequest(
                        AuditTestFixture.PROJECT_ID,
                        "audit-run-" + UUID.randomUUID(),
                        "audit coverage objective",
                        ExecutionMode.SEQUENTIAL,
                        FailurePolicy.FAIL_FAST,
                        1,
                        60_000L,
                        null,
                        null,
                        null),
                AuditTestFixture.auditAdminUser());

        assertThat(countBySource(AuditSource.ORCHESTRATION)).isGreaterThan(before);
    }

    private long countBySource(AuditSource source) {
        return eventRepository.findAll().stream().filter(event -> event.getSource() == source).count();
    }

    private RecordAuditEventRequest sampleRequest(AuditAction action, AuditSource source) {
        UUID entityId = UUID.randomUUID();
        return new RecordAuditEventRequest(
                AuditTestFixture.ORG_ID,
                AuditTestFixture.PROJECT_ID,
                AuditTestFixture.USER_ID,
                "Nova Admin",
                null,
                AuditEntityType.TASK,
                entityId,
                "coverage-" + action.name().toLowerCase(),
                action,
                AuditResult.SUCCESS,
                AuditSeverity.MEDIUM,
                source,
                "corr-" + UUID.randomUUID(),
                "req-" + UUID.randomUUID() + "-" + action + "-" + source,
                null,
                null,
                Map.of("coverage", true, "action", action.name(), "source", source.name()));
    }
}
