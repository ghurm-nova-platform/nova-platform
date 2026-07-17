package ai.nova.platform.prompt.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import ai.nova.platform.prompt.dto.PromptDtos.PromptResponse;
import ai.nova.platform.prompt.dto.PromptDtos.PromptVariableResponse;
import ai.nova.platform.prompt.dto.PromptDtos.PromptVersionResponse;
import ai.nova.platform.prompt.entity.Prompt;
import ai.nova.platform.prompt.entity.PromptVariable;
import ai.nova.platform.prompt.entity.PromptVersion;

@Component
public class PromptMapper {

    public PromptResponse toResponse(
            Prompt prompt,
            List<String> tags,
            Integer currentDraftVersionNumber,
            Integer publishedVersionNumber) {
        return new PromptResponse(
                prompt.getId(),
                prompt.getOrganizationId(),
                prompt.getProjectId(),
                prompt.getName(),
                prompt.getDescription(),
                prompt.getPromptType(),
                prompt.getStatus(),
                tags,
                prompt.getCurrentDraftVersionId(),
                currentDraftVersionNumber,
                prompt.getPublishedVersionId(),
                publishedVersionNumber,
                prompt.getVersion(),
                prompt.getCreatedBy(),
                prompt.getUpdatedBy(),
                prompt.getCreatedAt(),
                prompt.getUpdatedAt());
    }

    public PromptVersionResponse toVersionResponse(PromptVersion version, List<PromptVariable> variables) {
        return new PromptVersionResponse(
                version.getId(),
                version.getPromptId(),
                version.getVersionNumber(),
                version.getContent(),
                version.getChangeSummary(),
                version.getStatus(),
                variables.stream().map(this::toVariableResponse).toList(),
                version.getCreatedBy(),
                version.getCreatedAt(),
                version.getPublishedBy(),
                version.getPublishedAt());
    }

    public PromptVariableResponse toVariableResponse(PromptVariable variable) {
        return new PromptVariableResponse(
                variable.getId(),
                variable.getName(),
                variable.getDescription(),
                variable.getDataType(),
                variable.isRequired(),
                variable.getDefaultValue(),
                variable.getSampleValue());
    }
}
