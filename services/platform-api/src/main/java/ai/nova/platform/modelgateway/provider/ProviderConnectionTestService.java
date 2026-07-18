package ai.nova.platform.modelgateway.provider;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public ProviderConnectionTestService(
            AiProviderService providerService,
            AiProviderRepository providerRepository,
            ProviderCredentialResolver credentialResolver,
            ProviderRestClientFactory restClientFactory,
            UnifiedProviderErrorMapper errorMapper,
            ModelGatewayAuthorizationService authorizationService) {
        this.providerService = providerService;
        this.providerRepository = providerRepository;
        this.credentialResolver = credentialResolver;
        this.restClientFactory = restClientFactory;
        this.errorMapper = errorMapper;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public ConnectionTestResponse testConnection(UUID providerId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.PROVIDER_CONNECTION_TEST);
        AiProvider provider = providerService.requireProvider(providerId, user.getOrganizationId());
        Instant testedAt = Instant.now();

        if (provider.getProviderType() == AiProviderType.DETERMINISTIC_LOCAL) {
            return persist(provider, ConnectionTestStatus.SUCCESS, null, testedAt, user);
        }
        if (provider.getProviderType() != AiProviderType.OPENAI
                && provider.getProviderType() != AiProviderType.AZURE_OPENAI) {
            return persist(provider, ConnectionTestStatus.FAILED, "PROVIDER_TYPE_UNSUPPORTED", testedAt, user);
        }

        String credential = credentialResolver
                .resolve(provider.getCredentialReference(), provider.getOrganizationId())
                .orElse(null);
        if (credential == null || credential.isBlank()) {
            return persist(provider, ConnectionTestStatus.FAILED, "CREDENTIAL_UNRESOLVABLE", testedAt, user);
        }

        try {
            probe(provider, credential);
            return persist(provider, ConnectionTestStatus.SUCCESS, null, testedAt, user);
        } catch (ProviderException ex) {
            return persist(provider, ConnectionTestStatus.FAILED, ex.errorCode(), testedAt, user);
        } catch (ApiException ex) {
            return persist(provider, ConnectionTestStatus.FAILED, ex.getCode(), testedAt, user);
        } catch (Exception ex) {
            ProviderException mapped = errorMapper.mapTransport(ex);
            return persist(provider, ConnectionTestStatus.FAILED, mapped.errorCode(), testedAt, user);
        }
    }

    private void probe(AiProvider provider, String credential) throws ProviderException {
        int timeout = provider.getRequestTimeoutSeconds() != null ? provider.getRequestTimeoutSeconds() : 30;
        if (provider.getProviderType() == AiProviderType.OPENAI) {
            if (provider.getEndpointProfile() != null
                    && provider.getEndpointProfile() != EndpointProfile.OPENAI_PUBLIC) {
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

        if (provider.getEndpointProfile() != EndpointProfile.AZURE_OPENAI_RESOURCE
                || provider.getAzureResourceName() == null
                || provider.getAzureApiVersion() == null) {
            throw new ProviderException(
                    "ENDPOINT_PROFILE_INVALID",
                    ProviderFailureKind.PERMANENT,
                    "Invalid Azure endpoint profile");
        }
        URI base = restClientFactory.resolveAzureBaseUrl(provider.getAzureResourceName());
        RestClient client = restClientFactory.create(base, timeout);
        try {
            client.get()
                    .uri("/openai/models?api-version={apiVersion}", provider.getAzureApiVersion())
                    .accept(MediaType.APPLICATION_JSON)
                    .header("api-key", credential)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw errorMapper.map(ex);
        }
    }

    private ConnectionTestResponse persist(
            AiProvider provider,
            ConnectionTestStatus status,
            String errorCode,
            Instant testedAt,
            AuthenticatedUser user) {
        provider.setLastConnectionTestAt(testedAt);
        provider.setLastConnectionTestStatus(status);
        provider.setLastConnectionTestErrorCode(errorCode);
        provider.setUpdatedBy(user.getUserId());
        provider.setUpdatedAt(testedAt);
        providerRepository.save(provider);
        return new ConnectionTestResponse(status, errorCode, testedAt);
    }
}
