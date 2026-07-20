package ai.nova.platform.policy.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.policy.config.PolicyProperties;
import ai.nova.platform.policy.dto.PolicyDtos.CreatePolicyRequest;
import ai.nova.platform.policy.dto.PolicyDtos.EvaluatePolicyRequest;
import ai.nova.platform.policy.dto.PolicyDtos.Policy;
import ai.nova.platform.policy.entity.EvaluationMode;
import ai.nova.platform.policy.entity.PolicyDecision;
import ai.nova.platform.policy.entity.PolicyEvaluationEntity;
import ai.nova.platform.policy.entity.PolicyEventType;
import ai.nova.platform.policy.entity.PolicyStatus;
import ai.nova.platform.policy.entity.PolicyType;
import ai.nova.platform.policy.entity.PolicyVersionEntity;
import ai.nova.platform.policy.entity.ReleasePolicyEntity;
import ai.nova.platform.policy.repository.PolicyEvaluationRepository;
import ai.nova.platform.policy.repository.ReleasePolicyRepository;
import ai.nova.platform.policy.security.PolicyAuthorizationService;
import ai.nova.platform.release.entity.ReleaseOperationEntity;
import ai.nova.platform.release.repository.ReleaseOperationRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

/**
 * Release Policies evaluate whether a Release may advance. They never modify upstream records.
 */
@Service
public class ReleasePolicyService {

    private final PolicyProperties properties;
    private final PolicyAuthorizationService authorizationService;
    private final PolicyStorageService storageService;
    private final PolicyHashService hashService;
    private final PolicyEngineService engineService;
    private final ReleasePolicyRepository policyRepository;
    private final PolicyEvaluationRepository evaluationRepository;
    private final ReleaseOperationRepository releaseOperationRepository;

    public ReleasePolicyService(
            PolicyProperties properties,
            PolicyAuthorizationService authorizationService,
            PolicyStorageService storageService,
            PolicyHashService hashService,
            PolicyEngineService engineService,
            ReleasePolicyRepository policyRepository,
            PolicyEvaluationRepository evaluationRepository,
            ReleaseOperationRepository releaseOperationRepository) {
        this.properties = properties;
        this.authorizationService = authorizationService;
        this.storageService = storageService;
        this.hashService = hashService;
        this.engineService = engineService;
        this.policyRepository = policyRepository;
        this.evaluationRepository = evaluationRepository;
        this.releaseOperationRepository = releaseOperationRepository;
    }

    @Transactional
    public Policy create(CreatePolicyRequest request, AuthenticatedUser user) {
        authorizationService.require(user, PolicyAuthorizationService.POLICY_RUN);
        requireEnabled();

        if (request.policyType() == PolicyType.CUSTOM_EXPRESSION && !properties.isAllowCustomPolicies()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "POLICY_INVALID_CONFIGURATION",
                    "Custom policies are disabled");
        }

        EvaluationMode mode = request.evaluationMode() != null ? request.evaluationMode() : EvaluationMode.ALL_REQUIRED;
        int priority = request.priority() != null ? request.priority() : 100;
        Map<String, Object> configuration = request.configuration() == null ? Map.of() : request.configuration();
        String configJson = hashService.toConfigJson(configuration);
        String fingerprint = hashService.fingerprint(
                user.getOrganizationId(),
                request.projectId(),
                request.policyName(),
                request.policyType(),
                mode,
                priority,
                configuration);

        var existing = policyRepository.findByOrganizationIdAndPolicyFingerprint(user.getOrganizationId(), fingerprint);
        if (existing.isPresent()) {
            storageService.appendEvent(
                    existing.get().getId(),
                    null,
                    PolicyEventType.IDEMPOTENT_RETURN,
                    "Identical policy already exists",
                    Instant.now());
            return storageService.toDto(existing.get());
        }

