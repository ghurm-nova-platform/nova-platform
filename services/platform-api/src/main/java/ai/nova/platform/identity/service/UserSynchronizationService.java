package ai.nova.platform.identity.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.entity.ProviderType;

@Service
public class UserSynchronizationService {

    @Transactional
    public void synchronizeUser(UUID organizationId, UUID platformUserId) {
        // Directory sync hook stub for LDAP/AD/SCIM integrations.
    }

    @Transactional
    public void synchronizeProvider(UUID organizationId, UUID providerId, ProviderType providerType) {
        // Provider sync hook stub.
    }

    @Transactional
    public void synchronizeGroup(UUID organizationId, UUID groupId) {
        // Group sync hook stub.
    }
}
