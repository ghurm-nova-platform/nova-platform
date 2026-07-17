package ai.nova.platform.organization;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class OrganizationDtos {

    private OrganizationDtos() {
    }

    public record OrganizationRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 100) String slug) {
    }

    public record OrganizationResponse(
            UUID id,
            String name,
            String slug,
            Instant createdAt,
            Instant updatedAt,
            UUID createdBy,
            UUID updatedBy) {
    }
}
