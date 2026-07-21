package ai.nova.platform.deploymentexecution.provider;

import org.springframework.stereotype.Component;

import ai.nova.platform.deploymentexecution.entity.ExecutionLogLevel;
import ai.nova.platform.deploymentexecution.entity.ExecutionProviderCode;

@Component
public class LocalExecutionProvider implements DeploymentExecutionProvider {

    @Override
    public void prepare(ExecutionContext ctx) {
        ctx.updateStep("prepare", "PREPARE", "Preparing local deployment workspace");
        ctx.log(ExecutionLogLevel.INFO, "Local provider preparing deployment for release " + ctx.getReleaseId());
    }

    @Override
    public void deploy(ExecutionContext ctx) {
        ctx.updateStep("deploy", "DEPLOY", "Applying local deployment");
        ctx.log(ExecutionLogLevel.INFO, "Local provider simulated deploy completed instantly");
        ctx.saveArtifact("MANIFEST", "release-manifest", "memory://local/" + ctx.getReleaseId(), "local-sim");
    }

    @Override
    public void verify(ExecutionContext ctx) {
        ctx.updateStep("verify", "VERIFY", "Verifying local deployment");
        ctx.log(ExecutionLogLevel.INFO, "Local provider verification passed");
        ctx.saveResult(true, "Local deployment completed successfully", "{\"mode\":\"local-simulated\"}");
    }

    @Override
    public void cancel(ExecutionContext ctx) {
        ctx.log(ExecutionLogLevel.WARN, "Local provider cancel acknowledged");
    }

    @Override
    public ExecutionProviderCode providerCode() {
        return ExecutionProviderCode.LOCAL;
    }
}
