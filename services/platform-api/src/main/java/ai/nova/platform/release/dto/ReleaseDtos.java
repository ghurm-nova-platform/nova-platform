package ai.nova.platform.release.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import ai.nova.platform.release.entity.ReleaseContentType;
import ai.nova.platform.release.entity.ReleaseStatus;
import ai.nova.platform.release.entity.VersionBump;
import ai.nova.platform.release.entity.VersionStrategy;

public final class ReleaseDtos {

    private ReleaseDtos() {
    }

    public record ArtifactRef(
            @NotBlank String artifactType,
            @NotBlank @Size(max = 2000) String artifactUri,
            @Size(max = 64) String artifactHash,
            @Size(max = 255) String label) {
    }

    public record CreateReleaseRequest(
            @NotNull UUID projectId,
            @NotBlank @Size(max = 255) String releaseName,
            @Size(max = 2000) String description,
            VersionBump bumpType,
            @Size(max = 64) String semanticVersion,
            List<UUID> mergeOperationIds,
            List<UUID> approvalDecisionIds,
            List<UUID> pullRequestIds,
            List<UUID> patchIds,
            List<String> commitShas,
            List<ArtifactRef> artifacts) {
    }

    public record TimelineEvent(String eventType, Instant at, String detail) {
    }

    public record ReleaseContentItem(
            UUID id,
            ReleaseContentType contentType,
            UUID referenceId,
            String commitSha,
            int sortOrder) {
    }

    public record ReleaseArtifactItem(
            UUID id,
            String artifactType,
            String artifactUri,
            String artifactHash,
            String label,
            Instant createdAt) {
    }

    public record ReleaseVersionView(
            UUID id,
            String semanticVersion,
            VersionStrategy versionStrategy,
            VersionBump bumpType,
            int majorVersion,
            int minorVersion,
            int patchVersion,
            Instant createdAt) {
    }

    public record Release(
            UUID id,
            UUID organizationId,
            UUID projectId,
            long releaseNumber,
            String semanticVersion,
            String releaseName,
            String description,
            ReleaseStatus status,
            VersionStrategy versionStrategy,
            VersionBump bumpType,
            String contentFingerprint,
            String manifestHash,
            String manifestJson,
            String errorCode,
            String errorMessage,
            UUID createdBy,
            List<ReleaseContentItem> contents,
            List<ReleaseArtifactItem> artifacts,
            ReleaseVersionView version,
            List<TimelineEvent> timeline,
            Instant preparedAt,
            Instant publishedAt,
            Instant archivedAt,
            Instant createdAt,
            Instant updatedAt) {
    }
}
