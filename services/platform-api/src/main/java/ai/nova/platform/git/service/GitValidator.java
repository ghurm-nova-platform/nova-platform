package ai.nova.platform.git.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.patch.entity.PatchStatus;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.web.error.ApiException;

@Service
public class GitValidator {

    private final PatchDiffParser patchDiffParser;
    private final GitBranchStrategy branchStrategy;

    public GitValidator(PatchDiffParser patchDiffParser, GitBranchStrategy branchStrategy) {
        this.patchDiffParser = patchDiffParser;
        this.branchStrategy = branchStrategy;
    }

    public void requireApprovedPatch(PatchResult patch) {
        if (patch == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "GIT_PATCH_NOT_APPROVED", "No approved patch result found for task");
        }
        if (patch.status() != PatchStatus.VALID) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "GIT_PATCH_NOT_APPROVED", "Patch status is not VALID");
        }
        if (patch.validation() == null || !patch.validation().valid()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "GIT_PATCH_VALIDATION_FAILED", "Patch validation failed");
        }
        if (patch.patch() == null || patch.patch().isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "GIT_PATCH_VALIDATION_FAILED", "Patch content is empty");
        }
        try {
            patchDiffParser.parseAndValidate(patch.patch());
        } catch (ApiException ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "GIT_PATCH_VALIDATION_FAILED",
                    "Patch failed unified-diff validation: " + ex.getMessage());
        }
    }

    public void requireSafeBranch(String branchName, String baseRef) {
        branchStrategy.assertSafeBranchName(branchName);
        branchStrategy.assertSafeBaseRef(baseRef);
    }

    public String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "GIT_HASH_FAILED", "Failed to hash patch");
        }
    }
}
