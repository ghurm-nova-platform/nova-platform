package ai.nova.platform.deploymentexecution.service;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ai.nova.platform.deploymentexecution.config.ExecutionProperties;
import ai.nova.platform.deploymentexecution.entity.DeploymentExecutionEntity;
import ai.nova.platform.deploymentexecution.entity.ExecutionEventType;
import ai.nova.platform.deploymentexecution.entity.ExecutionLogLevel;
import ai.nova.platform.deploymentexecution.entity.ExecutionStatus;
import ai.nova.platform.deploymentexecution.provider.DeploymentExecutionProvider;
import ai.nova.platform.deploymentexecution.provider.DeploymentExecutionProviderRegistry;
import ai.nova.platform.deploymentexecution.provider.ExecutionContext;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

/**
 * Background worker that runs prepare/deploy/verify outside HTTP request transactions.
 * Cancellation is cooperative: cancel_requested is checked between stages and before COMPLETED.
 */
@Service
public class DeploymentExecutionWorker {

    private static final Logger log = LoggerFactory.getLogger(DeploymentExecutionWorker.class);

    private final ExecutionProperties properties;
    private final ExecutionTransitionService transitionService;
    private final ExecutionStorageService storageService;
    private final DeploymentExecutionProviderRegistry providerRegistry;

    public DeploymentExecutionWorker(
            ExecutionProperties properties,
            ExecutionTransitionService transitionService,
            ExecutionStorageService storageService,
            DeploymentExecutionProviderRegistry providerRegistry) {
        this.properties = properties;
        this.transitionService = transitionService;
        this.storageService = storageService;
        this.providerRegistry = providerRegistry;
    }

    public void run(UUID executionId, AuthenticatedUser user) {
        Instant deadline = Instant.now().plusSeconds(Math.max(1, properties.getExecutionTimeoutSeconds()));
        try {
            if (abortIfCancelled(executionId, user)) {
                return;
            }
            DeploymentExecutionEntity entity = transitionService.require(executionId);
            DeploymentExecutionProvider provider = providerRegistry.require(entity.getProvider());
            ExecutionContext context = new ExecutionContext(
                    executionId,
                    entity.getOrganizationId(),
                    entity.getProjectId(),
                    entity.getReleaseOperationId(),
                    entity.getEnvironmentId(),
                    entity.getProvider(),
                    null,
                    storageService);

            ensureDeadline(deadline, "prepare");
            provider.prepare(context);
            storageService.completeStep(executionId, "prepare", true, "Prepare completed");
            if (abortIfCancelled(executionId, user)) {
                return;
            }

            if (!transitionService.transition(
                    executionId,
                    ExecutionStatus.STARTING,
                    ExecutionStatus.DEPLOYING,
                    ExecutionEventType.DEPLOYING,
                    "deploy",
                    "DEPLOY",
                    "Deploying",
                    user,
                    null)) {
                abortIfCancelled(executionId, user);
                return;
            }

            ensureDeadline(deadline, "deploy");
            provider.deploy(context);
            storageService.completeStep(executionId, "deploy", true, "Deploy completed");
            if (abortIfCancelled(executionId, user)) {
                return;
            }

            if (!transitionService.transition(
                    executionId,
                    ExecutionStatus.DEPLOYING,
                    ExecutionStatus.VERIFYING,
                    ExecutionEventType.VERIFYING,
                    "verify",
                    "VERIFY",
                    "Verifying deployment",
                    user,
                    ai.nova.platform.audit.entity.AuditAction.VERIFY)) {
                abortIfCancelled(executionId, user);
                return;
            }

            ensureDeadline(deadline, "verify");
            int verifyTimeout = Math.max(1, properties.getVerificationTimeoutSeconds());
            Instant verifyDeadline = Instant.now().plusSeconds(verifyTimeout);
            runWithVerifyBudget(provider, context, verifyDeadline);
            storageService.completeStep(executionId, "verify", true, "Verify completed");

            if (abortIfCancelled(executionId, user)) {
                return;
            }
            if (!transitionService.completeIfNotCancelled(executionId, user)) {
                abortIfCancelled(executionId, user);
            }
        } catch (Exception ex) {
            log.warn("Deployment execution {} failed: {}", executionId, ex.toString());
            if (abortIfCancelled(executionId, user)) {
                return;
            }
            String code = ex instanceof ApiException api ? api.getCode() : "EXECUTION_FAILED";
            String message = ex.getMessage() != null ? ex.getMessage() : "Execution failed";
            transitionService.failIfActive(executionId, code, message, user);
        }
    }

    private void runWithVerifyBudget(
            DeploymentExecutionProvider provider, ExecutionContext context, Instant verifyDeadline) {
        long remainingMs = Math.max(1, verifyDeadline.toEpochMilli() - Instant.now().toEpochMilli());
        try {
            // Verification itself is synchronous; budget enforces an upper bound for Phase 1 providers.
            provider.verify(context);
            if (Instant.now().isAfter(verifyDeadline)) {
                throw new ApiException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "EXECUTION_VERIFY_TIMEOUT",
                        "Verification exceeded timeout of " + properties.getVerificationTimeoutSeconds() + "s");
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            if (remainingMs <= 0) {
                throw new ApiException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "EXECUTION_VERIFY_TIMEOUT",
                        "Verification exceeded timeout");
            }
            throw ex;
        }
    }

    private boolean abortIfCancelled(UUID executionId, AuthenticatedUser user) {
        DeploymentExecutionEntity current = transitionService.require(executionId);
        if (current.getStatus() == ExecutionStatus.CANCELLED) {
            return true;
        }
        if (!current.isCancelRequested()) {
            return false;
        }
        storageService.appendLog(
                executionId,
                ExecutionLogLevel.WARN,
                "Cancellation requested; provider cancellation is cooperative");
        transitionService.finalizeCancelled(executionId, user);
        return true;
    }

    private static void ensureDeadline(Instant deadline, String stage) {
        if (Instant.now().isAfter(deadline)) {
            throw new ApiException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "EXECUTION_TIMEOUT",
                    "Execution timed out during " + stage);
        }
    }
}
