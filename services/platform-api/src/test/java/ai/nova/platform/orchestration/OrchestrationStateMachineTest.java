package ai.nova.platform.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import ai.nova.platform.orchestration.entity.RunStatus;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.service.OrchestrationStateMachine;
import ai.nova.platform.web.error.ApiException;

class OrchestrationStateMachineTest {

    private final OrchestrationStateMachine stateMachine = new OrchestrationStateMachine();

    @Test
    void allowsDraftToReady() {
        stateMachine.transitionRun(RunStatus.DRAFT, RunStatus.READY);
    }

    @Test
    void rejectsTerminalToRunning() {
        assertThatThrownBy(() -> stateMachine.transitionRun(RunStatus.SUCCEEDED, RunStatus.RUNNING))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("INVALID_STATE_TRANSITION");
    }

    @Test
    void allowsReadyToClaimed() {
        stateMachine.transitionTask(TaskStatus.READY, TaskStatus.CLAIMED);
    }

    @Test
    void rejectsSucceededToReady() {
        assertThatThrownBy(() -> stateMachine.transitionTask(TaskStatus.SUCCEEDED, TaskStatus.READY))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("INVALID_STATE_TRANSITION"));
    }

    @Test
    void allowsFailedManualRetryToReady() {
        stateMachine.transitionTask(TaskStatus.FAILED, TaskStatus.READY);
    }

    @Test
    void canTransitionHelpers() {
        assertThat(stateMachine.canTransitionRun(RunStatus.RUNNING, RunStatus.WAITING)).isTrue();
        assertThat(stateMachine.canTransitionTask(TaskStatus.CLAIMED, TaskStatus.RUNNING)).isTrue();
        assertThat(stateMachine.canTransitionRun(RunStatus.ARCHIVED, RunStatus.RUNNING)).isFalse();
    }
}
