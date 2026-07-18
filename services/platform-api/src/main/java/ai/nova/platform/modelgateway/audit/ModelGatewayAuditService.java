package ai.nova.platform.modelgateway.audit;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ModelGatewayAuditService {

    private static final Logger log = LoggerFactory.getLogger(ModelGatewayAuditService.class);

    public void providerCreated(UUID organizationId, UUID providerId, UUID actorId) {
        log.info(
                "Model provider created organizationId={} providerId={} actorId={}",
                organizationId,
                providerId,
                actorId);
    }

    public void providerStatusChanged(UUID organizationId, UUID providerId, String status, UUID actorId) {
        log.info(
                "Model provider status changed organizationId={} providerId={} status={} actorId={}",
                organizationId,
                providerId,
                status,
                actorId);
    }

    public void modelAssignedToProject(UUID organizationId, UUID projectId, UUID modelId, UUID actorId) {
        log.info(
                "Model assigned to project organizationId={} projectId={} modelId={} actorId={}",
                organizationId,
                projectId,
                modelId,
                actorId);
    }

    public void invocationCompleted(UUID organizationId, UUID executionId, UUID invocationId, String status) {
        log.info(
                "Model invocation completed organizationId={} executionId={} invocationId={} status={}",
                organizationId,
                executionId,
                invocationId,
                status);
    }
}
