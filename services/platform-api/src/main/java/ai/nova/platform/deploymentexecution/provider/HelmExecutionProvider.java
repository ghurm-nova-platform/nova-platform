package ai.nova.platform.deploymentexecution.provider;

import org.springframework.stereotype.Component;

import ai.nova.platform.deploymentexecution.entity.ExecutionLogLevel;
import ai.nova.platform.deploymentexecution.entity.ExecutionProviderCode;

@Component
public class HelmExecutionProvider implements DeploymentExecutionProvider {

    private static final String ADAPTER_NOTE =
            "Helm adapter only — no real Helm releases are applied in Sprint 5 Phase 1";

    @Override
    public void prepare(ExecutionContext ctx) {
        ctx.log(ExecutionLogLevel.WARN, ADAPTER_NOTE);
        ctx.updateStep("prepare", "PREPARE", ADAPTER_NOTE);
    }

    @Override
    public void deploy(ExecutionContext ctx) {
        ctx.log(ExecutionLogLevel.WARN, ADAPTER_NOTE);
        ctx.updateStep("deploy", "DEPLOY", "Helm adapter dry run");
    }

    @Override
    public void verify(ExecutionContext ctx) {
        ctx.log(ExecutionLogLevel.WARN, ADAPTER_NOTE);
        ctx.saveResult(true, ADAPTER_NOTE, "{\"adapter\":\"helm\",\"dryRun\":true}");
    }

    @Override
    public void cancel(ExecutionContext ctx) {
        ctx.log(ExecutionLogLevel.WARN, "Helm adapter cancel acknowledged");
    }

    @Override
    public ExecutionProviderCode providerCode() {
        return ExecutionProviderCode.HELM;
    }
}
