package ai.nova.platform.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.approval.entity.ApprovalPolicyStatus;
import ai.nova.platform.approval.repository.ApprovalPolicyRepository;
import ai.nova.platform.approval.support.ApprovalTestFixture;
import ai.nova.platform.pullrequest.support.PullRequestTestFixture;

@SpringBootTest
@AutoConfigureMockMvc
class ApprovalPolicyVersioningTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApprovalPolicyRepository policyRepository;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).createOrUpdateAgentDefinition(
                any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void defaultSeedPolicyIsActive() throws Exception {
        String token = PullRequestTestFixture.loginAdmin(mockMvc, objectMapper);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/approval-policies")
                        .header("Authorization", "Bearer " + token))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        assertThat(policyRepository.findById(ApprovalTestFixture.DEFAULT_POLICY_ID)).isPresent();
        assertThat(policyRepository.findById(ApprovalTestFixture.DEFAULT_POLICY_ID).orElseThrow().getStatus())
                .isEqualTo(ApprovalPolicyStatus.ACTIVE);
    }
}
