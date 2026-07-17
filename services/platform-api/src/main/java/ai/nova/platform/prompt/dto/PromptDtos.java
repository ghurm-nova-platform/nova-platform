package ai.nova.platform.prompt.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import ai.nova.platform.prompt.entity.PromptStatus;
import ai.nova.platform.prompt.entity.PromptType;
import ai.nova.platform.prompt.entity.PromptVariableDataType;
import ai.nova.platform.prompt.entity.PromptVersionStatus;

public final class PromptDtos {

    private PromptDtos() {
    }

    public record PromptCreateRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 2000) String description,
            @NotNull PromptType promptType,
            @NotBlank String content,
            @Size(max = 1000) String changeSummary,
            @Valid List<PromptVariableRequest> variables,
            List<@NotBlank @Size(max = 100) String> tags) {
    }

    public record PromptUpdateRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 2000) String description,
            @NotNull PromptType promptType,
            List<@NotBlank @Size(max = 100) String> tags,
            @NotNull Integer version) {
    }

    public record PromptResponse(
            UUID id,
            UUID organizationId,
            UUID projectId,
            String name,
            String description,
            PromptType promptType,
            PromptStatus status,
            List<String> tags,
            UUID currentDraftVersionId,
            Integer currentDraftVersionNumber,
            UUID publishedVersionId,
            Integer publishedVersionNumber,
            Integer version,
            UUID createdBy,
            UUID updatedBy,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record PromptVersionCreateRequest(
            @Size(max = 1000) String changeSummary) {
    }

    public record PromptVersionUpdateRequest(
            @NotBlank String content,
            @Size(max = 1000) String changeSummary,
            @Valid List<PromptVariableRequest> variables) {
    }

    public record PromptVersionResponse(
            UUID id,
            UUID promptId,
            Integer versionNumber,
            String content,
            String changeSummary,
            PromptVersionStatus status,
            List<PromptVariableResponse> variables,
            UUID createdBy,
            Instant createdAt,
            UUID publishedBy,
            Instant publishedAt) {
    }

    public record PromptVariableRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 500) String description,
            @NotNull PromptVariableDataType dataType,
            boolean required,
            String defaultValue,
            String sampleValue) {
    }

    public record PromptVariableResponse(
            UUID id,
            String name,
            String description,
            PromptVariableDataType dataType,
            boolean required,
            String defaultValue,
            String sampleValue) {
    }

    public record PromptValidateRequest(
            @NotBlank String content,
            @Valid List<PromptVariableRequest> variables) {
    }

    public record PromptValidateResponse(
            boolean valid,
            List<String> detectedVariables,
            List<String> errors,
            List<String> warnings) {
    }

    public record PromptPreviewRequest(
            @NotBlank String content,
            @Valid List<PromptVariableRequest> variables,
            Map<String, String> values) {
    }

    public record PromptPreviewResponse(
            String renderedContent,
            List<String> errors,
            List<String> warnings,
            List<String> missingRequiredVariables) {
    }

    public record PromptCompareRequest(
            @NotNull UUID leftVersionId,
            @NotNull UUID rightVersionId) {
    }

    public record PromptCompareResponse(
            UUID leftVersionId,
            UUID rightVersionId,
            String leftContent,
            String rightContent,
            List<DiffLine> diff) {
    }

    public record DiffLine(
            String type,
            int lineNumber,
            String content) {
    }

    public record PromptPublishRequest(
            @Size(max = 1000) String reason) {
    }

    public record PromptRollbackRequest(
            @NotNull UUID sourceVersionId,
            @Size(max = 1000) String reason) {
    }
}
