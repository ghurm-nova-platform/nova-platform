package ai.nova.platform.deploymentexecution.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ai.nova.platform.deployment.entity.DeploymentEnvironmentEntity;
import ai.nova.platform.deployment.repository.DeploymentEnvironmentRepository;
import ai.nova.platform.deploymentexecution.entity.ExecutionProviderCode;
import ai.nova.platform.deploymentexecution.entity.ExecutionStatus;
import ai.nova.platform.deploymentexecution.repository.DeploymentExecutionRepository;
import ai.nova.platform.environment.entity.EnvironmentStatus;
import ai.nova.platform.policy.entity.PolicyDecision;
import ai.nova.platform.policy.entity.PolicyStatus;
import ai.nova.platform.policy.entity.ReleasePolicyEntity;
import ai.nova.platform.policy.repository.PolicyEvaluationRepository;
import ai.nova.platform.policy.repository.ReleasePolicyRepository;
import ai.nova.platform.release.entity.ReleaseArtifactEntity;
import ai.nova.platform.release.entity.ReleaseContentEntity;
import ai.nova.platform.release.entity.ReleaseContentType;
import ai.nova.platform.release.entity.ReleaseOperationEntity;
import ai.nova.platform.release.entity.ReleaseStatus;
import ai.nova.platform.release.repository.ReleaseOperationRepository;
import ai.nova.platform.release.service.ReleaseManifestService;
import ai.nova.platform.release.service.ReleaseManifestService.ArtifactFingerprint;
import ai.nova.platform.release.service.ReleaseStorageService;
import ai.nova.platform.rollback.entity.RollbackOperationEntity;
import ai.nova.platform.rollback.entity.RollbackStatus;
import ai.nova.platform.rollback.repository.RollbackOperationRepository;

@Service
public class ExecutionValidationService {

    private static final EnumSet<ExecutionStatus> ACTIVE_STATUSES = EnumSet.of(
            ExecutionStatus.READY,
            ExecutionStatus.QUEUED,
            ExecutionStatus.STARTING,
            ExecutionStatus.DEPLOYING,
            ExecutionStatus.VERIFYING);

    private final ReleaseOperationRepository releaseOperationRepository;
    private final ReleasePolicyRepository policyRepository;
    private final PolicyEvaluationRepository evaluationRepository;
    private final RollbackOperationRepository rollbackOperationRepository;
    private final DeploymentEnvironmentRepository environmentRepository;
    private final DeploymentExecutionRepository executionRepository;
    private final ReleaseStorageService releaseStorageService;
    private final ReleaseManifestService manifestService;
    private final ExecutionStorageService storageService;

    public ExecutionValidationService(
            ReleaseOperationRepository releaseOperationRepository,
            ReleasePolicyRepository policyRepository,
            PolicyEvaluationRepository evaluationRepository,
            RollbackOperationRepository rollbackOperationRepository,
            DeploymentEnvironmentRepository environmentRepository,
            DeploymentExecutionRepository executionRepository,
            ReleaseStorageService releaseStorageService,
            ReleaseManifestService manifestService,
            ExecutionStorageService storageService) {
        this.releaseOperationRepository = releaseOperationRepository;
        this.policyRepository = policyRepository;
        this.evaluationRepository = evaluationRepository;
        this.rollbackOperationRepository = rollbackOperationRepository;
        this.environmentRepository = environmentRepository;
        this.executionRepository = executionRepository;
        this.releaseStorageService = releaseStorageService;
        this.manifestService = manifestService;
        this.storageService = storageService;
    }

