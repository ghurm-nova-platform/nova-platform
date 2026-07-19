package ai.nova.platform.release.service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.release.entity.ReleaseVersionEntity;
import ai.nova.platform.release.entity.VersionBump;
import ai.nova.platform.release.repository.ReleaseVersionRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class ReleaseVersionService {

    private static final Pattern SEMVER = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");

    private final ReleaseVersionRepository versionRepository;

    public ReleaseVersionService(ReleaseVersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    public record ResolvedVersion(String semanticVersion, int major, int minor, int patch, VersionBump bumpType) {
    }

    public ResolvedVersion resolve(
            java.util.UUID organizationId,
            java.util.UUID projectId,
            VersionBump bumpType,
            String explicitVersion) {
        if (explicitVersion != null && !explicitVersion.isBlank()) {
            Parsed parsed = parse(explicitVersion.trim());
            return new ResolvedVersion(format(parsed), parsed.major, parsed.minor, parsed.patch, bumpType);
        }
        VersionBump bump = bumpType != null ? bumpType : VersionBump.PATCH;
        List<ReleaseVersionEntity> existing =
                versionRepository.findByOrganizationIdAndProjectIdOrderByMajorVersionDescMinorVersionDescPatchVersionDesc(
                        organizationId, projectId);
        if (existing.isEmpty()) {
            return switch (bump) {
                case MAJOR -> new ResolvedVersion("1.0.0", 1, 0, 0, bump);
                case MINOR -> new ResolvedVersion("0.1.0", 0, 1, 0, bump);
                case PATCH -> new ResolvedVersion("0.0.1", 0, 0, 1, bump);
            };
        }
        ReleaseVersionEntity latest = existing.get(0);
        int major = latest.getMajorVersion();
        int minor = latest.getMinorVersion();
        int patch = latest.getPatchVersion();
        return switch (bump) {
            case MAJOR -> new ResolvedVersion(format(major + 1, 0, 0), major + 1, 0, 0, bump);
            case MINOR -> new ResolvedVersion(format(major, minor + 1, 0), major, minor + 1, 0, bump);
            case PATCH -> new ResolvedVersion(format(major, minor, patch + 1), major, minor, patch + 1, bump);
        };
    }

    public Parsed parse(String semanticVersion) {
        Matcher matcher = SEMVER.matcher(semanticVersion);
        if (!matcher.matches()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "RELEASE_VERSION_CONFLICT",
                    "Semantic version must be MAJOR.MINOR.PATCH: " + semanticVersion);
        }
        return new Parsed(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)));
    }

    public String format(int major, int minor, int patch) {
        return major + "." + minor + "." + patch;
    }

    private String format(Parsed parsed) {
        return format(parsed.major, parsed.minor, parsed.patch);
    }

    public record Parsed(int major, int minor, int patch) {
        @Override
        public String toString() {
            return major + "." + minor + "." + patch;
        }
    }

    public static String normalizeBump(VersionBump bump) {
        return bump == null ? null : bump.name().toLowerCase(Locale.ROOT);
    }
}
