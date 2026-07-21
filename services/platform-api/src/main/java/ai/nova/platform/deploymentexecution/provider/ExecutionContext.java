package ai.nova.platform.deploymentexecution.provider;

import java.util.UUID;

import ai.nova.platform.deploymentexecution.entity.ExecutionLogLevel;
import ai.nova.platform.deploymentexecution.entity.ExecutionProviderCode;

public final class ExecutionContext {

    private final UUID executionId;
    private final UUID organizationId;
    private final UUID projectId;
    private final UUID releaseId;
    private final UUID environmentId;
    private final ExecutionProviderCode provider;
    private final String restDeployUrl;
    private final ExecutionStorageCallbacks callbacks;

    public ExecutionContext(
            UUID executionId,
            UUID organizationId,
            UUID projectId,
            UUID releaseId,
            UUID environmentId,
            ExecutionProviderCode provider,
            String restDeployUrl,
            ExecutionStorageCallbacks callbacks) {
        this.executionId = executionId;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.releaseId = releaseId;
        this.environmentId = environmentId;
        this.provider = provider;
        this.restDeployUrl = restDeployUrl;
        this.callbacks = callbacks;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getReleaseId() {
        return releaseId;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public ExecutionProviderCode getProvider() {
        return provider;
    }

    public String getRestDeployUrl() {
        return restDeployUrl;
    }

    public void log(ExecutionLogLevel level, String message) {
        callbacks.appendLog(executionId, level, message);
    }

    public void saveResult(boolean success, String summary, String providerResponseJson) {
        callbacks.saveResult(executionId, success, summary, providerResponseJson);
    }

    public void saveArtifact(String artifactType, String name, String contentRef, String checksum) {
        callbacks.saveArtifact(executionId, artifactType, name, contentRef, checksum);
    }

    public void updateStep(String stepKey, String stage, String detail) {
        callbacks.updateStep(executionId, stepKey, stage, detail);
    }
}
