package ai.nova.platform.deploymentexecution;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import ai.nova.platform.deploymentexecution.entity.ExecutionLogLevel;
import ai.nova.platform.deploymentexecution.entity.ExecutionProviderCode;
import ai.nova.platform.deploymentexecution.provider.ArgoCdExecutionProvider;
import ai.nova.platform.deploymentexecution.provider.ExecutionContext;
import ai.nova.platform.deploymentexecution.provider.ExecutionStorageCallbacks;
import ai.nova.platform.deploymentexecution.provider.HelmExecutionProvider;
import ai.nova.platform.deploymentexecution.provider.KubernetesExecutionProvider;
import ai.nova.platform.deploymentexecution.provider.LocalExecutionProvider;

class ExecutionProviderTest {

    @Test
    void localProviderCompletesVerification() {
        LocalExecutionProvider provider = new LocalExecutionProvider();
        RecordingCallbacks callbacks = new RecordingCallbacks();
        ExecutionContext ctx = context(ExecutionProviderCode.LOCAL, callbacks);

        provider.prepare(ctx);
        provider.deploy(ctx);
        provider.verify(ctx);

        assertThat(callbacks.resultSaved).isTrue();
        assertThat(provider.providerCode()).isEqualTo(ExecutionProviderCode.LOCAL);
    }

    @Test
    void adapterProvidersDocumentDryRun() {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        ExecutionContext ctx = context(ExecutionProviderCode.KUBERNETES, callbacks);

        new KubernetesExecutionProvider().verify(ctx);
        new ArgoCdExecutionProvider().verify(ctx);
        new HelmExecutionProvider().verify(ctx);

        assertThat(callbacks.resultSaved).isTrue();
        assertThat(callbacks.lastSummary).contains("adapter only");
    }

    private static ExecutionContext context(ExecutionProviderCode provider, ExecutionStorageCallbacks callbacks) {
        return new ExecutionContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                provider,
                null,
                callbacks);
    }

    private static final class RecordingCallbacks implements ExecutionStorageCallbacks {
        boolean resultSaved;
        String lastSummary;

        @Override
        public void appendLog(UUID executionId, ExecutionLogLevel level, String message) {
        }

        @Override
        public void saveResult(UUID executionId, boolean success, String summary, String providerResponseJson) {
            resultSaved = true;
            lastSummary = summary;
        }

        @Override
        public void saveArtifact(
                UUID executionId, String artifactType, String name, String contentRef, String checksum) {
        }

        @Override
        public void updateStep(UUID executionId, String stepKey, String stage, String detail) {
        }
    }
}
