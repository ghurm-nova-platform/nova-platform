package ai.nova.platform.policy.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import ai.nova.platform.approval.entity.ApprovalDecisionEntity;
import ai.nova.platform.approval.entity.ApprovalDecisionValue;
import ai.nova.platform.approval.repository.ApprovalDecisionRepository;
import ai.nova.platform.ci.entity.CiObservationOperationEntity;
import ai.nova.platform.ci.entity.CiOverallStatus;
import ai.nova.platform.ci.repository.CiObservationOperationRepository;
import ai.nova.platform.deployment.entity.DeploymentOperationEntity;
import ai.nova.platform.deployment.entity.DeploymentStatus;
import ai.nova.platform.deployment.repository.DeploymentOperationRepository;
import ai.nova.platform.merge.entity.MergeOperationEntity;
import ai.nova.platform.merge.entity.MergeStatus;
import ai.nova.platform.merge.repository.MergeOperationRepository;
import ai.nova.platform.policy.entity.EvaluationMode;
import ai.nova.platform.policy.entity.PolicyDecision;
import ai.nova.platform.policy.entity.PolicyType;
import ai.nova.platform.policy.entity.PolicyVersionEntity;
import ai.nova.platform.release.entity.ReleaseContentEntity;
import ai.nova.platform.release.entity.ReleaseContentType;
import ai.nova.platform.release.entity.ReleaseOperationEntity;
import ai.nova.platform.release.entity.ReleaseStatus;
import ai.nova.platform.release.repository.ReleaseContentRepository;
import ai.nova.platform.rollback.entity.RollbackOperationEntity;
import ai.nova.platform.rollback.entity.RollbackStatus;
import ai.nova.platform.rollback.repository.RollbackOperationRepository;

/**
 * Deterministic policy engine. Consumes upstream records by reference only.
 */
@Service
public class PolicyEngineService {

