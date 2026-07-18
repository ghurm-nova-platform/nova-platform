package ai.nova.platform.modelgateway.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ai.nova.platform.agent.runtime.RuntimeFinalResponse;
import ai.nova.platform.agent.runtime.RuntimeMessage;
import ai.nova.platform.agent.runtime.RuntimeTurnResult;
import ai.nova.platform.modelcatalog.entity.AiModelCapability;
import ai.nova.platform.modelcatalog.service.ModelCapabilityMatcher;
import ai.nova.platform.modelcatalog.service.ModelReferenceResolver;
import ai.nova.platform.modelcatalog.service.ModelReferenceResolver.ResolvedModel;
import ai.nova.platform.modelgateway.config.ModelGatewayInvokeExecutorConfig;
import ai.nova.platform.modelgateway.config.ModelGatewayProperties;
import ai.nova.platform.modelgateway.entity.AiModel;
import ai.nova.platform.modelgateway.entity.AiModelStatus;
import ai.nova.platform.modelgateway.entity.AiModelType;
import ai.nova.platform.modelgateway.entity.AiProvider;
import ai.nova.platform.modelgateway.entity.AiProviderStatus;
import ai.nova.platform.modelgateway.entity.AiProviderType;
import ai.nova.platform.modelgateway.entity.InvocationStatus;
import ai.nova.platform.modelgateway.entity.ModelInvocation;
import ai.nova.platform.modelgateway.provider.AiModelProvider;
import ai.nova.platform.modelgateway.provider.AiModelProviderRegistry;
import ai.nova.platform.modelgateway.provider.ProviderConcurrencyManager;
import ai.nova.platform.modelgateway.provider.ProviderCredentialResolver;
import ai.nova.platform.modelgateway.provider.ProviderInvokeRequest;
import ai.nova.platform.modelgateway.provider.ProviderInvokeResult;
import ai.nova.platform.modelgateway.routing.ModelRoutingService;
import ai.nova.platform.modelgateway.usage.ModelUsageRecorder;
import ai.nova.platform.web.error.ApiException;

class AiModelGatewayModelReferenceCapabilityTest {

    private ModelGatewayProperties properties;
    private ModelReferenceResolver modelReferenceResolver;
    private ModelCapabilityMatcher capabilityMatcher;
    private ModelInvocationPersistenceService persistenceService;
    private AiModelProviderRegistry providerRegistry;
    private ProviderCredentialResolver credentialResolver;
    private ProviderConcurrencyManager concurrencyManager;
    private ModelGatewayRuntimeMapper runtimeMapper;
    private ExecutorService invokeExecutor;
    private AiModelProvider adapter;
    private AiModelGateway gateway;

    private final UUID organizationId = UUID.randomUUID();
    private final UUID executionId = UUID.randomUUID();
    private final UUID providerId = UUID.randomUUID();
    private final UUID modelId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        properties = new ModelGatewayProperties();
        properties.setEnabled(true);
        properties.setMaximumTimeoutSeconds(30);
        properties.setMaxProviderAttempts(3);
        properties.setMaxTotalDurationMs(60_000L);
        properties.setInvokeCancelGraceMs(50L);
        properties.setMaxConcurrentRequestsPerProvider(10);

        modelReferenceResolver = mock(ModelReferenceResolver.class);
        capabilityMatcher = mock(ModelCapabilityMatcher.class);
        persistenceService = mock(ModelInvocationPersistenceService.class);
        providerRegistry = mock(AiModelProviderRegistry.class);
        credentialResolver = mock(ProviderCredentialResolver.class);
        concurrencyManager = new ProviderConcurrencyManager(properties);
        runtimeMapper = mock(ModelGatewayRuntimeMapper.class);
        adapter = mock(AiModelProvider.class);
        invokeExecutor = new ModelGatewayInvokeExecutorConfig().modelGatewayInvokeExecutor(properties);

        when(persistenceService.isExecutionCancelled(any())).thenReturn(false);
        when(persistenceService.nextAttemptNumber(any())).thenReturn(1);
        when(credentialResolver.resolve(nullable(String.class), any())).thenReturn(Optional.empty());
        when(providerRegistry.isRegistered("DETERMINISTIC_LOCAL")).thenReturn(true);
        when(providerRegistry.require("DETERMINISTIC_LOCAL")).thenReturn(adapter);
        when(adapter.adapterKey()).thenReturn("DETERMINISTIC_LOCAL");
        when(persistenceService.createRunning(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        nullable(UUID.class),
                        any(),
                        any(),
                        nullable(UUID.class),
                        anyInt(),
                        anyInt(),
                        any(),
                        nullable(UUID.class)))
                .thenAnswer(invocation -> {
                    ModelInvocation mi = new ModelInvocation();
                    mi.setId(invocation.getArgument(0));
                    mi.setExecutionId(executionId);
                    mi.setStatus(InvocationStatus.RUNNING);
                    mi.setStartedAt(Instant.now());
                    return mi;
                });

