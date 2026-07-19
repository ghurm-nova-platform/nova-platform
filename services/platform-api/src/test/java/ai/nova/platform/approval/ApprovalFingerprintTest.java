package ai.nova.platform.approval;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.approval.service.ApprovalEvidenceBundle;
import ai.nova.platform.approval.service.ApprovalFingerprint;
import ai.nova.platform.approval.support.ApprovalTestFixture;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.entity.TaskType;
import ai.nova.platform.project.Project;
import ai.nova.platform.project.ProjectStatus;
import ai.nova.platform.project.ProjectVisibility;

@SpringBootTest
class ApprovalFingerprintTest {

    @Autowired
    private ApprovalFingerprint fingerprint;

    @Test
    void evidenceFingerprintIsDeterministic() {
        ApprovalEvidenceBundle bundle = syntheticBundle();
        String first = fingerprint.evidenceFingerprint(bundle);
        String second = fingerprint.evidenceFingerprint(bundle);
        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
    }

    @Test
    void decisionFingerprintChangesWithHumanCounts() {
        String evidenceFp = "abc123";
        String first = fingerprint.decisionFingerprint(evidenceFp, 0, 0, 1);
        String second = fingerprint.decisionFingerprint(evidenceFp, 1, 0, 1);
        assertThat(first).isNotEqualTo(second);
    }

    private static ApprovalEvidenceBundle syntheticBundle() {
        UUID org = ApprovalTestFixture.ORG_ID;
        UUID projectId = UUID.fromString(ApprovalTestFixture.PROJECT_ID);
        AgentOrchestrationTask task = new AgentOrchestrationTask(
                UUID.randomUUID(),
                org,
                projectId,
                UUID.randomUUID(),
                "task-key",
                "Task",
                TaskType.AGENT_TURN,
                TaskStatus.READY,
                "idem",
                1,
                0L,
                1,
                60,
                ApprovalTestFixture.USER_ID,
                java.time.Instant.now());
        Project project = new Project(
                projectId,
                org,
                "Demo",
                "demo",
                ProjectStatus.ACTIVE,
                ProjectVisibility.PRIVATE,
                java.time.Instant.now(),
                java.time.Instant.now(),
                ApprovalTestFixture.USER_ID,
                ApprovalTestFixture.USER_ID);
        return new ApprovalEvidenceBundle(
                task, project, null, null, null, null, null, null, null, "hash",
                null, null, null, null, null, null, null, false, false);
    }
}
