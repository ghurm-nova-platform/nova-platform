package ai.nova.platform.collaboration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.collaboration.dto.CollaborationDtos.TaskAction;
import ai.nova.platform.collaboration.entity.CollaborationSessionStatus;
import ai.nova.platform.collaboration.entity.CollaborationTaskStatus;
import ai.nova.platform.collaboration.service.CollaborationStateTransitionValidator;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest(properties = {"nova.collaboration.enabled=true"})
class CollaborationStateTransitionTest {

    @Autowired
    private CollaborationStateTransitionValidator validator;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void allowsExpectedSessionTransitions() {
        assertThatCode(() -> validator.requireSessionTransition(CollaborationSessionStatus.CREATED, CollaborationSessionStatus.ACTIVE))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.requireSessionTransition(CollaborationSessionStatus.ACTIVE, CollaborationSessionStatus.WAITING))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.requireSessionTransition(CollaborationSessionStatus.ACTIVE, CollaborationSessionStatus.BLOCKED))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.requireSessionTransition(CollaborationSessionStatus.ACTIVE, CollaborationSessionStatus.COMPLETED))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.requireSessionTransition(CollaborationSessionStatus.ACTIVE, CollaborationSessionStatus.CANCELLED))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.requireSessionTransition(CollaborationSessionStatus.BLOCKED, CollaborationSessionStatus.ACTIVE))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.requireSessionTransition(CollaborationSessionStatus.WAITING, CollaborationSessionStatus.ACTIVE))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(
            value = CollaborationSessionStatus.class,
            names = {"COMPLETED", "FAILED", "CANCELLED"})
    void terminalSessionCannotBeMutated(CollaborationSessionStatus status) {
        assertThatThrownBy(() -> validator.requireMutableSession(status))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(api.getCode()).isEqualTo("COLLABORATION_SESSION_INVALID_STATUS");
                });
    }

    @ParameterizedTest
    @EnumSource(
            value = CollaborationTaskStatus.class,
            names = {"COMPLETED", "REJECTED", "CANCELLED"})
    void terminalTaskCannotBeReassignedBlockedOrCompleted(CollaborationTaskStatus status) {
        assertThatThrownBy(() -> validator.requireTaskAction(status, TaskAction.REASSIGN))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> validator.requireTaskAction(status, TaskAction.BLOCK))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> validator.requireTaskAction(status, TaskAction.COMPLETE))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void pendingTaskCanBeAssignedButNotCompleted() {
        assertThatCode(() -> validator.requireTaskAction(CollaborationTaskStatus.PENDING, TaskAction.ASSIGN))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.requireTaskAction(CollaborationTaskStatus.PENDING, TaskAction.COMPLETE))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void blockedTaskCanBeResumedOnly() {
        assertThatCode(() -> validator.requireTaskAction(CollaborationTaskStatus.BLOCKED, TaskAction.RESUME))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.requireTaskAction(CollaborationTaskStatus.BLOCKED, TaskAction.ASSIGN))
                .isInstanceOf(ApiException.class);
    }
}
