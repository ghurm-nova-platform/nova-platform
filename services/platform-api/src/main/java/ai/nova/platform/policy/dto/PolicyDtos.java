package ai.nova.platform.policy.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import ai.nova.platform.policy.entity.EvaluationMode;
import ai.nova.platform.policy.entity.PolicyDecision;
import ai.nova.platform.policy.entity.PolicyStatus;
import ai.nova.platform.policy.entity.PolicyType;

public final class PolicyDtos {

    private PolicyDtos() {
    }

    public record CreatePolicyRequest(
            @NotNull UUID projectId,
            @NotBlank @Size(max = 200) String policyName,
            @Size(max = 2000) String description,
            @NotNull PolicyType policyType,
            Integer priority,
            EvaluationMode evaluationMode,
            Map<String, Object> configuration) {
    }

    public record EvaluatePolicyRequest(@NotNull UUID releaseId) {
    }

    public record TimelineEvent(String eventType, Instant at, String detail) {
    }

    public record EvidenceItem(
            UUID id,
            String evidenceKey,
            String evidenceType,
            UUID referenceId,
            boolean passed,
            String detail,
            Instant createdAt) {
    }

    public record VersionView(
            UUID id, int versionNumber, PolicyType policyType, EvaluationMode evaluationMode, int priority, Instant createdAt) {
    }

    public record EvaluationView(
            UUID id,
            UUID releaseId,
            PolicyDecision decision,
            String evaluationHash,
            String summary,
            boolean completed,
            List<EvidenceItem> evidence,
            Instant evaluatedAt) {
    }

    public record Policy(
            UUID id,
            UUID organizationId,
            UUID projectId,
            String policyName,
            String description,
            PolicyType policyType,
            PolicyStatus status,
            int priority,
            EvaluationMode evaluationMode,
            Map<String, Object> configuration,
            String policyFingerprint,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt,
            EvaluationView latestEvaluation,
            List<VersionView> versions,
            List<TimelineEvent> timeline) {
    }
}
