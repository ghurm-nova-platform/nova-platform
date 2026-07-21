package ai.nova.platform.deploymentexecution.provider;

import java.util.UUID;

import ai.nova.platform.deploymentexecution.entity.ExecutionProviderCode;

public interface DeploymentExecutionProvider {

    void prepare(ExecutionContext ctx);

    void deploy(ExecutionContext ctx);

    void verify(ExecutionContext ctx);

    void cancel(ExecutionContext ctx);

    ExecutionProviderCode providerCode();
}
