package ai.nova.platform.modelcatalog.service;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.modelcatalog.entity.AiModelCapability;
import ai.nova.platform.modelcatalog.entity.AiModelCapabilityEntity;
import ai.nova.platform.modelcatalog.repository.AiModelCapabilityRepository;
import ai.nova.platform.modelgateway.entity.AiModel;
import ai.nova.platform.modelgateway.entity.AiModelStatus;
import ai.nova.platform.modelgateway.repository.AiModelRepository;

@Service
public class ModelCapabilityMatcher {

    private final AiModelCapabilityRepository capabilityRepository;
    private final AiModelRepository modelRepository;

    public ModelCapabilityMatcher(
            AiModelCapabilityRepository capabilityRepository, AiModelRepository modelRepository) {
        this.capabilityRepository = capabilityRepository;
        this.modelRepository = modelRepository;
    }

    @Transactional(readOnly = true)
    public boolean hasCapability(UUID modelId, AiModelCapability capability) {
        return capabilityRepository.existsByIdModelIdAndIdCapabilityAndEnabledTrue(modelId, capability);
    }

    @Transactional(readOnly = true)
    public boolean hasAnyCapability(UUID modelId, Collection<AiModelCapability> capabilities) {
        for (AiModelCapability capability : capabilities) {
            if (hasCapability(modelId, capability)) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<AiModel> findActiveModelsByCapabilities(
            UUID organizationId, Collection<AiModelCapability> required, boolean matchAll) {
        if (required == null || required.isEmpty()) {
            return modelRepository.findByOrganizationIdAndStatus(organizationId, AiModelStatus.ACTIVE);
        }
        List<AiModel> active = modelRepository.findByOrganizationIdAndStatus(organizationId, AiModelStatus.ACTIVE);
        Set<AiModelCapability> requiredSet = EnumSet.copyOf(required);
        return active.stream()
                .filter(model -> matches(model.getId(), requiredSet, matchAll))
                .toList();
    }

    private boolean matches(UUID modelId, Set<AiModelCapability> required, boolean matchAll) {
        List<AiModelCapabilityEntity> caps = capabilityRepository.findByIdModelId(modelId);
        Set<AiModelCapability> enabled = caps.stream()
                .filter(AiModelCapabilityEntity::isEnabled)
                .map(AiModelCapabilityEntity::getCapability)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(AiModelCapability.class)));
        if (matchAll) {
            return enabled.containsAll(required);
        }
        for (AiModelCapability capability : required) {
            if (enabled.contains(capability)) {
                return true;
            }
        }
        return false;
    }
}
