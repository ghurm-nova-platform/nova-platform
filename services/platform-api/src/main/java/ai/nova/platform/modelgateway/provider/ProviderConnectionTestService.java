package ai.nova.platform.modelgateway.provider;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.ConnectionTestResponse;
import ai.nova.platform.modelgateway.entity.AiProvider;
import ai.nova.platform.modelgateway.entity.AiProviderType;
import ai.nova.platform.modelgateway.entity.ConnectionTestStatus;
import ai.nova.platform.modelgateway.entity.EndpointProfile;
import ai.nova.platform.modelgateway.provider.error.UnifiedProviderErrorMapper;
import ai.nova.platform.modelgateway.provider.http.ProviderRestClientFactory;
import ai.nova.platform.modelgateway.repository.AiProviderRepository;
import ai.nova.platform.modelgateway.security.ModelGatewayAuthorizationService;
import ai.nova.platform.modelgateway.service.AiProviderService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class ProviderConnectionTestService {

    private final AiProviderService providerService;
    private final AiProviderRepository providerRepository;
    private final ProviderCredentialResolver credentialResolver;
    private final ProviderRestClientFactory restClientFactory;
    private final UnifiedProviderErrorMapper errorMapper;
    private final ModelGatewayAuthorizationService authorizationService;
    private final TransactionTemplate transactionTemplate;

    public ProviderConnectionTestService(
            AiProviderService providerService,
            AiProviderRepository providerRepository,
            ProviderCredentialResolver credentialResolver,
            ProviderRestClientFactory restClientFactory,
            UnifiedProviderErrorMapper errorMapper,
            ModelGatewayAuthorizationService authorizationService,
            TransactionTemplate transactionTemplate) {
        this.providerService = providerService;
        this.providerRepository = providerRepository;
        this.credentialResolver = credentialResolver;
        this.restClientFactory = restClientFactory;
        this.errorMapper = errorMapper;
        this.authorizationService = authorizationService;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * External HTTP probe runs outside any database transaction. Persistence is a short follow-up TX
     * that applies only when provider configuration still matches the probed snapshot.
     */
    public ConnectionTestResponse testConnection(UUID providerId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.PROVIDER_CONNECTION_TEST);
        ConnectionProbeContext context =
                transactionTemplate.execute(status -> loadProbeContext(providerId, user.getOrganizationId()));
        Instant testedAt = Instant.now();

        if (context.providerType() == AiProviderType.DETERMINISTIC_LOCAL) {
            return persistResult(context, user, ConnectionTestStatus.SUCCESS, null, testedAt);
        }
        if (context.providerType() != AiProviderType.OPENAI
                && context.providerType() != AiProviderType.AZURE_OPENAI) {
            return persistResult(context, user, ConnectionTestStatus.FAILED, "PROVIDER_TYPE_UNSUPPORTED", testedAt);
        }

        String credential = credentialResolver
                .resolve(context.credentialReference(), context.organizationId())
                .orElse(null);
        if (credential == null || credential.isBlank()) {
            return persistResult(context, user, ConnectionTestStatus.FAILED, "CREDENTIAL_UNRESOLVABLE", testedAt);
        }

        try {
            // Intentionally outside any DB transaction.
            probe(context, credential);
            return persistResult(context, user, ConnectionTestStatus.SUCCESS, null, testedAt);
        } catch (ProviderException ex) {
            return persistResult(context, user, ConnectionTestStatus.FAILED, ex.errorCode(), testedAt);
        } catch (ApiException ex) {
            return persistResult(context, user, ConnectionTestStatus.FAILED, ex.getCode(), testedAt);
        } catch (Exception ex) {
            ProviderException mapped = errorMapper.mapTransport(ex);
            return persistResult(context, user, ConnectionTestStatus.FAILED, mapped.errorCode(), testedAt);
        }
    }

    private ConnectionProbeContext loadProbeContext(UUID providerId, UUID organizationId) {
        AiProvider provider = providerService.requireProvider(providerId, organizationId);
        return ConnectionProbeContext.from(provider);
    }

    private ConnectionTestResponse persistResult(
            ConnectionProbeContext context,
            AuthenticatedUser user,
            ConnectionTestStatus status,
            String errorCode,
            Instant testedAt) {
        return transactionTemplate.execute(tx -> {
            AiProvider provider =
                    providerService.requireProvider(context.providerId(), user.getOrganizationId());
            if (!context.matchesCurrent(provider)) {
                // Configuration changed during the probe — never apply a stale SUCCESS/FAILED.
                provider.setLastConnectionTestAt(testedAt);
                provider.setLastConnectionTestStatus(ConnectionTestStatus.NEVER);
                provider.setLastConnectionTestErrorCode("CONNECTION_TEST_STALE");
                provider.setUpdatedBy(user.getUserId());
                provider.setUpdatedAt(testedAt);
                providerRepository.save(provider);
                return new ConnectionTestResponse(
                        ConnectionTestStatus.NEVER, "CONNECTION_TEST_STALE", testedAt);
            }
            provider.setLastConnectionTestAt(testedAt);
            provider.setLastConnectionTestStatus(status);
            provider.setLastConnectionTestErrorCode(errorCode);
            provider.setUpdatedBy(user.getUserId());
            provider.setUpdatedAt(testedAt);
            providerRepository.save(provider);
            return new ConnectionTestResponse(status, errorCode, testedAt);
        });
    }

    private void probe(ConnectionProbeContext context, String credential) throws ProviderException {
        int timeout = context.timeoutSeconds();
        if (context.providerType() == AiProviderType.OPENAI) {
            if (context.endpointProfile() != null && context.endpointProfile() != EndpointProfile.OPENAI_PUBLIC) {
                throw new ProviderException(
                        "ENDPOINT_PROFILE_INVALID",
                        ProviderFailureKind.PERMANENT,
                        "Invalid endpoint profile");
            }
            URI base = restClientFactory.resolveOpenAiBaseUrl();
            RestClient client = restClientFactory.create(base, timeout);
            try {
                client.get()
                        .uri("/v1/models")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + credential)
                        .retrieve()
                        .toBodilessEntity();
            } catch (RestClientResponseException ex) {
                throw errorMapper.map(ex);
            }
            return;
        }

        if (context.endpointProfile() != EndpointProfile.AZURE_OPENAI_RESOURCE
                || context.azureResourceName() == null
                || context.azureApiVersion() == null) {
            throw new ProviderException(
                    "ENDPOINT_PROFILE_INVALID",
                    ProviderFailureKind.PERMANENT,
                    "Invalid Azure endpoint profile");
        }
        URI base = restClientFactory.resolveAzureBaseUrl(context.azureResourceName());
        RestClient client = restClientFactory.create(base, timeout);
        try {
            client.get()
                    .uri("/openai/models?api-version={apiVersion}", context.azureApiVersion())
                    .accept(MediaType.APPLICATION_JSON)
                    .header("api-key", credential)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw errorMapper.map(ex);
        }
    }

    record ConnectionProbeContext(
            UUID providerId,
            UUID organizationId,
            Integer providerVersion,
            AiProviderType providerType,
            String credentialReference,
            EndpointProfile endpointProfile,
            String azureResourceName,
            String azureApiVersion,
            int timeoutSeconds) {

        static ConnectionProbeContext from(AiProvider provider) {
            return new ConnectionProbeContext(
                    provider.getId(),
                    provider.getOrganizationId(),
                    provider.getVersion(),
                    provider.getProviderType(),
                    provider.getCredentialReference(),
                    provider.getEndpointProfile(),
                    provider.getAzureResourceName(),
                    provider.getAzureApiVersion(),
                    provider.getRequestTimeoutSeconds() != null ? provider.getRequestTimeoutSeconds() : 30);
        }

        boolean matchesCurrent(AiProvider provider) {
            return Objects.equals(providerVersion, provider.getVersion())
                    && Objects.equals(credentialReference, provider.getCredentialReference())
                    && Objects.equals(endpointProfile, provider.getEndpointProfile())
                    && Objects.equals(azureResourceName, provider.getAzureResourceName())
                    && Objects.equals(azureApiVersion, provider.getAzureApiVersion())
                    && providerType == provider.getProviderType();
        }
    }
}
