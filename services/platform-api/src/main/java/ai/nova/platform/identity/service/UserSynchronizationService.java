package ai.nova.platform.identity.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.entity.ProviderType;

@Service
public class UserSynchronizationService {

    private static final Logger log = LoggerFactory.getLogger(UserSynchronizationService.class);

    private final IdentityMetrics identityMetrics;

    public UserSynchronizationService(IdentityMetrics identityMetrics) {
        this.identityMetrics = identityMetrics;
    }

    @Transactional
    public void synchronizeUser(UUID organizationId, UUID platformUserId) {
        identityMetrics.recordSyncJob();
        log.info("Identity user synchronization requested organizationId={} platformUserId={}", organizationId, platformUserId);
    }

    @Transactional
    public void synchronizeProvider(UUID organizationId, UUID providerId, ProviderType providerType) {
        identityMetrics.recordSyncJob();
        log.info(
                "Identity provider synchronization requested organizationId={} providerId={} type={}",
                organizationId,
                providerId,
                providerType);
    }

    @Transactional
    public void synchronizeGroup(UUID organizationId, UUID groupId) {
        identityMetrics.recordSyncJob();
        log.info("Identity group synchronization requested organizationId={} groupId={}", organizationId, groupId);
    }
}