    private static final Pattern SEMVER =
            Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-[0-9A-Za-z.-]+)?(?:\\+[0-9A-Za-z.-]+)?$");

    private static final Set<String> ALLOWED_EXPRESSION_TOKENS =
            Set.of("status==READY", "status==PUBLISHED", "hasManifest", "hasSemver");

    private final ReleaseContentRepository contentRepository;
    private final MergeOperationRepository mergeOperationRepository;
    private final ApprovalDecisionRepository approvalDecisionRepository;
    private final CiObservationOperationRepository ciObservationOperationRepository;
    private final DeploymentOperationRepository deploymentOperationRepository;
    private final RollbackOperationRepository rollbackOperationRepository;
    private final PolicyHashService hashService;

    public PolicyEngineService(
            ReleaseContentRepository contentRepository,
            MergeOperationRepository mergeOperationRepository,
            ApprovalDecisionRepository approvalDecisionRepository,
            CiObservationOperationRepository ciObservationOperationRepository,
            DeploymentOperationRepository deploymentOperationRepository,
            RollbackOperationRepository rollbackOperationRepository,
            PolicyHashService hashService) {
        this.contentRepository = contentRepository;
        this.mergeOperationRepository = mergeOperationRepository;
        this.approvalDecisionRepository = approvalDecisionRepository;
        this.ciObservationOperationRepository = ciObservationOperationRepository;
        this.deploymentOperationRepository = deploymentOperationRepository;
        this.rollbackOperationRepository = rollbackOperationRepository;
        this.hashService = hashService;
    }

    public EngineResult evaluate(ReleaseOperationEntity release, PolicyVersionEntity version, boolean allowCustomPolicies) {
        Map<String, Object> config = hashService.parseConfig(version.getConfigJson());
        List<EvidenceResult> evidence = new ArrayList<>();
        PolicyDecision decision =
                switch (version.getPolicyType()) {
                    case MINIMUM_APPROVALS -> checkMinimumApprovals(release, config, evidence);
                    case CI_REQUIRED -> checkCiRequired(release, evidence);
                    case NO_FAILED_CHECKS -> checkNoFailedChecks(release, evidence, version.getEvaluationMode());
                    case SIGNED_COMMITS_REQUIRED -> checkSignedCommits(release, config, evidence, version.getEvaluationMode());
                    case SEMANTIC_VERSION_REQUIRED -> checkSemanticVersion(release, evidence);
                    case MANIFEST_INTEGRITY -> checkManifestIntegrity(release, evidence);
                    case RELEASE_NOTES_REQUIRED -> checkReleaseNotes(release, config, evidence);
                    case DEPLOYMENT_OBSERVATION_EXISTS -> checkDeploymentExists(release, evidence);
                    case ROLLBACK_PLAN_EXISTS -> checkRollbackPlanExists(release, evidence);
                    case CUSTOM_EXPRESSION -> checkCustomExpression(release, config, evidence, allowCustomPolicies);
                };
        decision = applyMode(decision, version.getEvaluationMode());
        String summary = evidence.stream()
                .filter(e -> !e.passed())
                .map(EvidenceResult::detail)
                .findFirst()
                .orElse("Policy evaluation " + decision.name());
        return new EngineResult(decision, summary, evidence);
    }

    private PolicyDecision checkMinimumApprovals(
            ReleaseOperationEntity release, Map<String, Object> config, List<EvidenceResult> evidence) {
        int required = intConfig(config, "minApprovals", intConfig(config, "minimumApprovals", 1));
        List<UUID> approvalIds = refs(release.getId(), ReleaseContentType.APPROVAL_DECISION);
        int approved = 0;
        for (UUID id : approvalIds) {
            ApprovalDecisionEntity decision = approvalDecisionRepository.findById(id).orElse(null);
            boolean ok = decision != null
                    && (decision.getDecision() == ApprovalDecisionValue.APPROVED
                            || decision.getDecision() == ApprovalDecisionValue.ELIGIBLE)
                    && decision.isEligibleForMerge();
            if (ok) {
                approved++;
            }
            evidence.add(new EvidenceResult(
                    "approval:" + id,
                    "APPROVAL_DECISION",
                    id,
                    ok,
                    ok ? "Approval decision eligible" : "Approval decision missing or not eligible"));
        }
        boolean passed = approved >= required;
        evidence.add(new EvidenceResult(
                "minimum-approvals",
                "COUNT",
                null,
                passed,
                "Approved decisions=" + approved + " required=" + required));
        return passed ? PolicyDecision.PASSED : PolicyDecision.FAILED;
    }

    private PolicyDecision checkCiRequired(ReleaseOperationEntity release, List<EvidenceResult> evidence) {
        UUID ciId = resolveCiObservationId(release);
        if (ciId == null) {
            evidence.add(new EvidenceResult(
                    "ci-observation", "CI_OBSERVATION", null, false, "No CI observation referenced"));
            return PolicyDecision.FAILED;
        }
        CiObservationOperationEntity ci = ciObservationOperationRepository.findById(ciId).orElse(null);
        boolean ok = ci != null
                && ci.getOverallStatus() == CiOverallStatus.SUCCESS
                && release.getOrganizationId().equals(ci.getOrganizationId());
        evidence.add(new EvidenceResult(
                "ci-observation",
                "CI_OBSERVATION",
                ciId,
                ok,
                ok ? "CI overall status SUCCESS" : "CI observation missing or not SUCCESS"));
        return ok ? PolicyDecision.PASSED : PolicyDecision.FAILED;
    }

    private PolicyDecision checkNoFailedChecks(
            ReleaseOperationEntity release, List<EvidenceResult> evidence, EvaluationMode mode) {
        UUID ciId = resolveCiObservationId(release);
        if (ciId == null) {
            PolicyDecision noCi = mode == EvaluationMode.BEST_EFFORT ? PolicyDecision.WARNING : PolicyDecision.SKIPPED;
            evidence.add(new EvidenceResult(
                    "ci-checks",
                    "CI_OBSERVATION",
                    null,
                    noCi != PolicyDecision.FAILED,
                    "No CI observation; decision=" + noCi));
            return noCi;
        }
        CiObservationOperationEntity ci = ciObservationOperationRepository.findById(ciId).orElse(null);
        boolean failed = ci != null
                && (ci.getOverallStatus() == CiOverallStatus.FAILED
                        || ci.getOverallStatus() == CiOverallStatus.TIMED_OUT);
        evidence.add(new EvidenceResult(
                "ci-checks",
                "CI_OBSERVATION",
                ciId,
                !failed,
                failed ? "CI overall status indicates failure" : "No failed CI checks observed"));
        return failed ? PolicyDecision.FAILED : PolicyDecision.PASSED;
    }

    private PolicyDecision checkSignedCommits(
            ReleaseOperationEntity release,
            Map<String, Object> config,
            List<EvidenceResult> evidence,
            EvaluationMode mode) {
        if (boolConfig(config, "assumeSigned", false)
                || containsSignedHint(release.getDescription())) {
            evidence.add(new EvidenceResult(
                    "signed_commits",
                    "COMMIT",
                    null,
                    true,
                    "Signed commits assumed via configuration or release notes"));
            return PolicyDecision.PASSED;
        }

        List<ReleaseContentEntity> commits = contentRepository
                .findByReleaseOperationIdOrderBySortOrderAsc(release.getId())
                .stream()
                .filter(c -> c.getContentType() == ReleaseContentType.COMMIT)
                .toList();
        if (commits.isEmpty()) {
            evidence.add(new EvidenceResult(
                    "signed_commits",
                    "COMMIT",
                    null,
                    false,
                    "No commit references available for signature verification"));
            return applyMode(PolicyDecision.FAILED, mode);
        }
        evidence.add(new EvidenceResult(
                "signed_commits",
                "COMMIT",
                null,
                false,
                "Signed commit metadata is not available from upstream components"));
        return applyMode(PolicyDecision.FAILED, mode);
    }

    private PolicyDecision checkSemanticVersion(ReleaseOperationEntity release, List<EvidenceResult> evidence) {
        String version = release.getSemanticVersion();
        boolean ok = version != null && SEMVER.matcher(version.trim()).matches();
        evidence.add(new EvidenceResult(
                "semantic-version",
                "RELEASE",
                release.getId(),
                ok,
                ok ? "Semantic version valid: " + version : "Invalid or missing semantic version"));
        return ok ? PolicyDecision.PASSED : PolicyDecision.FAILED;
    }

    private PolicyDecision checkManifestIntegrity(ReleaseOperationEntity release, List<EvidenceResult> evidence) {
        boolean hashOk = release.getManifestHash() != null && !release.getManifestHash().isBlank();
        boolean statusOk = release.getStatus() == ReleaseStatus.READY
                || release.getStatus() == ReleaseStatus.PUBLISHED
                || (release.getStatus() == ReleaseStatus.PREPARING && hashOk);
        boolean ok = hashOk && statusOk;
        evidence.add(new EvidenceResult(
                "manifest-integrity",
                "RELEASE",
                release.getId(),
                ok,
                ok ? "Manifest hash present with acceptable status" : "Manifest hash missing or release status invalid"));
        return ok ? PolicyDecision.PASSED : PolicyDecision.FAILED;
    }

    private PolicyDecision checkReleaseNotes(
            ReleaseOperationEntity release, Map<String, Object> config, List<EvidenceResult> evidence) {
        boolean configNotes = boolConfig(config, "notesPresent", false);
        boolean ok = configNotes
                || (release.getDescription() != null && !release.getDescription().isBlank());
        evidence.add(new EvidenceResult(
                "release-notes",
                "RELEASE",
                release.getId(),
                ok,
                ok ? "Release notes/description present" : "Release notes/description missing"));
        return ok ? PolicyDecision.PASSED : PolicyDecision.FAILED;
    }

    private PolicyDecision checkDeploymentExists(ReleaseOperationEntity release, List<EvidenceResult> evidence) {
        List<DeploymentOperationEntity> deployments = deploymentOperationRepository
                .findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(
                        release.getOrganizationId(), release.getProjectId())
                .stream()
                .filter(d -> release.getId().equals(d.getReleaseOperationId()))
                .toList();
        boolean ok = deployments.stream()
                .anyMatch(d -> d.getStatus() == DeploymentStatus.SUCCEEDED
                        || d.getStatus() == DeploymentStatus.RUNNING
                        || d.getStatus() == DeploymentStatus.VERIFYING);
        evidence.add(new EvidenceResult(
                "deployment-observation",
                "DEPLOYMENT",
                deployments.isEmpty() ? null : deployments.get(0).getId(),
                ok,
                ok ? "Deployment observation exists for release" : "No deployment observation for release"));
        return ok ? PolicyDecision.PASSED : PolicyDecision.FAILED;
    }

    private PolicyDecision checkRollbackPlanExists(ReleaseOperationEntity release, List<EvidenceResult> evidence) {
        List<RollbackOperationEntity> rollbacks = rollbackOperationRepository
                .findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(
                        release.getOrganizationId(), release.getProjectId())
                .stream()
                .filter(r -> release.getId().equals(r.getReleaseOperationId()))
                .toList();
        boolean ok = rollbacks.stream().anyMatch(r -> r.getStatus() == RollbackStatus.READY);
        evidence.add(new EvidenceResult(
                "rollback-plan",
                "ROLLBACK",
                rollbacks.isEmpty() ? null : rollbacks.get(0).getId(),
                ok,
                ok ? "READY rollback plan exists for release" : "No READY rollback plan for release"));
        return ok ? PolicyDecision.PASSED : PolicyDecision.FAILED;
    }

    private PolicyDecision checkCustomExpression(
            ReleaseOperationEntity release,
            Map<String, Object> config,
            List<EvidenceResult> evidence,
            boolean allowCustomPolicies) {
        if (!allowCustomPolicies) {
            evidence.add(new EvidenceResult(
                    "custom-expression",
                    "CUSTOM",
                    null,
                    false,
                    "Custom policies are disabled"));
            return PolicyDecision.ERROR;
        }

        String expression = stringConfig(config, "expression", null);
        Object requiredStatus = config.get("requireStatus");
        if (expression != null && !expression.isBlank()) {
            return evaluateExpression(release, expression, evidence);
        }
        if (requiredStatus == null || requiredStatus.toString().isBlank()) {
            evidence.add(new EvidenceResult(
                    "custom-expression",
                    "CUSTOM",
                    null,
                    false,
                    "CUSTOM_EXPRESSION requires configuration.expression or requireStatus"));
            return PolicyDecision.ERROR;
        }
        String expected = requiredStatus.toString().trim().toUpperCase(Locale.ROOT);
        boolean ok = release.getStatus().name().equals(expected);
        evidence.add(new EvidenceResult(
                "custom-expression",
                "CUSTOM",
                release.getId(),
                ok,
                "Release status=" + release.getStatus() + " required=" + expected));
        return ok ? PolicyDecision.PASSED : PolicyDecision.FAILED;
    }

    private PolicyDecision evaluateExpression(
            ReleaseOperationEntity release, String expression, List<EvidenceResult> evidence) {
        String[] parts = expression.split("&&");
        StringBuilder detail = new StringBuilder();
        boolean allOk = true;
        for (String raw : parts) {
            String token = raw.trim();
            if (!ALLOWED_EXPRESSION_TOKENS.contains(token)) {
                evidence.add(new EvidenceResult(
                        "custom-expression",
                        "CUSTOM",
                        null,
                        false,
                        "Disallowed expression token: " + token));
                return PolicyDecision.ERROR;
            }
            boolean tokenOk =
                    switch (token) {
                        case "status==READY" -> release.getStatus() == ReleaseStatus.READY;
                        case "status==PUBLISHED" -> release.getStatus() == ReleaseStatus.PUBLISHED;
                        case "hasManifest" -> release.getManifestHash() != null && !release.getManifestHash().isBlank();
                        case "hasSemver" -> release.getSemanticVersion() != null
                                && SEMVER.matcher(release.getSemanticVersion().trim()).matches();
                        default -> false;
                    };
            if (!tokenOk) {
                allOk = false;
            }
            detail.append(token).append("=").append(tokenOk).append("; ");
        }
        evidence.add(new EvidenceResult(
                "custom-expression",
                "CUSTOM",
                release.getId(),
                allOk,
                detail.toString().trim()));
        return allOk ? PolicyDecision.PASSED : PolicyDecision.FAILED;
    }

    private UUID resolveCiObservationId(ReleaseOperationEntity release) {
        for (UUID approvalId : refs(release.getId(), ReleaseContentType.APPROVAL_DECISION)) {
            ApprovalDecisionEntity decision = approvalDecisionRepository.findById(approvalId).orElse(null);
            if (decision != null && decision.getCiObservationOperationId() != null) {
                return decision.getCiObservationOperationId();
            }
        }
        for (UUID mergeId : refs(release.getId(), ReleaseContentType.MERGE_OPERATION)) {
            MergeOperationEntity merge = mergeOperationRepository.findById(mergeId).orElse(null);
            if (merge != null && merge.getCiObservationOperationId() != null) {
                return merge.getCiObservationOperationId();
            }
            if (merge != null && merge.getStatus() == MergeStatus.SUCCEEDED && merge.getApprovalDecisionId() != null) {
                ApprovalDecisionEntity decision =
                        approvalDecisionRepository.findById(merge.getApprovalDecisionId()).orElse(null);
                if (decision != null && decision.getCiObservationOperationId() != null) {
                    return decision.getCiObservationOperationId();
                }
            }
        }
        return null;
    }

    private List<UUID> refs(UUID releaseId, ReleaseContentType type) {
        return contentRepository.findByReleaseOperationIdOrderBySortOrderAsc(releaseId).stream()
                .filter(c -> c.getContentType() == type && c.getReferenceId() != null)
                .map(ReleaseContentEntity::getReferenceId)
                .toList();
    }

    private static PolicyDecision applyMode(PolicyDecision decision, EvaluationMode mode) {
        if (mode == EvaluationMode.BEST_EFFORT && decision == PolicyDecision.FAILED) {
            return PolicyDecision.WARNING;
        }
        return decision;
    }

    private static boolean containsSignedHint(String description) {
        return description != null && description.toLowerCase(Locale.ROOT).contains("signed");
    }

    private static int intConfig(Map<String, Object> config, String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static boolean boolConfig(Map<String, Object> config, String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value != null) {
            return Boolean.parseBoolean(value.toString());
        }
        return defaultValue;
    }

    private static String stringConfig(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        return value == null ? defaultValue : value.toString();
    }

    public record EvidenceResult(
            String evidenceKey, String evidenceType, UUID referenceId, boolean passed, String detail) {
    }

    public record EngineResult(PolicyDecision decision, String summary, List<EvidenceResult> evidence) {
    }
}
