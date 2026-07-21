package ai.nova.platform.llm.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.llm.configuration.LlmProperties;
import ai.nova.platform.llm.dto.LlmDtos.ProviderStatusView;
import ai.nova.platform.llm.entity.LlmProviderHealthStatus;
import ai.nova.platform.llm.entity.LlmProviderStatusEntity;
import ai.nova.platform.llm.entity.LlmProviderType;
import ai.nova.platform.llm.mapper.LlmMapper;
import ai.nova.platform.llm.provider.LlmProviderRegistry;
import ai.nova.platform.llm.provider.LlmRuntimeProvider;
import ai.nova.platform.llm.repository.LlmProviderStatusRepository;

@Service
public class HealthCheckService {

    private final LlmProviderRegistry providerRegistry;
    private final LlmProviderStatusRepository statusRepository;
    private final LlmProperties properties;
    private final LlmMapper mapper;

    public HealthCheckService(
            LlmProviderRegistry providerRegistry,
            LlmProviderStatusRepository statusRepository,
            LlmProperties properties,
            LlmMapper mapper) {
        this.providerRegistry = providerRegistry;
        this.statusRepository = statusRepository;
        this.properties = properties;
        this.mapper = mapper;
    }

    @Transactional
    public List<ProviderStatusView> checkAll(UUID organizationId) {
        Instant now = Instant.now();
        for (LlmRuntimeProvider provider : providerRegistry.list()) {
            LlmRuntimeProvider.ProviderHealth health = provider.health();
            LlmProviderStatusEntity entity = statusRepository
                    .findByOrganizationIdAndProviderType(organizationId, provider.providerType())
                    .orElseGet(() -> new LlmProviderStatusEntity(
                            UUID.randomUUID(),
                            organizationId,
                            provider.providerType(),
                            LlmProviderHealthStatus.UNKNOWN,
                            now));
            entity.setStatus(health.status());
            entity.setLastError(
                    health.status() == LlmProviderHealthStatus.HEALTHY
                                    || health.status() == LlmProviderHealthStatus.DISABLED
                            ? null
                            : health.detail());
            entity.setLastHealthCheckAt(now);
            entity.setEndpointUrl(endpointFor(provider.providerType()));
            entity.touch(now);
            statusRepository.save(entity);
        }
        return statusRepository.findByOrganizationIdOrderByProviderTypeAsc(organizationId).stream()
                .map(mapper::toProviderStatusView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderStatusView> list(UUID organizationId) {
        return statusRepository.findByOrganizationIdOrderByProviderTypeAsc(organizationId).stream()
                .map(mapper::toProviderStatusView)
                .toList();
    }

    private String endpointFor(LlmProviderType type) {
        return switch (type) {
            case OLLAMA -> properties.getOllama().getBaseUrl();
            case LLAMA_CPP -> properties.getLlamacpp().getBaseUrl();
            case VLLM -> properties.getVllm().getBaseUrl();
            default -> null;
        };
    }
}
