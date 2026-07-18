package ai.nova.platform.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import ai.nova.platform.git.service.GitBranchStrategy;
import ai.nova.platform.web.error.ApiException;

class GitBranchStrategyTest {

    private final GitBranchStrategy strategy = new GitBranchStrategy();

    @Test
    void buildsDeterministicBranchAndCommitMessage() {
        UUID taskId = UUID.fromString("11111111-1111-1111-1111-111111111024");
        assertThat(strategy.branchNameForTask(taskId)).isEqualTo("ai/task-11111111-1111-1111-1111-111111111024");
        assertThat(strategy.commitMessageForTask(taskId))
                .isEqualTo("AI: Apply approved patch for Task #11111111-1111-1111-1111-111111111024");
    }

    @Test
    void rejectsProtectedBranchNames() {
        assertThatThrownBy(() -> strategy.assertSafeBranchName("main"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("GIT_INVALID_BRANCH");
        assertThatThrownBy(() -> strategy.assertSafeBranchName("ai/task-main"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("GIT_INVALID_BRANCH");
        assertThatThrownBy(() -> strategy.assertSafeBranchName("develop"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("GIT_INVALID_BRANCH");
    }

    @Test
    void acceptsAiTaskBranch() {
        strategy.assertSafeBranchName("ai/task-" + UUID.randomUUID());
    }
}
