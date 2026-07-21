package ai.nova.platform.identity.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSynchronizationService {

    @Transactional
    public void synchronizeUser(UUID organizationId, UUID platformUserId) {
        // Directory sync hook stub for LDAP/AD/SCIM integrations.
    }
}
