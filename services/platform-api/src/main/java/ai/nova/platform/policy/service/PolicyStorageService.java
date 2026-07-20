package ai.nova.platform.policy.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.policy.dto.PolicyDtos.EvaluationView;
import ai.nova.platform.policy.dto.PolicyDtos.EvidenceItem;
import ai.nova.platform.policy.dto.PolicyDtos.Policy;
import ai.nova.platform.policy.dto.PolicyDtos.TimelineEvent;
import ai.nova.platform.policy.dto.PolicyDtos.VersionView;
import ai.nova.platform.policy.entity.EvaluationMode;
import ai.nova.platform.policy.entity.PolicyDecision;
import ai.nova.platform.policy.entity.PolicyEvaluationEntity;
import ai.nova.platform.policy.entity.PolicyEventEntity;
import ai.nova.platform.policy.entity.PolicyEventType;
import ai.nova.platform.policy.entity.PolicyEvidenceEntity;
import ai.nova.platform.policy.entity.PolicyStatus;
import ai.nova.platform.policy.entity.PolicyType;
import ai.nova.platform.policy.entity.PolicyVersionEntity;
import ai.nova.platform.policy.entity.ReleasePolicyEntity;
import ai.nova.platform.policy.repository.PolicyEvaluationRepository;
import ai.nova.platform.policy.repository.PolicyEventRepository;
import ai.nova.platform.policy.repository.PolicyEvidenceRepository;
import ai.nova.platform.policy.repository.PolicyVersionRepository;
import ai.nova.platform.policy.repository.ReleasePolicyRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class PolicyStorageService {

    private final ReleasePolicyRepository policyRepository;
    private final PolicyVersionRepository versionRepository;
    private final PolicyEvaluationRepository evaluationRepository;
    private final PolicyEvidenceRepository evidenceRepository;
    private final PolicyEventRepository eventRepository;
    private final PolicyHashService hashService;

    public PolicyStorageService(
            ReleasePolicyRepository policyRepository,
            PolicyVersionRepository versionRepository,
            PolicyEvaluationRepository evaluationRepository,
            PolicyEvidenceRepository evidenceRepository,
            PolicyEventRepository eventRepository,
            PolicyHashService hashService) {
        this.policyRepository = policyRepository;
        this.versionRepository = versionRepository;
        this.evaluationRepository = evaluationRepository;
        this.evidenceRepository = evidenceRepository;
        this.eventRepository = eventRepository;
        this.hashService = hashService;
    }

    @Transactional
    public ReleasePolicyEntity createPolicy(
            UUID organizationId,
            UUID projectId,
            String policyName,
            String description,
            PolicyType policyType,
            int priority,
            EvaluationMode mode,
            String configJson,
            String fingerprint,
            UUID createdBy) {
        Instant now = Instant.now();
        UUID id = UUID.randomUUID();
        ReleasePolicyEntity entity = new ReleasePolicyEntity(
                id,
                organizationId,
                projectId,
                policyName.trim(),
                truncate(description, 2000),
                policyType,
                PolicyStatus.ACTIVE,
                priority,
                mode,
                configJson,
                fingerprint,
                createdBy,
                now);
        policyRepository.save(entity);
        versionRepository.save(new PolicyVersionEntity(
                UUID.randomUUID(), id, 1, policyType, mode, priority, configJson, createdBy, now));
        appendEvent(id, null, PolicyEventType.CREATED, "Policy created", now);
        appendEvent(id, null, PolicyEventType.ENABLED, "Policy enabled on create", now);
        return entity;
    }

    @Transactional
    public void appendEvent(UUID policyId, UUID evaluationId, PolicyEventType type, String detail, Instant at) {
        eventRepository.save(new PolicyEventEntity(
                UUID.randomUUID(), policyId, evaluationId, type, truncate(detail, 2000), at));
    }

    @Transactional
    public PolicyEvaluationEntity createEvaluation(
            UUID organizationId,
            UUID projectId,
            UUID policyId,
            UUID policyVersionId,
            UUID releaseId,
            PolicyDecision decision,
            String evaluationHash,
            String summary,
            UUID evaluatedBy,
            Instant at) {
        PolicyEvaluationEntity entity = new PolicyEvaluationEntity(
                UUID.randomUUID(),
                organizationId,
                projectId,
                policyId,
                policyVersionId,
                releaseId,
                decision,
                evaluationHash,
                truncate(summary, 2000),
                true,
                evaluatedBy,
                at);
        evaluationRepository.save(entity);
        return entity;
    }

    @Transactional
    public void saveEvaluation(PolicyEvaluationEntity evaluation) {
        evaluationRepository.save(evaluation);
    }

    @Transactional
    public void appendEvidence(
            UUID evaluationId,
            String evidenceKey,
            String evidenceType,
            UUID referenceId,
            boolean passed,
            String detail,
            Instant at) {
        if (evidenceRepository.findByPolicyEvaluationIdAndEvidenceKey(evaluationId, evidenceKey).isPresent()) {
            return;
        }
        evidenceRepository.save(new PolicyEvidenceEntity(
                UUID.randomUUID(),
                evaluationId,
                evidenceKey,
                evidenceType,
                referenceId,
                passed,
                truncate(detail, 2000),
                at));
    }

    public ReleasePolicyEntity requireForOrg(UUID id, UUID organizationId) {
        return policyRepository
                .findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POLICY_NOT_FOUND", "Policy not found"));
    }

    public PolicyVersionEntity requireLatestVersion(UUID policyId) {
        return versionRepository
                .findFirstByPolicyIdOrderByVersionNumberDesc(policyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POLICY_NOT_FOUND", "Policy version not found"));
    }

    public Policy toDto(ReleasePolicyEntity entity) {
        List<PolicyVersionEntity> versions = versionRepository.findByPolicyIdOrderByVersionNumberDesc(entity.getId());
        List<PolicyEventEntity> events = eventRepository.findByPolicyIdOrderByCreatedAtAsc(entity.getId());
        EvaluationView latest = evaluationRepository
                .findFirstByPolicyIdOrderByEvaluatedAtDesc(entity.getId())
                .map(this::toEvaluationView)
                .orElse(null);

        List<VersionView> versionViews = versions.stream()
                .map(v -> new VersionView(
                        v.getId(),
                        v.getVersionNumber(),
                        v.getPolicyType(),
                        v.getEvaluationMode(),
                        v.getPriority(),
                        v.getCreatedAt()))
                .toList();
        List<TimelineEvent> timeline = events.stream()
                .map(e -> new TimelineEvent(e.getEventType().name(), e.getCreatedAt(), e.getDetail()))
                .toList();
        Map<String, Object> config = hashService.parseConfig(entity.getConfigJson());

        return new Policy(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getProjectId(),
                entity.getPolicyName(),
                entity.getDescription(),
                entity.getPolicyType(),
                entity.getStatus(),
                entity.getPriority(),
                entity.getEvaluationMode(),
                config,
                entity.getPolicyFingerprint(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                latest,
                versionViews,
                timeline);
    }

    public EvaluationView toEvaluationView(PolicyEvaluationEntity evaluation) {
        List<EvidenceItem> evidence = new ArrayList<>();
        for (PolicyEvidenceEntity e :
                evidenceRepository.findByPolicyEvaluationIdOrderByCreatedAtAsc(evaluation.getId())) {
            evidence.add(new EvidenceItem(
                    e.getId(),
                    e.getEvidenceKey(),
                    e.getEvidenceType(),
                    e.getReferenceId(),
                    e.isPassed(),
                    e.getDetail(),
                    e.getCreatedAt()));
        }
        return new EvaluationView(
                evaluation.getId(),
                evaluation.getReleaseOperationId(),
                evaluation.getDecision(),
                evaluation.getEvaluationHash(),
                evaluation.getSummary(),
                evaluation.isCompleted(),
                evidence,
                evaluation.getEvaluatedAt());
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
