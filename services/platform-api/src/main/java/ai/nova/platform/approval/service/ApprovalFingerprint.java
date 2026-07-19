package ai.nova.platform.approval.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.web.error.ApiException;

@Service
public class ApprovalFingerprint {

    public String evidenceFingerprint(ApprovalEvidenceBundle bundle) {
        List<String> parts = new ArrayList<>();
        parts.add("org=" + bundle.task().getOrganizationId());
        parts.add("project=" + bundle.task().getProjectId());
        parts.add("task=" + bundle.task().getId());
        parts.add("review=" + nullableId(bundle.review() != null ? bundle.review().id() : null));
        parts.add("testing=" + nullableId(bundle.testing() != null ? bundle.testing().id() : null));
        parts.add("patch=" + nullableId(bundle.patch() != null ? bundle.patch().id() : null));
        parts.add("patchHash=" + nullable(bundle.computedPatchHash()));
        parts.add("git=" + nullableId(bundle.git() != null ? bundle.git().id() : null));
        parts.add("commit=" + nullable(bundle.git() != null ? bundle.git().commitHash() : null));
        parts.add("pr=" + nullableId(bundle.pullRequest() != null ? bundle.pullRequest().id() : null));
        parts.add("prNumber=" + (bundle.pullRequest() != null && bundle.pullRequest().pullRequestNumber() != null
                ? bundle.pullRequest().pullRequestNumber()
                : ""));
        parts.add("ci=" + nullableId(bundle.ci() != null ? bundle.ci().id() : null));
        parts.add("repair=" + nullableId(bundle.repair() != null ? bundle.repair().id() : null));
        parts.sort(Comparator.naturalOrder());
        return sha256(String.join("|", parts));
    }

    public String decisionFingerprint(
            String evidenceFingerprint, int receivedApprovals, int rejectionCount, int requiredApprovals) {
        List<String> parts = List.of(
                "evidence=" + evidenceFingerprint,
                "required=" + requiredApprovals,
                "received=" + receivedApprovals,
                "rejections=" + rejectionCount);
        return sha256(String.join("|", parts));
    }

    private static String nullableId(UUID id) {
        return id == null ? "" : id.toString();
    }

    private static String nullable(String value) {
        return value == null ? "" : value;
    }

    private static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "APPROVAL_FINGERPRINT_FAILED", "Failed to compute fingerprint");
        }
    }
}
