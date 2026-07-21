package ai.nova.platform.collaboration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.collaboration.dto.CollaborationDtos.AssignTaskRequest;
import ai.nova.platform.collaboration.dto.CollaborationDtos.RecordDecisionRequest;
import ai.nova.platform.collaboration.dto.CollaborationDtos.SessionDetail;
import ai.nova.platform.collaboration.dto.CollaborationDtos.TaskAction;
import ai.nova.platform.collaboration.entity.CollaborationDecisionType;
import ai.nova.platform.collaboration.service.CollaborationService;
import ai.nova.platform.collaboration.support.CollaborationTestFixture;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest(properties = {"nova.collaboration.enabled=true", "nova.audit.enabled=true"})
class CollaborationTenantIsolationTest {

    @Autowired
    private CollaborationService collaborationService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void tenantCannotReferenceParticipantFromAnotherSession() {
        SessionDetail sessionA = createSession("session-a");
        SessionDetail sessionB = createSession("session-b");
        UUID foreignParticipant = sessionB.participants().getFirst().id();
        UUID taskId = sessionA.tasks().getFirst().id();

        assertThatThrownBy(() -> collaborationService.assign(
                        sessionA.id(),
                        new AssignTaskRequest(
                                taskId, TaskAction.ASSIGN, foreignParticipant, null, null, null, null, null),
                        CollaborationTestFixture.collaborationWriteUser()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("COLLABORATION_PARTICIPANT_MISMATCH"));
    }

    @Test
    void tenantCannotReferenceTaskFromAnotherSession() {
        SessionDetail sessionA = createSession("session-a");
        SessionDetail sessionB = createSession("session-b");
        UUID foreignTask = sessionB.tasks().getFirst().id();

        assertThatThrownBy(() -> collaborationService.sendMessage(
                        sessionA.id(),
                        CollaborationTestFixture.infoMessageForTask("cross session", foreignTask),
                        CollaborationTestFixture.collaborationWriteUser()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("COLLABORATION_TASK_MISMATCH"));
    }

    @Test
    void decisionCannotReferenceTaskFromAnotherSession() {
        SessionDetail sessionA = createSession("session-a");
        SessionDetail sessionB = createSession("session-b");
        UUID foreignTask = sessionB.tasks().getFirst().id();

        assertThatThrownBy(() -> collaborationService.recordDecision(
                        sessionA.id(),
                        new RecordDecisionRequest(CollaborationDecisionType.APPROVE, "approve", null, foreignTask),
                        CollaborationTestFixture.collaborationWriteUser()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("COLLABORATION_TASK_MISMATCH"));
    }

    @Test
    void tenantCannotAccessSessionFromAnotherOrganization() {
        SessionDetail session = createSession("org-a-session");

        assertThatThrownBy(() -> collaborationService.get(session.id(), CollaborationTestFixture.collaborationOtherOrgWriteUser()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("COLLABORATION_SESSION_NOT_FOUND"));
    }

    private SessionDetail createSession(String prefix) {
        return collaborationService.create(
                CollaborationTestFixture.sampleCreateSessionRequest(CollaborationTestFixture.uniqueName(prefix)),
                CollaborationTestFixture.collaborationWriteUser());
    }
}
