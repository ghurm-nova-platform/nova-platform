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
import ai.nova.platform.collaboration.entity.CollaborationTimelineEventType;
import ai.nova.platform.collaboration.service.CollaborationService;
import ai.nova.platform.collaboration.support.CollaborationTestFixture;

@SpringBootTest(properties = {"nova.collaboration.enabled=true", "nova.audit.enabled=true"})
class CollaborationConflictTest {

    private static final String SHARED_ARTIFACT = "src/main/java/App.java";

    @Autowired
    private CollaborationService collaborationService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void twoTasksWithSameArtifactRefTriggerConflictDetected() {
        SessionDetail session = collaborationService.create(
                CollaborationTestFixture.createSessionWithConflictTasks(CollaborationTestFixture.uniqueName("conflict")),
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

        UUID taskA = session.tasks().stream()
                .filter(t -> t.taskKey().equals("conflict-a"))
                .findFirst()
                .orElseThrow()
                .id();
        UUID taskB = session.tasks().stream()
                .filter(t -> t.taskKey().equals("conflict-b"))
                .findFirst()
                .orElseThrow()
                .id();

        collaborationService.assign(
                session.id(),
                new AssignTaskRequest(
                        taskA, TaskAction.ASSIGN, codingParticipant, null, null, SHARED_ARTIFACT, null, null),
                CollaborationTestFixture.collaborationWriteUser());

        SessionDetail afterFirstAssign =
                collaborationService.get(session.id(), CollaborationTestFixture.collaborationReadUser());
        assertThat(afterFirstAssign.conflictDetected()).isFalse();

        collaborationService.assign(
                session.id(),
                new AssignTaskRequest(
                        taskB, TaskAction.ASSIGN, reviewParticipant, null, null, SHARED_ARTIFACT, null, null),
                CollaborationTestFixture.collaborationWriteUser());

        SessionDetail conflicted =
                collaborationService.get(session.id(), CollaborationTestFixture.collaborationReadUser());
        assertThat(conflicted.conflictDetected()).isTrue();
        assertThat(conflicted.status()).isEqualTo(CollaborationSessionStatus.BLOCKED);
        assertThat(conflicted.conflictDetails()).isNotEmpty();
        assertThat(conflicted.timeline().stream().map(e -> e.eventType()))
                .contains(CollaborationTimelineEventType.CONFLICT);
    }
}
