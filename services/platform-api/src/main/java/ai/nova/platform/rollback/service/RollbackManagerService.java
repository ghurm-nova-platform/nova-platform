package ai.nova.platform.rollback.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.deployment.entity.DeploymentEnvironmentEntity;
import ai.nova.platform.deployment.entity.DeploymentOperationEntity;
import ai.nova.platform.deployment.repository.DeploymentEnvironmentRepository;
import ai.nova.platform.deployment.repository.DeploymentOperationRepository;
import ai.nova.platform.release.entity.ReleaseOperationEntity;
import ai.nova.platform.release.entity.ReleaseStatus;
import ai.nova.platform.release.repository.ReleaseOperationRepository;
import ai.nova.platform.rollback.config.RollbackProperties;
import ai.nova.platform.rollback.dto.RollbackDtos.CreateRollbackRequest;
import ai.nova.platform.rollback.dto.RollbackDtos.Rollback;
import ai.nova.platform.rollback.entity.RollbackEventType;
import ai.nova.platform.rollback.entity.RollbackOperationEntity;
import ai.nova.platform.rollback.entity.RollbackPlanEntity;
import ai.nova.platform.rollback.entity.RollbackRiskLevel;
import ai.nova.platform.rollback.entity.RollbackStatus;
import ai.nova.platform.rollback.entity.RollbackStrategy;
import ai.nova.platform.rollback.entity.RollbackValidationResult;
import ai.nova.platform.rollback.repository.RollbackOperationRepository;
import ai.nova.platform.rollback.repository.RollbackPlanRepository;
import ai.nova.platform.rollback.security.RollbackAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

/**
 * Rollback Manager plans and validates rollbacks. Planning only — never executes rollback.
 */
@Service
public class RollbackManagerService {

    private final RollbackProperties properties;
    private final RollbackAuthorizationService authorizationService;
    private final RollbackStorageService storageService;
    private final RollbackPlanHashService planHashService;
    private final RollbackOperationRepository operationRepository;
    private final RollbackPlanRepository planRepository;
    private final ReleaseOperationRepository releaseOperationRepository;
    private final DeploymentOperationRepository deploymentOperationRepository;
    private final DeploymentEnvironmentRepository environmentRepository;

    public RollbackManagerService(
            RollbackProperties properties,
            RollbackAuthorizationService authorizationService,
            RollbackStorageService storageService,
            RollbackPlanHashService planHashService,
            RollbackOperationRepository operationRepository,
            RollbackPlanRepository planRepository,
            ReleaseOperationRepository releaseOperationRepository,
            DeploymentOperationRepository deploymentOperationRepository,
            DeploymentEnvironmentRepository environmentRepository) {
        this.properties = properties;
        this.authorizationService = authorizationService;
        this.storageService = storageService;
        this.planHashService = planHashService;
        this.operationRepository = operationRepository;
        this.planRepository = planRepository;
        this.releaseOperationRepository = releaseOperationRepository;
        this.deploymentOperationRepository = deploymentOperationRepository;
        this.environmentRepository = environmentRepository;
    }

    @Transactional
    public Rollback create(CreateRollbackRequest request, AuthenticatedUser user) {
        authorizationService.require(user, RollbackAuthorizationService.ROLLBACK_RUN);
        requireEnabled();

        ReleaseOperationEntity current = releaseOperationRepository
                .findByIdAndOrganizationId(request.releaseId(), user.getOrganizationId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "ROLLBACK_RELEASE_NOT_FOUND", "Current release not found"));

