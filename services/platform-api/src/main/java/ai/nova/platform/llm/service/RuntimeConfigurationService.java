package ai.nova.platform.llm.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.llm.dto.LlmDtos.ConfigEntryView;
import ai.nova.platform.llm.entity.LlmRuntimeConfigEntity;
import ai.nova.platform.llm.mapper.LlmMapper;
import ai.nova.platform.llm.repository.LlmRuntimeConfigRepository;
import ai.nova.platform.security.AuthenticatedUser;

@Service
public class RuntimeConfigurationService {

    private final LlmRuntimeConfigRepository repository;
    private final LlmMapper mapper;

    public RuntimeConfigurationService(LlmRuntimeConfigRepository repository, LlmMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ConfigEntryView> list(UUID organizationId) {
        return repository.findByOrganizationIdOrderByConfigKeyAsc(organizationId).stream()
                .map(mapper::toConfigEntryView)
                .toList();
    }

    @Transactional
    public ConfigEntryView set(String key, String value, AuthenticatedUser user) {
        Instant now = Instant.now();
        LlmRuntimeConfigEntity entity = repository
                .findByOrganizationIdAndConfigKey(user.getOrganizationId(), key)
                .orElseGet(() -> new LlmRuntimeConfigEntity(
                        UUID.randomUUID(), user.getOrganizationId(), key, value, now));
        entity.setConfigValue(value);
        entity.touch(now);
        repository.save(entity);
        return mapper.toConfigEntryView(entity);
    }
}
