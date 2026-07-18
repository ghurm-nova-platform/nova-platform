package ai.nova.platform.patch.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.patch.dto.PatchDtos.ParsedPatchOutput;
import ai.nova.platform.patch.entity.PatchStatus;
import ai.nova.platform.patch.service.PatchDiffParser.ParsedDiff;
import ai.nova.platform.web.error.ApiException;

@Service
public class PatchValidator {

    private final PatchDiffParser diffParser;

    public PatchValidator(PatchDiffParser diffParser) {
        this.diffParser = diffParser;
    }

    public ParsedDiff validate(ParsedPatchOutput output) {
        if (output == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PATCH_INVALID_JSON", "Patch output is required");
        }
        if (output.summary() == null || output.summary().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PATCH_MISSING_SUMMARY", "Patch summary is required");
        }
        if (output.patch() == null || output.patch().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PATCH_EMPTY", "Patch content is empty");
        }
        if (output.status() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PATCH_UNKNOWN_STATUS", "Patch status is required");
        }
        if (output.status() == PatchStatus.INVALID) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "PATCH_INVALID_DIFF", "Model reported INVALID patch status");
        }
        ParsedDiff parsed = diffParser.parseAndValidate(output.patch());
        if (output.filesChanged() != null && output.filesChanged() != parsed.filesChanged()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PATCH_STATS_MISMATCH",
                    "filesChanged does not match parsed unified diff");
        }
        if (output.insertions() != null && output.insertions() != parsed.insertions()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PATCH_STATS_MISMATCH",
                    "insertions does not match parsed unified diff");
        }
        if (output.deletions() != null && output.deletions() != parsed.deletions()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PATCH_STATS_MISMATCH",
                    "deletions does not match parsed unified diff");
        }
        return parsed;
    }
}
