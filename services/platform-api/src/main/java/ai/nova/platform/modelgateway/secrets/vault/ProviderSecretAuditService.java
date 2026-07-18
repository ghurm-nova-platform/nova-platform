package ai.nova.platform.modelgateway.secrets.vault;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProviderSecretAuditService {

    private static final Logger log = LoggerFactory.getLogger(ProviderSecretAuditService.class);

    public void secretCreated(UUID organizationId, UUID secretId, UUID actorId) {
        log.info(
                "Provider secret created organizationId={} secretId={} actorId={}",
                organizationId,
                secretId,
                actorId);
    }

    public void secretRotated(UUID organizationId, UUID secretId, UUID actorId) {
        log.info(
                "Provider secret rotated organizationId={} secretId={} actorId={}",
                organizationId,
                secretId,
                actorId);
    }

    public void secretRevoked(UUID organizationId, UUID secretId, UUID actorId) {
        log.info(
                "Provider secret revoked organizationId={} secretId={} actorId={}",
                organizationId,
                secretId,
                actorId);
    }
}
