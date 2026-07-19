package ai.nova.platform.ci;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ai.nova.platform.ci.entity.CiOverallStatus;
import ai.nova.platform.ci.service.WorkflowParser;

class WorkflowParserTest {

    @Test
    void mapsGitHubConclusions() {
        assertThat(WorkflowParser.mapConclusion("completed", "success")).isEqualTo(CiOverallStatus.SUCCESS);
        assertThat(WorkflowParser.mapConclusion("completed", "failure")).isEqualTo(CiOverallStatus.FAILED);
        assertThat(WorkflowParser.mapConclusion("completed", "cancelled")).isEqualTo(CiOverallStatus.CANCELLED);
        assertThat(WorkflowParser.mapConclusion("completed", "timed_out")).isEqualTo(CiOverallStatus.TIMED_OUT);
        assertThat(WorkflowParser.mapConclusion("in_progress", null)).isEqualTo(CiOverallStatus.IN_PROGRESS);
        assertThat(WorkflowParser.mapConclusion("queued", null)).isEqualTo(CiOverallStatus.IN_PROGRESS);
        assertThat(WorkflowParser.mapConclusion("completed", "action_required")).isEqualTo(CiOverallStatus.UNKNOWN);
    }

    @Test
    void identifiesFailedStatuses() {
        assertThat(WorkflowParser.isFailed(CiOverallStatus.FAILED)).isTrue();
        assertThat(WorkflowParser.isFailed(CiOverallStatus.CANCELLED)).isTrue();
        assertThat(WorkflowParser.isFailed(CiOverallStatus.TIMED_OUT)).isTrue();
        assertThat(WorkflowParser.isFailed(CiOverallStatus.SUCCESS)).isFalse();
        assertThat(WorkflowParser.isFailed(CiOverallStatus.IN_PROGRESS)).isFalse();
    }
}
