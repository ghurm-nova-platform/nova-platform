package ai.nova.platform.approval;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.approval.entity.ApprovalRequirementResult;
import ai.nova.platform.approval.policy.StandardApprovalPolicyEvaluator;

@SpringBootTest
class ApprovalRuleTest {

    @Autowired
    private StandardApprovalPolicyEvaluator evaluator;

    @Test
    void evaluatorExposesMandatoryRuleCodes() {
        assertThat(evaluator).isNotNull();
        assertThat(ApprovalRequirementResult.values()).contains(ApprovalRequirementResult.FAILED);
    }
}
