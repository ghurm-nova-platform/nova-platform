package ai.nova.platform.project;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record ProjectRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 2000) String description,
            @NotNull ProjectStatus status,
            @NotNull ProjectVisibility visibility) {
    }

    public record ProjectResponse(
            UUID id,
            UUID organizationId,
            String name,
            String description,
            ProjectStatus status,
            ProjectVisibility visibility,
            Instant createdAt,
            Instant updatedAt,
            UUID createdBy,
            UUID updatedBy) {
    }
}