    public ValidationOutcome validateForCreate(
            UUID organizationId, UUID releaseId, UUID environmentId, ExecutionProviderCode provider) {
        Instant now = Instant.now();
        List<CheckOutcome> outcomes = new ArrayList<>();

        ReleaseOperationEntity release = releaseOperationRepository
                .findByIdAndOrganizationId(releaseId, organizationId)
                .orElse(null);
        outcomes.add(check(
                "RELEASE_EXISTS",
                release != null,
                release != null ? "Release exists" : "Release not found",
                "EXECUTION_RELEASE_NOT_FOUND"));

        boolean published = release != null && release.getStatus() == ReleaseStatus.PUBLISHED;
        outcomes.add(check(
                "RELEASE_PUBLISHED",
                published,
                published
                        ? "Release is PUBLISHED"
                        : "Release must be PUBLISHED for execution; was "
                                + (release == null ? "missing" : release.getStatus()),
                "EXECUTION_VALIDATION_FAILED"));

        outcomes.addAll(checkPolicies(organizationId, release));

        outcomes.add(checkRollbackPlan(release));

        DeploymentEnvironmentEntity environment = environmentRepository.findById(environmentId).orElse(null);
        outcomes.add(check(
                "ENVIRONMENT_EXISTS",
                environment != null,
                environment != null ? "Environment exists" : "Environment not found",
                "EXECUTION_ENVIRONMENT_NOT_FOUND"));

        boolean noActive = !executionRepository.existsByOrganizationIdAndEnvironmentIdAndStatusIn(
                organizationId, environmentId, ACTIVE_STATUSES);
        outcomes.add(check(
                "NO_ACTIVE_EXECUTION",
                noActive,
                noActive ? "No active execution on environment" : "Another execution is active on this environment",
                "EXECUTION_CONCURRENCY_BLOCKED"));

        boolean envManaged = environment != null
                && environment.isActive()
                && (environment.getOrganizationId() == null
                        || (release != null
                                && organizationId.equals(environment.getOrganizationId())
                                && release.getProjectId().equals(environment.getProjectId())));
        outcomes.add(check(
                "ENVIRONMENT_MANAGED",
                envManaged,
                envManaged ? "Environment is managed and accessible" : "Environment is not managed for this org/project",
                "EXECUTION_ENVIRONMENT_NOT_MANAGED"));

        boolean envActive = environment != null && environment.getStatus() == EnvironmentStatus.ACTIVE;
        outcomes.add(check(
                "ENVIRONMENT_ACTIVE",
                envActive,
                envActive
                        ? "Environment is ACTIVE"
                        : "Environment must be ACTIVE; was "
                                + (environment == null ? "missing" : environment.getStatus()),
                "EXECUTION_ENVIRONMENT_INACTIVE"));

        outcomes.add(checkManifest(release));
        outcomes.add(checkContentFingerprint(release));

        // Prefer concurrency as the reported failure when multiple checks fail after an active execution exists.
        CheckOutcome concurrencyFailure = outcomes.stream()
                .filter(o -> "NO_ACTIVE_EXECUTION".equals(o.checkCode()) && !o.passed())
                .findFirst()
                .orElse(null);
        CheckOutcome firstFailure = concurrencyFailure != null
                ? concurrencyFailure
                : outcomes.stream().filter(o -> !o.passed()).findFirst().orElse(null);
        if (firstFailure != null) {
            return ValidationOutcome.failed(firstFailure.errorCode(), firstFailure.message(), outcomes, release, environment);
        }
        return ValidationOutcome.passed(outcomes, release, environment);
    }

    public void persistChecks(UUID executionId, List<CheckOutcome> outcomes) {
        Instant now = Instant.now();
        for (CheckOutcome outcome : outcomes) {
            storageService.appendValidation(executionId, outcome.checkCode(), outcome.passed(), outcome.message(), now);
        }
    }

    private List<CheckOutcome> checkPolicies(UUID organizationId, ReleaseOperationEntity release) {
        List<CheckOutcome> outcomes = new ArrayList<>();
        if (release == null) {
            outcomes.add(check(
                    "POLICY_EVALUATION", false, "Release missing for policy evaluation", "EXECUTION_POLICY_FAILED"));
            return outcomes;
        }
        List<ReleasePolicyEntity> activePolicies = policyRepository
                .findByOrganizationIdAndProjectIdAndStatusOrderByPriorityAscCreatedAtDesc(
                        organizationId, release.getProjectId(), PolicyStatus.ACTIVE);
        if (activePolicies.isEmpty()) {
            outcomes.add(check("POLICY_EVALUATION", true, "No ACTIVE policies configured", "EXECUTION_POLICY_FAILED"));
            return outcomes;
        }
        for (ReleasePolicyEntity policy : activePolicies) {
            var evaluation = evaluationRepository.findFirstByPolicyIdAndReleaseOperationIdOrderByEvaluatedAtDesc(
                    policy.getId(), release.getId());
            boolean passed = evaluation.isPresent() && evaluation.get().getDecision() == PolicyDecision.PASSED;
            outcomes.add(check(
                    "POLICY_" + policy.getPolicyName(),
                    passed,
                    passed
                            ? "Policy evaluation PASSED for " + policy.getPolicyName()
                            : "Policy evaluation missing or not PASSED for " + policy.getPolicyName(),
                    "EXECUTION_POLICY_FAILED"));
        }
        return outcomes;
    }

