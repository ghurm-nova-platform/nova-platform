package ai.nova.platform.deploymentexecution.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.deployment.dto.DeploymentDtos.ObserveDeploymentRequest;
import ai.nova.platform.deployment.support.DeploymentTestFixture;
import ai.nova.platform.deployment.entity.DeploymentHealthLevel;
import ai.nova.platform.deployment.entity.DeploymentStatus;
import ai.nova.platform.deployment.service.DeploymentObservationService;
import ai.nova.platform.release.entity.ReleaseContentType;
import ai.nova.platform.release.entity.VersionBump;
import ai.nova.platform.release.entity.VersionStrategy;
import ai.nova.platform.release.service.ReleaseManifestService;
import ai.nova.platform.release.service.ReleaseStorageService;
import ai.nova.platform.release.service.ReleaseStorageService.ContentSpec;
import ai.nova.platform.release.service.ReleaseVersionService.ResolvedVersion;
import ai.nova.platform.rollback.dto.RollbackDtos.CreateRollbackRequest;
import ai.nova.platform.rollback.entity.RollbackRiskLevel;
import ai.nova.platform.rollback.entity.RollbackStrategy;
import ai.nova.platform.rollback.service.RollbackManagerService;
import ai.nova.platform.security.AuthenticatedUser;

@Component
public class ExecutionSeedSupport {

    private final ReleaseStorageService releaseStorageService;
    private final ReleaseManifestService releaseManifestService;
    private final RollbackManagerService rollbackManagerService;
    private final DeploymentObservationService deploymentObservationService;
    private final ObjectMapper objectMapper;

    public ExecutionSeedSupport(
            ReleaseStorageService releaseStorageService,
            ReleaseManifestService releaseManifestService,
            RollbackManagerService rollbackManagerService,
            DeploymentObservationService deploymentObservationService,
            ObjectMapper objectMapper) {
        this.releaseStorageService = releaseStorageService;
        this.releaseManifestService = releaseManifestService;
        this.rollbackManagerService = rollbackManagerService;
        this.deploymentObservationService = deploymentObservationService;
        this.objectMapper = objectMapper;
    }

    public UUID seedPublishedRelease(String version) {
        UUID mergeId = UUID.randomUUID();
        var draft = releaseStorageService.createDraft(
                ExecutionTestFixture.ORG_ID,
                ExecutionTestFixture.PROJECT_ID,
                "Exec " + version,
                "execution seed",
                VersionStrategy.SEMVER,
                VersionBump.PATCH,
                new ResolvedVersion(version, 1, 0, 0, VersionBump.PATCH),
                "exec-fp-" + version + "-" + mergeId,
                ExecutionTestFixture.USER_ID,
                List.of(new ContentSpec(ReleaseContentType.MERGE_OPERATION, mergeId, null)),
                List.of());
        releaseStorageService.markPreparing(draft.getId());
        var manifest = releaseManifestService.build(
                releaseStorageService.require(draft.getId()),
                releaseStorageService.contents(draft.getId()),
                releaseStorageService.artifacts(draft.getId()));
        releaseStorageService.markReady(draft.getId(), manifest);
        releaseStorageService.markPublished(draft.getId());
        return draft.getId();
    }

    public UUID observeDeployment(MockMvc mockMvc, String accessToken, UUID releaseId) throws Exception {
        if (mockMvc == null) {
            return observeDeploymentDirect(releaseId);
        }
        MvcResult result = mockMvc.perform(post("/api/deployments/observe")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DeploymentTestFixture.observeBody(
                                releaseId,
                                "STAGING",
                                "LOCAL",
                                "exec-" + UUID.randomUUID(),
                                "SUCCEEDED",
                                "HEALTHY")))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID observeDeploymentDirect(UUID releaseId) {
        AuthenticatedUser user = ExecutionTestFixture.executionAdminUser();
        return deploymentObservationService
                .observe(
                        new ObserveDeploymentRequest(
                                releaseId,
                                "STAGING",
                                null,
                                DeploymentStatus.SUCCEEDED,
                                DeploymentHealthLevel.HEALTHY,
                                "ok",
                                "LOCAL",
                                "exec-" + UUID.randomUUID(),
                                null,
                                null,
                                null,
                                null),
                        user)
                .id();
    }

    public void seedReadyRollback(UUID currentReleaseId, UUID deploymentId, UUID targetReleaseId) {
        AuthenticatedUser user = ExecutionTestFixture.executionAdminUser();
        var rollback = rollbackManagerService.create(
                new CreateRollbackRequest(
                        currentReleaseId,
                        deploymentId,
                        targetReleaseId,
                        "STAGING",
                        RollbackStrategy.PREVIOUS_RELEASE,
                        "execution seed",
                        RollbackRiskLevel.MEDIUM),
                user);
        rollbackManagerService.validate(rollback.id(), user);
    }

    public ExecutionSeedContext seedExecutionReadyContext() throws Exception {
        long patch = Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 900_000L) + 100_000L;
        UUID targetId = seedPublishedRelease("82.1." + patch);
        UUID currentId = seedPublishedRelease("82.2." + patch);
        UUID deploymentId = observeDeployment(null, null, currentId);
        seedReadyRollback(currentId, deploymentId, targetId);
        return new ExecutionSeedContext(currentId, deploymentId, targetId);
    }

    public ExecutionSeedContext seedExecutionReadyContext(MockMvc mockMvc, String accessToken) throws Exception {
        long patch = Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 900_000L) + 100_000L;
        UUID targetId = seedPublishedRelease("82.1." + patch);
        UUID currentId = seedPublishedRelease("82.2." + patch);
        UUID deploymentId = observeDeployment(mockMvc, accessToken, currentId);
        seedReadyRollback(currentId, deploymentId, targetId);
        return new ExecutionSeedContext(currentId, deploymentId, targetId);
    }

    public record ExecutionSeedContext(UUID releaseId, UUID deploymentId, UUID targetReleaseId) {
    }
}
