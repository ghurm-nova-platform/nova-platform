package ai.nova.platform.rollback;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.rollback.entity.RollbackRiskLevel;
import ai.nova.platform.rollback.entity.RollbackStrategy;
import ai.nova.platform.rollback.service.RollbackPlanHashService;
import ai.nova.platform.rollback.support.RollbackTestFixture;

@SpringBootTest(properties = {"nova.rollback.enabled=true", "nova.rollback.execution-enabled=false"})
class RollbackPlanHashTest {

    @Autowired
    private RollbackPlanHashService planHashService;

    @Test
    void identicalInputsProduceSameHash() {
        UUID org = RollbackTestFixture.ORG_ID;
        UUID project = RollbackTestFixture.PROJECT_ID;
        UUID release = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
        UUID deployment = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
        UUID target = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3");

        var first = planHashService.build(
                org,
                project,
                release,
                deployment,
                target,
                "STAGING",
                RollbackStrategy.PREVIOUS_RELEASE,
                "reason",
                RollbackRiskLevel.MEDIUM,
                "2.0.0",
                "1.9.0");
        var second = planHashService.build(
                org,
                project,
                release,
                deployment,
                target,
                "staging",
                RollbackStrategy.PREVIOUS_RELEASE,
                "reason",
                RollbackRiskLevel.MEDIUM,
                "2.0.0",
                "1.9.0");

        assertThat(first.planHash()).isEqualTo(second.planHash());
        assertThat(first.planHash()).hasSize(64);
        assertThat(first.planJson()).contains("\"strategy\":\"PREVIOUS_RELEASE\"");
    }

    @Test
    void differentReasonChangesHash() {
        UUID org = RollbackTestFixture.ORG_ID;
        UUID project = RollbackTestFixture.PROJECT_ID;
        UUID release = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1");
        UUID deployment = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2");
        UUID target = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3");

        var a = planHashService.build(
                org,
                project,
                release,
                deployment,
                target,
                "QA",
                RollbackStrategy.SPECIFIC_RELEASE,
                "a",
                RollbackRiskLevel.LOW,
                "3.0.0",
                "2.0.0");
        var b = planHashService.build(
                org,
                project,
                release,
                deployment,
                target,
                "QA",
                RollbackStrategy.SPECIFIC_RELEASE,
                "b",
                RollbackRiskLevel.LOW,
                "3.0.0",
                "2.0.0");
        assertThat(a.planHash()).isNotEqualTo(b.planHash());
    }
}
