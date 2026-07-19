package ai.nova.platform.repair.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.patch.dto.PatchDtos.ParsedPatchOutput;
import ai.nova.platform.patch.entity.PatchStatus;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.patch.service.PatchDiffParser.ParsedDiff;
import ai.nova.platform.repair.config.RepairProperties;
import ai.nova.platform.repair.service.RepairJsonParser.ParsedRepairOutput;
import ai.nova.platform.web.error.ApiException;

@Service
public class RepairValidator {

    private final PatchDiffParser diffParser;
    private final RepairProperties properties;

    public RepairValidator(PatchDiffParser diffParser, RepairProperties properties) {
        this.diffParser = diffParser;
        this.properties = properties;
    }

    public ParsedDiff validate(ParsedRepairOutput output) {
        if (output == null) {
            throw repair("REPAIR_INVALID_JSON", "Repair output is required");
        }
        if (output.summary() == null || output.summary().isBlank()) {
            throw repair("REPAIR_MISSING_SUMMARY", "Repair summary is required");
        }
        if (output.patch() == null || output.patch().isBlank()) {
            throw repair("REPAIR_EMPTY", "Repair patch content is empty");
        }
        if (output.status() == null) {
            throw repair("REPAIR_UNKNOWN_STATUS", "Repair status is required");
        }
        if (output.status() == PatchStatus.INVALID) {
            throw repair("REPAIR_INVALID_DIFF", "Model reported INVALID repair patch status");
        }
        if (output.confidence() != null && (output.confidence() < 0 || output.confidence() > 1)) {
            throw repair("REPAIR_INVALID_CONFIDENCE", "Confidence must be between 0 and 1");
        }

        ParsedDiff parsed;
        try {
            parsed = diffParser.parseAndValidate(output.patch());
        } catch (ApiException ex) {
            throw remapPatchError(ex);
        }

        if (parsed.filesChanged() > properties.getMaxFiles()) {
            throw repair(
                    "REPAIR_TOO_MANY_FILES",
                    "Too many files in repair patch (max " + properties.getMaxFiles() + ")");
        }
        int generatedLines = parsed.insertions() + parsed.deletions();
        if (generatedLines > properties.getMaxGeneratedLines()) {
            throw repair(
                    "REPAIR_TOO_MANY_LINES",
                    "Too many generated lines in repair patch (max " + properties.getMaxGeneratedLines() + ")");
        }

        if (output.filesChanged() != null && output.filesChanged() != parsed.filesChanged()) {
            throw repair("REPAIR_STATS_MISMATCH", "filesChanged does not match parsed unified diff");
        }
        if (output.insertions() != null && output.insertions() != parsed.insertions()) {
            throw repair("REPAIR_STATS_MISMATCH", "insertions does not match parsed unified diff");
        }
        if (output.deletions() != null && output.deletions() != parsed.deletions()) {
            throw repair("REPAIR_STATS_MISMATCH", "deletions does not match parsed unified diff");
        }
        return parsed;
    }

    public ParsedDiff validatePatchOutput(ParsedPatchOutput output) {
        if (output == null) {
            throw repair("REPAIR_INVALID_JSON", "Repair output is required");
        }
        ParsedRepairOutput repair = new ParsedRepairOutput(
                output.summary(),
                output.filesChanged(),
                output.insertions(),
                output.deletions(),
                output.patch(),
                output.status(),
                null,
                null,
                List.of());
        return validate(repair);
    }

    private ApiException remapPatchError(ApiException ex) {
        String code = ex.getCode();
        if (code != null && code.startsWith("PATCH_")) {
            code = "REPAIR_" + code.substring("PATCH_".length());
        }
        return new ApiException(ex.getStatus(), code, ex.getMessage());
    }

    private static ApiException repair(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }
}