        gateway = new AiModelGateway(
                properties,
                mock(ModelRoutingService.class),
                modelReferenceResolver,
                capabilityMatcher,
                persistenceService,
                mock(ModelUsageRecorder.class),
                providerRegistry,
                credentialResolver,
                concurrencyManager,
                runtimeMapper,
                new ModelGatewayInputValidator(properties),
                invokeExecutor);
    }

    @AfterEach
    void tearDown() {
        invokeExecutor.shutdownNow();
    }

    @Test
    void embeddingsOnlyModelReferenceIsRejected() throws Exception {
        when(modelReferenceResolver.resolve(organizationId, "embed-only"))
                .thenReturn(resolvedModel(AiModelType.EMBEDDING));
        when(capabilityMatcher.hasCapability(modelId, AiModelCapability.CHAT)).thenReturn(false);

        assertThatThrownBy(() -> gateway.invoke(request("embed-only", false)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MODEL_CAPABILITY_MISSING");
        verify(adapter, never()).invoke(any(ProviderInvokeRequest.class));
    }

    @Test
    void imageGenerationOnlyModelReferenceIsRejected() throws Exception {
        when(modelReferenceResolver.resolve(organizationId, "image-only"))
                .thenReturn(resolvedModel(AiModelType.MULTIMODAL));
        when(capabilityMatcher.hasCapability(modelId, AiModelCapability.CHAT)).thenReturn(false);

        assertThatThrownBy(() -> gateway.invoke(request("image-only", false)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MODEL_CAPABILITY_MISSING");
        verify(adapter, never()).invoke(any(ProviderInvokeRequest.class));
    }

    @Test
    void chatModelReferenceSucceeds() throws Exception {
        when(modelReferenceResolver.resolve(organizationId, "chat-model"))
                .thenReturn(resolvedModel(AiModelType.CHAT));
        when(capabilityMatcher.hasCapability(modelId, AiModelCapability.CHAT)).thenReturn(true);
        when(adapter.invoke(any(ProviderInvokeRequest.class)))
                .thenReturn(ProviderInvokeResult.finalResponse("ok", 1, 1, 5L, "stop"));

        ModelInvocation completed = new ModelInvocation();
        completed.setId(UUID.randomUUID());
        completed.setExecutionId(executionId);
        completed.setStatus(InvocationStatus.COMPLETED);
        when(persistenceService.completeSuccess(any(), any()))
                .thenReturn(ModelInvocationPersistenceService.CompletionOutcome.from(completed));
        when(runtimeMapper.toTurnResult(any(), any()))
                .thenReturn(RuntimeTurnResult.finalResponse(new RuntimeFinalResponse("ok", 1, 1, 2, 5L)));

        ModelGatewayResponse response = gateway.invoke(request("chat-model", false));
        assertThat(response.attemptCount()).isEqualTo(1);
        verify(adapter).invoke(any(ProviderInvokeRequest.class));
    }

    @Test
    void chatModelWithoutToolsIsRejectedWhenToolsRequired() throws Exception {
        AiModel model = resolvedModel(AiModelType.CHAT).model();
        model.setSupportsTools(false);
        when(modelReferenceResolver.resolve(organizationId, "chat-no-tools"))
                .thenReturn(new ResolvedModel(
                        model,
                        resolvedModel(AiModelType.CHAT).provider(),
                        "chat-no-tools"));
        when(capabilityMatcher.hasCapability(modelId, AiModelCapability.CHAT)).thenReturn(true);
        when(capabilityMatcher.hasAnyCapability(
                        eq(modelId),
                        eq(List.of(AiModelCapability.TOOL_CALLING, AiModelCapability.FUNCTION_CALLING))))
                .thenReturn(false);

        assertThatThrownBy(() -> gateway.invoke(request("chat-no-tools", true)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MODEL_CAPABILITY_MISSING");
        verify(adapter, never()).invoke(any(ProviderInvokeRequest.class));
    }

    private ModelGatewayRequest request(String modelReference, boolean requiresTools) {
        return new ModelGatewayRequest(
                organizationId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                executionId,
                null,
                UUID.randomUUID(),
                "system",
                List.of(new RuntimeMessage("user", "hello")),
                List.of(),
                List.of(),
                null,
                requiresTools,
                false,
                modelReference);
    }

    private ResolvedModel resolvedModel(AiModelType type) {
        AiProvider provider = new AiProvider();
        provider.setId(providerId);
        provider.setOrganizationId(organizationId);
        provider.setName("Local");
        provider.setProviderType(AiProviderType.DETERMINISTIC_LOCAL);
        provider.setAdapterKey("DETERMINISTIC_LOCAL");
        provider.setStatus(AiProviderStatus.ACTIVE);
        provider.setRequestTimeoutSeconds(10);
        provider.setMaxRetries(0);
        provider.setRetryBackoffMs(0);
        provider.setMaxConcurrentRequests(10);

        AiModel model = new AiModel();
        model.setId(modelId);
        model.setOrganizationId(organizationId);
        model.setProviderId(providerId);
        model.setModelKey("ref-model");
        model.setProviderModelId("provider-model");
        model.setDisplayName("Ref");
        model.setModelType(type);
        model.setStatus(AiModelStatus.ACTIVE);
        model.setContextWindowTokens(8192);
        model.setMaxOutputTokens(2048);
        model.setSupportsTools(false);
        model.setSupportsKnowledgeContext(true);

        return new ResolvedModel(model, provider, model.getModelKey());
    }
}
