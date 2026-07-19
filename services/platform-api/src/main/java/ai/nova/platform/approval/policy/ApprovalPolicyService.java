package ai.nova.platform.approval.policy;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.approval.entity.ApprovalPolicyEntity;
import ai.nova.platform.approval.entity.ApprovalPolicyStatus;
import ai.nova.platform.approval.repository.ApprovalPolicyRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class ApprovalPolicyService {

    private final ApprovalPolicyRepository policyRepository;

    public ApprovalPolicyService(ApprovalPolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Transactional(readOnly = true)
    public ApprovalPolicyEntity requireActiveDefaultPolicy(UUID organizationId, UUID projectId) {
        ApprovalPolicyEntity policy = policyRepository
                .findFirstByOrganizationIdAndProjectIdAndIsDefaultTrueAndStatusOrderByVersionDesc(
                        organizationId, projectId, ApprovalPolicyStatus.ACTIVE)
                .or(() -> policyRepository.findFirstByOrganizationIdAndProjectIdIsNullAndIsDefaultTrueAndStatusOrderByVersionDesc(
                        organizationId, ApprovalPolicyStatus.ACTIVE))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "APPROVAL_POLICY_NOT_FOUND", "No active default approval policy found"));

        if (policy.getStatus() != ApprovalPolicyStatus.ACTIVE) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "APPROVAL_POLICY_INACTIVE", "Approval policy is not active");
        }
        return policy;
    }

    @Transactional(readOnly = true)
    public ApprovalPolicyEntity requirePolicy(UUID policyId, UUID organizationId) {
        return policyRepository
                .findByIdAndOrganizationId(policyId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "APPROVAL_POLICY_NOT_FOUND", "Approval policy not found"));
    }
}
