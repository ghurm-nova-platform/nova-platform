package ai.nova.platform.collaboration;

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
import ai.nova.platform.collaboration.dto.CollaborationDtos.AssignTaskRequest;
import ai.nova.platform.collaboration.dto.CollaborationDtos.SessionDetail;
import ai.nova.platform.collaboration.dto.CollaborationDtos.TaskAction;
import ai.nova.platform.collaboration.entity.CollaborationSessionStatus;
import ai.nova.platform.collaboration.entity.CollaborationTaskStatus;
import ai.nova.platform.collaboration.service.CollaborationService;
import ai.nova.platform.collaboration.support.CollaborationTestFixture;

@SpringBootTest(properties = {"nova.collaboration.enabled=true", "nova.audit.enabled=true"})
class CollaborationCoordinatorTest {

    @Autowired
    private CollaborationService collaborationService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void assignAndCompleteTask() {
        SessionDetail session = collaborationService.create(
                CollaborationTestFixture.sampleCreateSessionRequest(CollaborationTestFixture.uniqueName("coord")),
                CollaborationTestFixture.collaborationWriteUser());

        UUID taskId = session.tasks().getFirst().id();
        UUID participantId = session.participants().getFirst().id();

        collaborationService.assign(
                session.id(),
                new AssignTaskRequest(taskId, TaskAction.ASSIGN, participantId, null, null, null, null, null),
                CollaborationTestFixture.collaborationWriteUser());

        SessionDetail assigned = collaborationService.get(session.id(), CollaborationTestFixture.collaborationReadUser());
        assertThat(assigned.status()).isEqualTo(CollaborationSessionStatus.ACTIVE);
        assertThat(assigned.tasks().getFirst().status()).isEqualTo(CollaborationTaskStatus.ASSIGNED);
        assertThat(assigned.tasks().getFirst().participantId()).isEqualTo(participantId);

        collaborationService.assign(
                session.id(),
                new AssignTaskRequest(taskId, TaskAction.COMPLETE, participantId, null, null, null, null, null),
                CollaborationTestFixture.collaborationWriteUser());

        SessionDetail completed = collaborationService.get(session.id(), CollaborationTestFixture.collaborationReadUser());
        assertThat(completed.tasks().getFirst().status()).isEqualTo(CollaborationTaskStatus.COMPLETED);
        assertThat(completed.participants().getFirst().progressPercent()).isEqualTo(100);
    }

    @Test
    void parallelGroupTasksCanBeAssignedTogether() {
        String group = "group-" + UUID.randomUUID();
        SessionDetail session = collaborationService.create(
                CollaborationTestFixture.createSessionWithParallelTasks(
                        CollaborationTestFixture.uniqueName("parallel"), group),
                CollaborationTestFixture.collaborationWriteUser());

        UUID codingParticipant = session.participants().stream()
                .filter(p -> p.participantRole().name().equals("CODING"))
                .findFirst()
                .orElseThrow()
                .id();
        UUID reviewParticipant = session.participants().stream()
                .filter(p -> p.participantRole().name().equals("REVIEW"))
                .findFirst()
                .orElseThrow()
                .id();

        UUID taskAId = session.tasks().stream()
                .filter(t -> t.taskKey().equals("parallel-a"))
                .findFirst()
                .orElseThrow()
                .id();
        UUID taskBId = session.tasks().stream()
                .filter(t -> t.taskKey().equals("parallel-b"))
                .findFirst()
                .orElseThrow()
                .id();

        collaborationService.assign(
                session.id(),
                new AssignTaskRequest(taskAId, TaskAction.ASSIGN, codingParticipant, null, null, null, group, null),
                CollaborationTestFixture.collaborationWriteUser());
        collaborationService.assign(
                session.id(),
                new AssignTaskRequest(taskBId, TaskAction.ASSIGN, reviewParticipant, null, null, null, group, null),
                CollaborationTestFixture.collaborationWriteUser());

        SessionDetail updated = collaborationService.get(session.id(), CollaborationTestFixture.collaborationReadUser());
        assertThat(updated.tasks()).allMatch(t -> t.status() == CollaborationTaskStatus.ASSIGNED);
        assertThat(updated.tasks()).allMatch(t -> group.equals(t.parallelGroup()));
        assertThat(updated.status()).isEqualTo(CollaborationSessionStatus.ACTIVE);
        assertThat(updated.conflictDetected()).isFalse();
    }
}
