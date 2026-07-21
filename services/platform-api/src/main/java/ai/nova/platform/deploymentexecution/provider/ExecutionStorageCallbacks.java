package ai.nova.platform.deploymentexecution.provider;

import java.util.UUID;

import ai.nova.platform.deploymentexecution.entity.ExecutionLogLevel;

public interface ExecutionStorageCallbacks {

    void appendLog(UUID executionId, ExecutionLogLevel level, String message);

    void saveResult(UUID executionId, boolean success, String summary, String providerResponseJson);

    void saveArtifact(UUID executionId, String artifactType, String name, String contentRef, String checksum);

    void updateStep(UUID executionId, String stepKey, String stage, String detail);
}