        ReleaseOperationEntity target = releaseOperationRepository
                .findByIdAndOrganizationId(request.targetReleaseId(), user.getOrganizationId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "ROLLBACK_TARGET_NOT_FOUND", "Target release not found"));

        DeploymentOperationEntity deployment = deploymentOperationRepository
                .findByIdAndOrganizationId(request.deploymentId(), user.getOrganizationId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "ROLLBACK_DEPLOYMENT_NOT_FOUND", "Deployment not found"));

        if (!deployment.getProjectId().equals(current.getProjectId())
                || !target.getProjectId().equals(current.getProjectId())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ROLLBACK_VALIDATION_FAILED",
                    "Release, target release, and deployment must belong to the same project");
        }

        String environmentCode = request.environment().trim().toUpperCase(Locale.ROOT);
        DeploymentEnvironmentEntity environment = environmentRepository
                .findById(deployment.getEnvironmentId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST, "ROLLBACK_ENVIRONMENT_MISMATCH", "Deployment environment missing"));
        if (!environment.getCode().equalsIgnoreCase(environmentCode)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ROLLBACK_ENVIRONMENT_MISMATCH",
                    "Requested environment does not match deployment environment " + environment.getCode());
        }

        RollbackRiskLevel riskLevel = request.riskLevel() != null ? request.riskLevel() : RollbackRiskLevel.MEDIUM;
        var hashResult = planHashService.build(
                user.getOrganizationId(),
                current.getProjectId(),
                current.getId(),
                deployment.getId(),
                target.getId(),
                environmentCode,
                request.strategy(),
                request.reason(),
                riskLevel,
                current.getSemanticVersion(),
                target.getSemanticVersion());

        var existing = operationRepository.findByOrganizationIdAndRollbackPlanHash(
                user.getOrganizationId(), hashResult.planHash());
        if (existing.isPresent()) {
            storageService.appendEvent(
                    existing.get().getId(),
                    RollbackEventType.IDEMPOTENT_RETURN,
                    "Identical rollback plan already exists",
                    Instant.now());
            return storageService.toDto(existing.get());
        }

        RollbackOperationEntity created = storageService.createDraft(
                user.getOrganizationId(),
                current.getProjectId(),
                current.getId(),
                deployment.getId(),
                target.getId(),
                current.getSemanticVersion(),
                target.getSemanticVersion(),
                environment.getId(),
                environmentCode,
                request.strategy(),
                request.reason(),
                riskLevel,
                hashResult.planJson(),
                hashResult.planHash(),
                user.getUserId());
        return storageService.toDto(created);
    }

    @Transactional
    public Rollback validate(UUID rollbackId, AuthenticatedUser user) {
        authorizationService.require(user, RollbackAuthorizationService.ROLLBACK_RUN);
        requireEnabled();

        RollbackOperationEntity entity = storageService.requireForOrg(rollbackId, user.getOrganizationId());
        RollbackPlanEntity plan = storageService.requirePlan(rollbackId);

        if (entity.getStatus() == RollbackStatus.READY && plan.isImmutable()) {
            return storageService.toDto(entity);
        }
        if (entity.getStatus() == RollbackStatus.CANCELLED) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "ROLLBACK_INVALID_STATUS", "Cannot validate a cancelled rollback");
        }
        if (entity.getStatus() == RollbackStatus.EXECUTING
                || entity.getStatus() == RollbackStatus.SUCCEEDED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ROLLBACK_INVALID_STATUS",
                    "Cannot validate rollback in status " + entity.getStatus());
        }
        if (plan.isImmutable() && !properties.isAllowPlanEdit()) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "ROLLBACK_INVALID_STATUS", "Rollback plan is immutable");
        }

        Instant now = Instant.now();
        entity.setStatus(RollbackStatus.VALIDATING);
        entity.setUpdatedAt(now);
        entity.setErrorCode(null);
        entity.setErrorMessage(null);
        operationRepository.save(entity);
        plan.setValidationResult(RollbackValidationResult.PENDING);
        plan.setValidationMessage(null);
        plan.setUpdatedAt(now);
        planRepository.save(plan);
        storageService.appendEvent(rollbackId, RollbackEventType.VALIDATING, "Validation started", now);

        List<CheckOutcome> outcomes = runChecks(entity, user.getOrganizationId(), now);
        boolean allPassed = outcomes.stream().allMatch(CheckOutcome::passed);
        if (!allPassed) {
            CheckOutcome firstFailure = outcomes.stream().filter(o -> !o.passed()).findFirst().orElseThrow();
            failValidation(entity, plan, firstFailure.errorCode(), firstFailure.message(), now);
        }

        entity.setStatus(RollbackStatus.READY);
        entity.setValidatedAt(now);
        entity.setUpdatedAt(now);
        operationRepository.save(entity);
        plan.setValidationResult(RollbackValidationResult.PASSED);
        plan.setValidationMessage("All validation checks passed");
        plan.setImmutable(true);
        plan.setUpdatedAt(now);
        planRepository.save(plan);
        storageService.appendEvent(rollbackId, RollbackEventType.VALIDATION_PASSED, "Validation passed", now);
        storageService.appendEvent(rollbackId, RollbackEventType.READY, "Rollback plan ready (planning only)", now);
        return storageService.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<Rollback> list(UUID projectId, AuthenticatedUser user) {
        authorizationService.require(user, RollbackAuthorizationService.ROLLBACK_READ);
        requireEnabled();
        List<RollbackOperationEntity> entities = projectId == null
                ? operationRepository.findByOrganizationIdOrderByCreatedAtDesc(user.getOrganizationId())
                : operationRepository.findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(
                        user.getOrganizationId(), projectId);
        return entities.stream().map(storageService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Rollback get(UUID id, AuthenticatedUser user) {
        authorizationService.require(user, RollbackAuthorizationService.ROLLBACK_READ);
        requireEnabled();
        return storageService.toDto(storageService.requireForOrg(id, user.getOrganizationId()));
    }

    @Transactional(readOnly = true)
    public Rollback history(UUID id, AuthenticatedUser user) {
        return get(id, user);
    }

    private List<CheckOutcome> runChecks(RollbackOperationEntity entity, UUID organizationId, Instant now) {
        List<CheckOutcome> outcomes = new ArrayList<>();

        ReleaseOperationEntity current = releaseOperationRepository
                .findByIdAndOrganizationId(entity.getReleaseOperationId(), organizationId)
                .orElse(null);
        outcomes.add(record(
                entity.getId(),
                "CURRENT_RELEASE_EXISTS",
                current != null,
                current != null ? "Current release exists" : "Current release missing",
                "ROLLBACK_RELEASE_NOT_FOUND",
                now));

        ReleaseOperationEntity target = releaseOperationRepository
                .findByIdAndOrganizationId(entity.getTargetReleaseOperationId(), organizationId)
                .orElse(null);
        boolean targetExists = target != null;
        outcomes.add(record(
                entity.getId(),
                "TARGET_RELEASE_EXISTS",
                targetExists,
                targetExists ? "Target release exists" : "Target release not found",
                "ROLLBACK_TARGET_NOT_FOUND",
                now));

        boolean targetPublished = target != null && target.getStatus() == ReleaseStatus.PUBLISHED;
        if (entity.getStrategy() == RollbackStrategy.PREVIOUS_STABLE
                || entity.getStrategy() == RollbackStrategy.PREVIOUS_RELEASE
                || entity.getStrategy() == RollbackStrategy.SPECIFIC_RELEASE
                || entity.getStrategy() == RollbackStrategy.HOTFIX_ONLY) {
            outcomes.add(record(
                    entity.getId(),
                    "TARGET_RELEASE_PUBLISHED",
                    targetPublished,
                    targetPublished
                            ? "Target release is PUBLISHED"
                            : "Target release must be PUBLISHED; was "
                                    + (target == null ? "missing" : target.getStatus()),
                    "ROLLBACK_VALIDATION_FAILED",
                    now));
        } else if (target != null) {
            boolean readyOrPublished =
                    target.getStatus() == ReleaseStatus.PUBLISHED || target.getStatus() == ReleaseStatus.READY;
            outcomes.add(record(
                    entity.getId(),
                    "TARGET_RELEASE_PUBLISHED",
                    readyOrPublished,
                    readyOrPublished
                            ? "Target release is " + target.getStatus()
                            : "Target release must be READY or PUBLISHED",
                    "ROLLBACK_VALIDATION_FAILED",
                    now));
        }

        DeploymentOperationEntity deployment = deploymentOperationRepository
                .findByIdAndOrganizationId(entity.getDeploymentOperationId(), organizationId)
                .orElse(null);
        outcomes.add(record(
                entity.getId(),
                "DEPLOYMENT_EXISTS",
                deployment != null,
                deployment != null ? "Deployment exists" : "Deployment not found",
                "ROLLBACK_DEPLOYMENT_NOT_FOUND",
                now));

        boolean envMatches = deployment != null
                && environmentRepository
                        .findById(deployment.getEnvironmentId())
                        .map(env -> env.getCode().equalsIgnoreCase(entity.getEnvironmentCode()))
                        .orElse(false);
        outcomes.add(record(
                entity.getId(),
                "ENVIRONMENT_MATCH",
                envMatches,
                envMatches
                        ? "Environment matches deployment"
                        : "Requested environment does not match deployment environment",
                "ROLLBACK_ENVIRONMENT_MISMATCH",
                now));

        boolean lineageOk = deployment != null
                && current != null
                && deployment.getReleaseOperationId().equals(current.getId());
        outcomes.add(record(
                entity.getId(),
                "RELEASE_LINEAGE",
                lineageOk,
                lineageOk
                        ? "Deployment is linked to current release"
                        : "Deployment is not associated with the current release",
                "ROLLBACK_VALIDATION_FAILED",
                now));

        boolean manifestOk = true;
        String manifestMsg = "Manifest integrity not applicable";
        if (deployment != null && current != null
                && deployment.getReleaseManifestHash() != null
                && current.getManifestHash() != null) {
            manifestOk = deployment.getReleaseManifestHash().equals(current.getManifestHash());
            manifestMsg = manifestOk ? "Manifest hash matches release" : "Deployment manifest hash mismatch";
        }
        outcomes.add(record(
                entity.getId(),
                "MANIFEST_INTEGRITY",
                manifestOk,
                manifestMsg,
                "ROLLBACK_MANIFEST_MISMATCH",
                now));

        boolean versionOk = current != null
                && target != null
                && !current.getId().equals(target.getId())
                && isVersionCompatible(entity.getStrategy(), current.getSemanticVersion(), target.getSemanticVersion());
        outcomes.add(record(
                entity.getId(),
                "VERSION_COMPATIBILITY",
                versionOk,
                versionOk
                        ? "Target version is compatible for " + entity.getStrategy()
                        : "Target version is incompatible with current version for strategy "
                                + entity.getStrategy(),
                "ROLLBACK_VERSION_INCOMPATIBLE",
                now));

        return outcomes;
    }

    private CheckOutcome record(
            UUID rollbackId,
            String checkCode,
            boolean passed,
            String message,
            String errorCode,
            Instant at) {
        storageService.appendValidation(rollbackId, checkCode, passed, message, at);
        return new CheckOutcome(passed, errorCode, message);
    }

    private void failValidation(
            RollbackOperationEntity entity,
            RollbackPlanEntity plan,
            String errorCode,
            String message,
            Instant now) {
        entity.setStatus(RollbackStatus.FAILED);
        entity.setErrorCode(errorCode);
        entity.setErrorMessage(truncate(message, 2000));
        entity.setUpdatedAt(now);
        operationRepository.save(entity);
        plan.setValidationResult(RollbackValidationResult.FAILED);
        plan.setValidationMessage(truncate(message, 2000));
        plan.setUpdatedAt(now);
        planRepository.save(plan);
        storageService.appendEvent(entity.getId(), RollbackEventType.VALIDATION_FAILED, message, now);
        storageService.appendEvent(entity.getId(), RollbackEventType.FAILED, message, now);
        throw new ApiException(HttpStatus.CONFLICT, errorCode, message);
    }

    public static boolean isVersionCompatible(RollbackStrategy strategy, String currentVersion, String targetVersion) {
        if (currentVersion == null || targetVersion == null) {
            return false;
        }
        if (currentVersion.equals(targetVersion)) {
            return false;
        }
        int cmp = compareSemVer(targetVersion, currentVersion);
        return switch (strategy) {
            case PREVIOUS_RELEASE, PREVIOUS_STABLE, HOTFIX_ONLY -> cmp < 0;
            case SPECIFIC_RELEASE, CUSTOM -> true;
        };
    }

    public static int compareSemVer(String left, String right) {
        int[] a = parseSemVer(left);
        int[] b = parseSemVer(right);
        for (int i = 0; i < 3; i++) {
            if (a[i] != b[i]) {
                return Integer.compare(a[i], b[i]);
            }
        }
        return 0;
    }

    private static int[] parseSemVer(String version) {
        String core = version.trim();
        int dash = core.indexOf('-');
        if (dash >= 0) {
            core = core.substring(0, dash);
        }
        String[] parts = core.split("\\.");
        int major = parts.length > 0 ? parsePart(parts[0]) : 0;
        int minor = parts.length > 1 ? parsePart(parts[1]) : 0;
        int patch = parts.length > 2 ? parsePart(parts[2]) : 0;
        return new int[] {major, minor, patch};
    }

    private static int parsePart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9].*", ""));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE, "ROLLBACK_DISABLED", "Rollback Manager is disabled");
        }
        if (properties.isExecutionEnabled()) {
            // Hard safety: Phase 3 must remain planning-only.
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "ROLLBACK_DISABLED",
                    "Rollback Manager must remain planning-only (execution-enabled=false)");
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private record CheckOutcome(boolean passed, String errorCode, String message) {
    }
}
