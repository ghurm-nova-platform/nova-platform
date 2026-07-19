package ai.nova.platform.approval.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.approval.config.ApprovalGateProperties;
import ai.nova.platform.approval.dto.ApprovalDtos.ApprovalPolicyView;
import ai.nova.platform.approval.dto.ApprovalDtos.CreateApprovalPolicyRequest;
import ai.nova.platform.approval.dto.ApprovalDtos.CreateApprovalPolicyVersionRequest;
import ai.nova.platform.approval.entity.ApprovalPolicyEntity;
import ai.nova.platform.approval.entity.ApprovalPolicyStatus;
import ai.nova.platform.approval.repository.ApprovalPolicyRepository;
import ai.nova.platform.approval.security.ApprovalAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class ApprovalPolicyAdminService {

    private final ApprovalAuthorizationService authorizationService;
    private final ApprovalPolicyRepository policyRepository;
    private final ApprovalGateProperties properties;

    public ApprovalPolicyAdminService(
            ApprovalAuthorizationService authorizationService,
            ApprovalPolicyRepository policyRepository,
            ApprovalGateProperties properties) {
        this.authorizationService = authorizationService;
        this.policyRepository = policyRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<ApprovalPolicyView> list(AuthenticatedUser user) {
        authorizationService.require(user, ApprovalAuthorizationService.APPROVAL_POLICY_READ);
        return policyRepository.findByOrganizationIdOrderByNameAscVersionDesc(user.getOrganizationId()).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApprovalPolicyView get(UUID policyId, AuthenticatedUser user) {
        authorizationService.require(user, ApprovalAuthorizationService.APPROVAL_POLICY_READ);
        return toView(requirePolicy(policyId, user.getOrganizationId()));
    }

    @Transactional
    public ApprovalPolicyView create(CreateApprovalPolicyRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ApprovalAuthorizationService.APPROVAL_POLICY_MANAGE);
        Instant now = Instant.now();
        ApprovalPolicyStatus status = request.activate() ? ApprovalPolicyStatus.ACTIVE : ApprovalPolicyStatus.DRAFT;
        ApprovalPolicyEntity entity = new ApprovalPolicyEntity(
                UUID.randomUUID(),
                user.getOrganizationId(),
                request.projectId(),
                request.name(),
                request.description(),
                1,
                status,
                false,
                request.requiredHumanApprovals() != null
                        ? request.requiredHumanApprovals()
                        : properties.getDefaultRequiredHumanApprovals(),
                properties.isRequireDistinctApprovers(),
                properties.isProhibitAuthorApproval(),
                true,
                true,
                70,
                true,
                50,
                true,
                false,
                true,
                true,
                true,
                properties.getDefaultDecisionValidityMinutes(),
                user.getUserId(),
                now,
                user.getUserId(),
                now);
        return toView(policyRepository.save(entity));
    }

    @Transactional
    public ApprovalPolicyView createVersion(
            UUID policyId, CreateApprovalPolicyVersionRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ApprovalAuthorizationService.APPROVAL_POLICY_MANAGE);
        ApprovalPolicyEntity previous = requirePolicy(policyId, user.getOrganizationId());
        Instant now = Instant.now();
        if (request.activate()) {
            previous.setStatus(ApprovalPolicyStatus.SUPERSEDED);
            previous.setUpdatedAt(now);
            previous.setUpdatedBy(user.getUserId());
            policyRepository.save(previous);
        }
        ApprovalPolicyEntity entity = new ApprovalPolicyEntity(
                UUID.randomUUID(),
                previous.getOrganizationId(),
                previous.getProjectId(),
                previous.getName(),
                request.description() != null ? request.description() : previous.getDescription(),
                previous.getVersion() + 1,
                request.activate() ? ApprovalPolicyStatus.ACTIVE : ApprovalPolicyStatus.DRAFT,
                previous.isDefault(),
                previous.getRequiredHumanApprovals(),
                previous.isRequireDistinctApprovers(),
                previous.isProhibitAuthorApproval(),
                previous.isRequireCiSuccess(),
                previous.isRequireReviewApproved(),
                previous.getMinimumReviewScore(),
                previous.isRequireTestingSuccess(),
                previous.getMinimumEstimatedCoverage(),
                previous.isRequireNoCriticalFindings(),
                previous.isRequireNoHighFindings(),
                previous.isRequireRepairSuccessWhenFailed(),
                previous.isRequirePullRequestOpen(),
                previous.isRequireExactCommitMatch(),
                previous.getDecisionValidityMinutes(),
                user.getUserId(),
                now,
                user.getUserId(),
                now);
        return toView(policyRepository.save(entity));
    }

    @Transactional
    public ApprovalPolicyView activate(UUID policyId, AuthenticatedUser user) {
        authorizationService.require(user, ApprovalAuthorizationService.APPROVAL_POLICY_MANAGE);
        ApprovalPolicyEntity policy = requirePolicy(policyId, user.getOrganizationId());
        if (policy.getStatus() == ApprovalPolicyStatus.ACTIVE) {
            return toView(policy);
        }
        Instant now = Instant.now();
        policyRepository
                .findByOrganizationIdOrderByNameAscVersionDesc(user.getOrganizationId())
                .stream()
                .filter(p -> p.getName().equals(policy.getName()))
                .filter(p -> java.util.Objects.equals(p.getProjectId(), policy.getProjectId()))
                .filter(p -> p.getStatus() == ApprovalPolicyStatus.ACTIVE)
                .forEach(active -> {
                    active.setStatus(ApprovalPolicyStatus.SUPERSEDED);
                    active.setUpdatedAt(now);
                    active.setUpdatedBy(user.getUserId());
                    policyRepository.save(active);
                });
        policy.setStatus(ApprovalPolicyStatus.ACTIVE);
        policy.setUpdatedAt(now);
        policy.setUpdatedBy(user.getUserId());
        return toView(policyRepository.save(policy));
    }

    private ApprovalPolicyEntity requirePolicy(UUID policyId, UUID organizationId) {
        return policyRepository
                .findByIdAndOrganizationId(policyId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "APPROVAL_POLICY_NOT_FOUND", "Approval policy not found"));
    }

    private ApprovalPolicyView toView(ApprovalPolicyEntity entity) {
        return new ApprovalPolicyView(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getProjectId(),
                entity.getName(),
                entity.getDescription(),
                entity.getVersion(),
                entity.getStatus(),
                entity.isDefault(),
                entity.getRequiredHumanApprovals(),
                entity.isRequireDistinctApprovers(),
                entity.isProhibitAuthorApproval(),
                entity.isRequireCiSuccess(),
                entity.isRequireReviewApproved(),
                entity.getMinimumReviewScore(),
                entity.isRequireTestingSuccess(),
                entity.getMinimumEstimatedCoverage(),
                entity.isRequireNoCriticalFindings(),
                entity.isRequireNoHighFindings(),
                entity.isRequireRepairSuccessWhenFailed(),
                entity.isRequirePullRequestOpen(),
                entity.isRequireExactCommitMatch(),
                entity.getDecisionValidityMinutes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
