package ai.nova.platform.modelgateway.provider;

import java.net.URI;
import java.time.Instant;
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
     * External HTTP probe runs outside any database transaction. Persistence is a short follow-up TX.
     */
    public ConnectionTestResponse testConnection(UUID providerId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.PROVIDER_CONNECTION_TEST);
        ConnectionProbeContext context = transactionTemplate.execute(status -> loadProbeContext(providerId, user.getOrganizationId()));
        Instant testedAt = Instant.now();

        if (context.providerType() == AiProviderType.DETERMINISTIC_LOCAL) {
            return persistResult(providerId, user, ConnectionTestStatus.SUCCESS, null, testedAt);
        }
        if (context.providerType() != AiProviderType.OPENAI
                && context.providerType() != AiProviderType.AZURE_OPENAI) {
            return persistResult(providerId, user, ConnectionTestStatus.FAILED, "PROVIDER_TYPE_UNSUPPORTED", testedAt);
        }

        String credential = credentialResolver
                .resolve(context.credentialReference(), context.organizationId())
                .orElse(null);
        if (credential == null || credential.isBlank()) {
            return persistResult(providerId, user, ConnectionTestStatus.FAILED, "CREDENTIAL_UNRESOLVABLE", testedAt);
        }

        try {
            // Intentionally outside any DB transaction.
            probe(context, credential);
            return persistResult(providerId, user, ConnectionTestStatus.SUCCESS, null, testedAt);
        } catch (ProviderException ex) {
            return persistResult(providerId, user, ConnectionTestStatus.FAILED, ex.errorCode(), testedAt);
        } catch (ApiException ex) {
            return persistResult(providerId, user, ConnectionTestStatus.FAILED, ex.getCode(), testedAt);
        } catch (Exception ex) {
            ProviderException mapped = errorMapper.mapTransport(ex);
            return persistResult(providerId, user, ConnectionTestStatus.FAILED, mapped.errorCode(), testedAt);
        }
    }

    private ConnectionProbeContext loadProbeContext(UUID providerId, UUID organizationId) {
        AiProvider provider = providerService.requireProvider(providerId, organizationId);
        return new ConnectionProbeContext(
                provider.getId(),
                provider.getOrganizationId(),
                provider.getProviderType(),
                provider.getCredentialReference(),
                provider.getEndpointProfile(),
                provider.getAzureResourceName(),
                provider.getAzureApiVersion(),
                provider.getRequestTimeoutSeconds() != null ? provider.getRequestTimeoutSeconds() : 30);
    }

    private ConnectionTestResponse persistResult(
            UUID providerId,
            AuthenticatedUser user,
            ConnectionTestStatus status,
            String errorCode,
            Instant testedAt) {
        return transactionTemplate.execute(tx -> {
            AiProvider provider = providerService.requireProvider(providerId, user.getOrganizationId());
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

    private record ConnectionProbeContext(
            UUID providerId,
            UUID organizationId,
            AiProviderType providerType,
            String credentialReference,
            EndpointProfile endpointProfile,
            String azureResourceName,
            String azureApiVersion,
            int timeoutSeconds) {
    }
}
