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
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.collaboration.dto.CollaborationDtos.AssignTaskRequest;
import ai.nova.platform.collaboration.dto.CollaborationDtos.SessionDetail;
import ai.nova.platform.collaboration.dto.CollaborationDtos.TaskAction;
import ai.nova.platform.collaboration.entity.CollaborationParticipantStatus;
import ai.nova.platform.collaboration.entity.CollaborationTaskStatus;
import ai.nova.platform.collaboration.service.CollaborationService;
import ai.nova.platform.collaboration.support.CollaborationTestFixture;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest(properties = {"nova.collaboration.enabled=true", "nova.audit.enabled=true"})
class CollaborationParticipantTaskTest {

    @Autowired
    private CollaborationService collaborationService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void assigningSecondActiveTaskToSameParticipantFails() {
        SessionDetail session = createSession();
        UUID codingParticipant = participantId(session, "CODING");
        UUID taskOne = session.tasks().get(0).id();
        UUID taskTwo = session.tasks().get(1).id();

        assign(session.id(), taskOne, codingParticipant);

        assertThatThrownBy(() -> assign(session.id(), taskTwo, codingParticipant))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(api.getCode()).isEqualTo("COLLABORATION_PARTICIPANT_BUSY");
                });
    }

    @Test
    void completingOneTaskDoesNotClearAnotherParticipantsCurrentTask() {
        SessionDetail session = createSession();
        UUID codingParticipant = participantId(session, "CODING");
        UUID reviewParticipant = participantId(session, "REVIEW");
        UUID taskOne = session.tasks().get(0).id();
        UUID taskTwo = session.tasks().get(1).id();

        assign(session.id(), taskOne, codingParticipant);
        assign(session.id(), taskTwo, reviewParticipant);

        complete(session.id(), taskOne, codingParticipant);

        SessionDetail updated = collaborationService.get(session.id(), CollaborationTestFixture.collaborationReadUser());
        assertThat(updated.participants().stream()
                        .filter(p -> p.id().equals(reviewParticipant))
                        .findFirst()
                        .orElseThrow()
                        .currentTaskId())
                .isEqualTo(taskTwo);
        assertThat(updated.participants().stream()
                        .filter(p -> p.id().equals(reviewParticipant))
                        .findFirst()
                        .orElseThrow()
                        .status())
                .isEqualTo(CollaborationParticipantStatus.ACTIVE);
    }

    @Test
    void reassignmentUpdatesPreviousAndNewParticipants() {
        SessionDetail session = createSession();
        UUID codingParticipant = participantId(session, "CODING");
        UUID reviewParticipant = participantId(session, "REVIEW");
        UUID taskId = session.tasks().getFirst().id();

        assign(session.id(), taskId, codingParticipant);
        reassign(session.id(), taskId, reviewParticipant);

        SessionDetail updated = collaborationService.get(session.id(), CollaborationTestFixture.collaborationReadUser());
        assertThat(updated.tasks().getFirst().participantId()).isEqualTo(reviewParticipant);
        assertThat(updated.participants().stream()
                        .filter(p -> p.id().equals(codingParticipant))
                        .findFirst()
                        .orElseThrow()
                        .status())
                .isEqualTo(CollaborationParticipantStatus.IDLE);
        assertThat(updated.participants().stream()
                        .filter(p -> p.id().equals(reviewParticipant))
                        .findFirst()
                        .orElseThrow()
                        .currentTaskId())
                .isEqualTo(taskId);
    }

    @Test
    void participantStatusRemainsConsistentWithTaskStatus() {
        SessionDetail session = createSession();
        UUID codingParticipant = participantId(session, "CODING");
        UUID taskId = session.tasks().getFirst().id();

        assign(session.id(), taskId, codingParticipant);
        block(session.id(), taskId);

        SessionDetail blocked = collaborationService.get(session.id(), CollaborationTestFixture.collaborationReadUser());
        assertThat(blocked.tasks().getFirst().status()).isEqualTo(CollaborationTaskStatus.BLOCKED);
        assertThat(blocked.participants().getFirst().status()).isEqualTo(CollaborationParticipantStatus.BLOCKED);

        resume(session.id(), taskId);
        SessionDetail resumed = collaborationService.get(session.id(), CollaborationTestFixture.collaborationReadUser());
        assertThat(resumed.tasks().getFirst().status()).isEqualTo(CollaborationTaskStatus.IN_PROGRESS);
        assertThat(resumed.participants().getFirst().status()).isEqualTo(CollaborationParticipantStatus.ACTIVE);
    }

    private SessionDetail createSession() {
        return collaborationService.create(
                CollaborationTestFixture.sampleCreateSessionRequest(CollaborationTestFixture.uniqueName("participant")),
                CollaborationTestFixture.collaborationWriteUser());
    }

    private UUID participantId(SessionDetail session, String role) {
        return session.participants().stream()
                .filter(p -> p.participantRole().name().equals(role))
                .findFirst()
                .orElseThrow()
                .id();
    }

    private void assign(UUID sessionId, UUID taskId, UUID participantId) {
        collaborationService.assign(
                sessionId,
                new AssignTaskRequest(taskId, TaskAction.ASSIGN, participantId, null, null, null, null, null),
                CollaborationTestFixture.collaborationWriteUser());
    }

    private void complete(UUID sessionId, UUID taskId, UUID participantId) {
        collaborationService.assign(
                sessionId,
                new AssignTaskRequest(taskId, TaskAction.COMPLETE, participantId, null, null, null, null, null),
                CollaborationTestFixture.collaborationWriteUser());
    }

    private void reassign(UUID sessionId, UUID taskId, UUID participantId) {
        collaborationService.assign(
                sessionId,
                new AssignTaskRequest(taskId, TaskAction.REASSIGN, null, null, participantId, null, null, null),
                CollaborationTestFixture.collaborationWriteUser());
    }

    private void block(UUID sessionId, UUID taskId) {
        collaborationService.assign(
                sessionId,
                new AssignTaskRequest(taskId, TaskAction.BLOCK, null, null, null, null, null, "dependency"),
                CollaborationTestFixture.collaborationWriteUser());
    }

    private void resume(UUID sessionId, UUID taskId) {
        collaborationService.assign(
                sessionId,
                new AssignTaskRequest(taskId, TaskAction.RESUME, null, null, null, null, null, null),
                CollaborationTestFixture.collaborationWriteUser());
    }
}
