package ai.nova.platform.merge.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.approval.repository.ApprovalPolicyRepository;
import ai.nova.platform.approval.support.ApprovalTestFixture;
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.ci.service.CiStorageService;
import ai.nova.platform.git.config.GitProperties;
import ai.nova.platform.git.service.ControlledGitService;
import ai.nova.platform.git.service.GitIntegrationAgentService;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.pullrequest.repository.ProjectRepositoryConfigRepository;
import ai.nova.platform.pullrequest.service.ProjectRepositoryConfigService;
import ai.nova.platform.pullrequest.service.PullRequestStorageService;
import ai.nova.platform.pullrequest.support.PullRequestTestFixture;
import ai.nova.platform.review.service.ReviewStorageService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.testing.service.TestingStorageService;

public final class MergeTestFixture {

    public static final UUID ORG_ID = ApprovalTestFixture.ORG_ID;
    public static final UUID USER_ID = ApprovalTestFixture.USER_ID;

    private MergeTestFixture() {
    }

    public static AuthenticatedUser mergeAdminUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "admin@nova.local",
                "Nova Admin",
                List.of("ORG_ADMIN"),
                List.of("MERGE_RUN", "MERGE_READ", "APPROVAL_GATE_RUN", "APPROVAL_GATE_READ", "APPROVAL_GATE_APPROVE"),
                true);
    }

    public static AuthenticatedUser mergeReadOnlyUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "viewer@nova.local",
                "Viewer",
                List.of("USER"),
                List.of("MERGE_READ"),
                true);
    }

    public static UUID seedApprovedTask(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            String accessToken,
            AuthenticatedUser user,
            ApprovalPolicyRepository policyRepository,
            GitIntegrationAgentService gitAgentService,
            ArtifactStorageService artifactStorageService,
            PatchStorageService patchStorageService,
            PatchDiffParser patchDiffParser,
            ReviewStorageService reviewStorageService,
            TestingStorageService testingStorageService,
            PullRequestStorageService pullRequestStorageService,
            ProjectRepositoryConfigService repositoryConfigService,
            CiStorageService ciStorageService,
            ControlledGitService controlledGitService,
            GitProperties gitProperties,
            ProjectRepositoryConfigRepository configRepository,
            AgentOrchestrationTaskRepository taskRepository,
            Path bareRemoteParent,
            String namePrefix) throws Exception {
        ApprovalTestFixture.allowAuthorApproval(policyRepository);
        UUID taskId = ApprovalTestFixture.seedTaskWithFullPipeline(
                mockMvc,
                objectMapper,
                accessToken,
                user,
                gitAgentService,
                artifactStorageService,
                patchStorageService,
                patchDiffParser,
                reviewStorageService,
                testingStorageService,
                pullRequestStorageService,
                repositoryConfigService,
                ciStorageService,
                controlledGitService,
                gitProperties,
                configRepository,
                taskRepository,
                bareRemoteParent,
                namePrefix);
        mockMvc.perform(post("/api/approval-gate/run")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"" + taskId + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/approval-gate/" + taskId + "/approve")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Approved for merge\"}"))
                .andExpect(status().isOk());
        return taskId;
    }

    public static String loginAdmin(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        return PullRequestTestFixture.loginAdmin(mockMvc, objectMapper);
    }
}
