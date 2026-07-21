package ai.nova.platform.collaboration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.collaboration.dto.CollaborationDtos.CreateSessionRequest;
import ai.nova.platform.collaboration.dto.CollaborationDtos.SessionDetail;
import ai.nova.platform.collaboration.dto.CollaborationDtos.SessionSummary;
import ai.nova.platform.collaboration.service.CollaborationService;
import ai.nova.platform.collaboration.support.CollaborationTestFixture;

@SpringBootTest(properties = {"nova.collaboration.enabled=true", "nova.audit.enabled=true"})
class CollaborationServiceTest {

    @Autowired
    private CollaborationService collaborationService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void createListAndGetDetail() {
        String name = CollaborationTestFixture.uniqueName("svc-session");
        CreateSessionRequest request = CollaborationTestFixture.sampleCreateSessionRequest(name);

        SessionDetail created = collaborationService.create(request, CollaborationTestFixture.collaborationWriteUser());

        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo(name);
        assertThat(created.projectId()).isEqualTo(CollaborationTestFixture.PROJECT_ID);
        assertThat(created.organizationId()).isEqualTo(CollaborationTestFixture.ORG_ID);
        assertThat(created.participants()).hasSize(2);
        assertThat(created.tasks()).hasSize(2);

        var summaries = collaborationService.list(
                CollaborationTestFixture.PROJECT_ID, CollaborationTestFixture.collaborationReadUser());
        assertThat(summaries.stream().map(SessionSummary::id)).contains(created.id());

        SessionDetail loaded = collaborationService.get(created.id(), CollaborationTestFixture.collaborationReadUser());
        assertThat(loaded.id()).isEqualTo(created.id());
        assertThat(loaded.name()).isEqualTo(name);
        assertThat(loaded.tasks()).extracting(t -> t.taskKey()).containsExactly("task-1", "task-2");
    }
}
