package ai.nova.platform.collaboration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.collaboration.dto.CollaborationDtos.AssignTaskRequest;
import ai.nova.platform.collaboration.dto.CollaborationDtos.SendMessageRequest;
import ai.nova.platform.collaboration.dto.CollaborationDtos.SessionDetail;
import ai.nova.platform.collaboration.dto.CollaborationDtos.TaskAction;
import ai.nova.platform.collaboration.dto.CollaborationDtos.TimelineEventView;
import ai.nova.platform.collaboration.entity.CollaborationMessageType;
import ai.nova.platform.collaboration.entity.CollaborationParticipantRole;
import ai.nova.platform.collaboration.entity.CollaborationTimelineEventType;
import ai.nova.platform.collaboration.service.CollaborationService;
import ai.nova.platform.collaboration.support.CollaborationTestFixture;

@SpringBootTest(properties = {"nova.collaboration.enabled=true", "nova.audit.enabled=true"})
class CollaborationTimelineTest {

    @Autowired
    private CollaborationService collaborationService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void timelineIsAppendOnlyAfterCreateAssignAndMessage() {
        SessionDetail session = collaborationService.create(
                CollaborationTestFixture.sampleCreateSessionRequest(CollaborationTestFixture.uniqueName("timeline")),
                CollaborationTestFixture.collaborationWriteUser());

        List<TimelineEventView> afterCreate = collaborationService.timeline(
                session.id(), CollaborationTestFixture.collaborationReadUser());
        assertThat(afterCreate).isNotEmpty();
        assertThat(afterCreate.getFirst().eventType()).isEqualTo(CollaborationTimelineEventType.CREATED);
        List<UUID> eventIdsAfterCreate = afterCreate.stream().map(TimelineEventView::id).toList();

        UUID taskId = session.tasks().getFirst().id();
        UUID participantId = session.participants().getFirst().id();

        collaborationService.assign(
                session.id(),
                new AssignTaskRequest(taskId, TaskAction.ASSIGN, participantId, null, null, null, null, null),
                CollaborationTestFixture.collaborationWriteUser());

        List<TimelineEventView> afterAssign = collaborationService.timeline(
                session.id(), CollaborationTestFixture.collaborationReadUser());
        assertThat(afterAssign.size()).isGreaterThan(afterCreate.size());
        assertThat(afterAssign.stream().map(TimelineEventView::id)).containsAll(eventIdsAfterCreate);
        assertThat(afterAssign.stream().map(TimelineEventView::eventType))
                .contains(
                        CollaborationTimelineEventType.CREATED,
                        CollaborationTimelineEventType.STARTED,
                        CollaborationTimelineEventType.TASK_ASSIGNED);

        collaborationService.sendMessage(
                session.id(),
                new SendMessageRequest(
                        CollaborationParticipantRole.CODING,
                        CollaborationMessageType.INFO,
                        "Implementation complete",
                        taskId),
                CollaborationTestFixture.collaborationWriteUser());

        List<TimelineEventView> afterMessage = collaborationService.timeline(
                session.id(), CollaborationTestFixture.collaborationReadUser());
        assertThat(afterMessage.size()).isGreaterThan(afterAssign.size());
        assertThat(afterMessage.stream().map(TimelineEventView::id)).containsAll(
                afterAssign.stream().map(TimelineEventView::id).toList());
        assertThat(afterMessage.stream().map(TimelineEventView::eventType))
                .contains(CollaborationTimelineEventType.MESSAGE_SENT);
    }
}