    private CheckOutcome checkRollbackPlan(ReleaseOperationEntity release) {
        if (release == null) {
            return check(
                    "ROLLBACK_PLAN_READY", false, "Release missing for rollback plan check", "EXECUTION_ROLLBACK_PLAN_MISSING");
        }
        List<RollbackOperationEntity> rollbacks = rollbackOperationRepository
                .findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(
                        release.getOrganizationId(), release.getProjectId())
                .stream()
                .filter(r -> release.getId().equals(r.getReleaseOperationId()))
                .toList();
        boolean ok = rollbacks.stream().anyMatch(r -> r.getStatus() == RollbackStatus.READY);
        return check(
                "ROLLBACK_PLAN_READY",
                ok,
                ok ? "READY rollback plan exists for release" : "No READY rollback plan for release",
                "EXECUTION_ROLLBACK_PLAN_MISSING");
    }

    private CheckOutcome checkManifest(ReleaseOperationEntity release) {
        if (release == null || release.getManifestHash() == null) {
            return check("MANIFEST_HASH", false, "Release manifest hash missing", "EXECUTION_MANIFEST_MISMATCH");
        }
        var manifest = manifestService.build(
                release,
                releaseStorageService.contents(release.getId()),
                releaseStorageService.artifacts(release.getId()));
        boolean ok = release.getManifestHash().equals(manifest.manifestHash());
        return check(
                "MANIFEST_HASH",
                ok,
                ok ? "Manifest hash matches recomputed value" : "Manifest hash mismatch",
                "EXECUTION_MANIFEST_MISMATCH");
    }

    private CheckOutcome checkContentFingerprint(ReleaseOperationEntity release) {
        if (release == null || release.getContentFingerprint() == null) {
            return check(
                    "CONTENT_FINGERPRINT", false, "Release content fingerprint missing", "EXECUTION_FINGERPRINT_MISMATCH");
        }
        List<ReleaseContentEntity> contents = releaseStorageService.contents(release.getId());
        List<ReleaseArtifactEntity> artifacts = releaseStorageService.artifacts(release.getId());
        String recomputed = manifestService.contentFingerprint(
                refs(contents, ReleaseContentType.MERGE_OPERATION),
                refs(contents, ReleaseContentType.APPROVAL_DECISION),
                refs(contents, ReleaseContentType.PULL_REQUEST),
                refs(contents, ReleaseContentType.PATCH),
                contents.stream()
                        .filter(c -> c.getContentType() == ReleaseContentType.COMMIT)
                        .map(ReleaseContentEntity::getCommitSha)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()),
                artifacts.stream()
                        .map(a -> new ArtifactFingerprint(a.getArtifactType(), a.getArtifactUri(), a.getArtifactHash()))
                        .toList());
        boolean ok = release.getContentFingerprint().equals(recomputed);
        return check(
                "CONTENT_FINGERPRINT",
                ok,
                ok ? "Content fingerprint matches release record" : "Content fingerprint mismatch",
                "EXECUTION_FINGERPRINT_MISMATCH");
    }

    private static List<UUID> refs(List<ReleaseContentEntity> contents, ReleaseContentType type) {
        return contents.stream()
                .filter(c -> c.getContentType() == type)
                .map(ReleaseContentEntity::getReferenceId)
                .filter(Objects::nonNull)
                .toList();
    }

    private static CheckOutcome check(String checkCode, boolean passed, String message, String errorCode) {
        return new CheckOutcome(checkCode, passed, message, errorCode);
    }

    public record CheckOutcome(String checkCode, boolean passed, String message, String errorCode) {
    }

    public record ValidationOutcome(
            boolean passed,
            String errorCode,
            String message,
            List<CheckOutcome> checks,
            ReleaseOperationEntity release,
            DeploymentEnvironmentEntity environment) {

        public static ValidationOutcome passed(
                List<CheckOutcome> checks, ReleaseOperationEntity release, DeploymentEnvironmentEntity environment) {
            return new ValidationOutcome(true, null, null, checks, release, environment);
        }

        public static ValidationOutcome failed(
                String errorCode,
                String message,
                List<CheckOutcome> checks,
                ReleaseOperationEntity release,
                DeploymentEnvironmentEntity environment) {
            return new ValidationOutcome(false, errorCode, message, checks, release, environment);
        }
    }
}
