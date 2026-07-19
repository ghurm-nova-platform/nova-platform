package ai.nova.platform.approval.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.approval.entity.ApprovalHumanActionEntity;
import ai.nova.platform.approval.entity.ApprovalHumanActionType;

public interface ApprovalHumanActionRepository extends JpaRepository<ApprovalHumanActionEntity, UUID> {

    List<ApprovalHumanActionEntity> findByApprovalDecisionIdOrderByCreatedAtAsc(UUID approvalDecisionId);

    List<ApprovalHumanActionEntity> findByOrganizationIdAndTaskIdAndEvidenceFingerprintOrderByCreatedAtAsc(
            UUID organizationId, UUID taskId, String evidenceFingerprint);

    boolean existsByOrganizationIdAndTaskIdAndActorUserIdAndIdempotencyKey(
            UUID organizationId, UUID taskId, UUID actorUserId, String idempotencyKey);

    boolean existsByOrganizationIdAndTaskIdAndEvidenceFingerprintAndActorUserIdAndAction(
            UUID organizationId, UUID taskId, String evidenceFingerprint, UUID actorUserId, ApprovalHumanActionType action);
}
