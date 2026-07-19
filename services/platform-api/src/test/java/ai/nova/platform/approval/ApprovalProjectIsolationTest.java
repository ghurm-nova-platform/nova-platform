package ai.nova.platform.approval;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.approval.policy.ApprovalPolicyService;
import ai.nova.platform.approval.support.ApprovalTestFixture;
import ai.nova.platform.project.ProjectRepository;

@SpringBootTest
class ApprovalProjectIsolationTest {

    @Autowired
    private ApprovalPolicyService policyService;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void activeDefaultPolicyResolvesForSeedProject() {
        var project = projectRepository
                .findByIdAndOrganizationId(
                        java.util.UUID.fromString(ApprovalTestFixture.PROJECT_ID), ApprovalTestFixture.ORG_ID)
                .orElseThrow();
        var policy = policyService.requireActiveDefaultPolicy(project.getOrganizationId(), project.getId());
        assertThat(policy.getId()).isEqualTo(ApprovalTestFixture.DEFAULT_POLICY_ID);
        assertThat(policy.isDefault()).isTrue();
    }
}