        try {
            ReleasePolicyEntity created = storageService.createPolicy(
                    user.getOrganizationId(),
                    request.projectId(),
                    request.policyName(),
                    request.description(),
                    request.policyType(),
                    priority,
                    mode,
                    configJson,
                    fingerprint,
                    user.getUserId());
            return storageService.toDto(created);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "POLICY_ALREADY_EXISTS", "A policy with the same name already exists");
        }
    }

    @Transactional
    public Policy evaluate(UUID policyId, EvaluatePolicyRequest request, AuthenticatedUser user) {
        authorizationService.require(user, PolicyAuthorizationService.POLICY_RUN);
        requireEnabled();

        ReleasePolicyEntity policy = storageService.requireForOrg(policyId, user.getOrganizationId());
        ReleaseOperationEntity release = releaseOperationRepository
                .findByIdAndOrganizationId(request.releaseId(), user.getOrganizationId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "POLICY_EVIDENCE_MISSING", "Release not found for policy evaluation"));

        if (!release.getProjectId().equals(policy.getProjectId())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "POLICY_EVALUATION_FAILED",
                    "Release project does not match policy project");
        }

        PolicyVersionEntity version = storageService.requireLatestVersion(policyId);
        Instant now = Instant.now();
        String evaluationHash = hashService.evaluationHash(
                policy.getId(),
                version.getId(),
                release.getId(),
                release.getSemanticVersion(),
                release.getManifestHash(),
                release.getContentFingerprint(),
                version.getConfigJson());

        var existing = evaluationRepository.findByOrganizationIdAndEvaluationHash(user.getOrganizationId(), evaluationHash);
        if (existing.isPresent() && existing.get().isCompleted()) {
            storageService.appendEvent(
                    policyId,
                    existing.get().getId(),
                    PolicyEventType.IDEMPOTENT_RETURN,
                    "Identical evaluation already exists",
                    now);
            return storageService.toDto(policy);
        }

        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            return recordSkippedEvaluation(
                    policy, version, release, evaluationHash, user.getUserId(), now, "Policy disabled");
        }

        storageService.appendEvent(policyId, null, PolicyEventType.EVALUATION_STARTED, "Evaluation started", now);
        PolicyEngineService.EngineResult result;
        try {
            result = engineService.evaluate(release, version, properties.isAllowCustomPolicies());
        } catch (RuntimeException ex) {
            storageService.appendEvent(policyId, null, PolicyEventType.FAILED, ex.getMessage(), Instant.now());
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "POLICY_ENGINE_ERROR", "Policy engine failed: " + ex.getMessage());
        }

        PolicyEvaluationEntity evaluation = storageService.createEvaluation(
                user.getOrganizationId(),
                policy.getProjectId(),
                policyId,
                version.getId(),
                release.getId(),
                result.decision(),
                evaluationHash,
                result.summary(),
                user.getUserId(),
                now);
        for (PolicyEngineService.EvidenceResult evidence : result.evidence()) {
            storageService.appendEvidence(
                    evaluation.getId(),
                    evidence.evidenceKey(),
                    evidence.evidenceType(),
                    evidence.referenceId(),
                    evidence.passed(),
                    evidence.detail(),
                    now);
        }
        storageService.appendEvent(
                policyId,
                evaluation.getId(),
                PolicyEventType.EVALUATION_COMPLETED,
                "Decision=" + result.decision(),
                Instant.now());

        if (result.decision() == PolicyDecision.ERROR) {
            throw new ApiException(HttpStatus.CONFLICT, "POLICY_EVALUATION_FAILED", result.summary());
        }
        return storageService.toDto(policy);
    }

    @Transactional
    public Policy enable(UUID policyId, AuthenticatedUser user) {
        authorizationService.require(user, PolicyAuthorizationService.POLICY_RUN);
        requireEnabled();
        ReleasePolicyEntity policy = storageService.requireForOrg(policyId, user.getOrganizationId());
        if (policy.getStatus() == PolicyStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "POLICY_INVALID_CONFIGURATION", "Cannot enable an archived policy");
        }
        Instant now = Instant.now();
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setUpdatedAt(now);
        policyRepository.save(policy);
        storageService.appendEvent(policyId, null, PolicyEventType.ENABLED, "Policy enabled", now);
        return storageService.toDto(policy);
    }

    @Transactional
    public Policy disable(UUID policyId, AuthenticatedUser user) {
        authorizationService.require(user, PolicyAuthorizationService.POLICY_RUN);
        requireEnabled();
        ReleasePolicyEntity policy = storageService.requireForOrg(policyId, user.getOrganizationId());
        Instant now = Instant.now();
        policy.setStatus(PolicyStatus.DISABLED);
        policy.setUpdatedAt(now);
        policyRepository.save(policy);
        storageService.appendEvent(policyId, null, PolicyEventType.DISABLED, "Policy disabled", now);
        return storageService.toDto(policy);
    }

    @Transactional(readOnly = true)
    public List<Policy> list(UUID projectId, AuthenticatedUser user) {
        authorizationService.require(user, PolicyAuthorizationService.POLICY_READ);
        requireEnabled();
        List<ReleasePolicyEntity> entities = projectId == null
                ? policyRepository.findByOrganizationIdOrderByPriorityAscCreatedAtDesc(user.getOrganizationId())
                : policyRepository.findByOrganizationIdAndProjectIdOrderByPriorityAscCreatedAtDesc(
                        user.getOrganizationId(), projectId);
        return entities.stream().map(storageService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Policy get(UUID id, AuthenticatedUser user) {
        authorizationService.require(user, PolicyAuthorizationService.POLICY_READ);
        requireEnabled();
        return storageService.toDto(storageService.requireForOrg(id, user.getOrganizationId()));
    }

    @Transactional(readOnly = true)
    public Policy history(UUID id, AuthenticatedUser user) {
        return get(id, user);
    }

    private Policy recordSkippedEvaluation(
            ReleasePolicyEntity policy,
            PolicyVersionEntity version,
            ReleaseOperationEntity release,
            String evaluationHash,
            UUID evaluatedBy,
            Instant now,
            String summary) {
        storageService.appendEvent(policy.getId(), null, PolicyEventType.EVALUATION_STARTED, "Evaluation skipped", now);
        PolicyEvaluationEntity evaluation = storageService.createEvaluation(
                policy.getOrganizationId(),
                policy.getProjectId(),
                policy.getId(),
                version.getId(),
                release.getId(),
                PolicyDecision.SKIPPED,
                evaluationHash,
                summary,
                evaluatedBy,
                now);
        evaluation.setErrorCode("POLICY_DISABLED");
        storageService.saveEvaluation(evaluation);
        storageService.appendEvidence(
                evaluation.getId(),
                "policy-disabled",
                "POLICY",
                policy.getId(),
                false,
                summary,
                now);
        storageService.appendEvent(
                policy.getId(),
                evaluation.getId(),
                PolicyEventType.EVALUATION_COMPLETED,
                "Decision=SKIPPED",
                now);
        return storageService.toDto(policy);
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE, "POLICY_DISABLED", "Release Policies are disabled");
        }
    }
}
