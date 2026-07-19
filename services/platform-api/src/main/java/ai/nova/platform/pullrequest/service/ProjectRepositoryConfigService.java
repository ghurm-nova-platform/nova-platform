package ai.nova.platform.pullrequest.service;

import java.net.URI;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.pullrequest.config.PullRequestProperties;
import ai.nova.platform.pullrequest.entity.ProjectRepositoryConfigEntity;
import ai.nova.platform.pullrequest.provider.PullRequestProviderRegistry;
import ai.nova.platform.pullrequest.provider.RepositoryRef;
import ai.nova.platform.pullrequest.repository.ProjectRepositoryConfigRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class ProjectRepositoryConfigService {

    public record ResolvedRepositoryConfig(
            ProjectRepositoryConfigEntity entity,
            RepositoryRef repositoryRef,
            String effectiveProvider,
            String remoteUrl,
            String remoteName,
            String targetBranch,
            String tokenOrNull,
            boolean fileRemote) {
    }

    private final ProjectRepositoryConfigRepository configRepository;
    private final PullRequestProperties properties;
    private final PullRequestProviderRegistry providerRegistry;
    private final PullRequestRemoteGitService remoteGitService;

    public ProjectRepositoryConfigService(
            ProjectRepositoryConfigRepository configRepository,
            PullRequestProperties properties,
            PullRequestProviderRegistry providerRegistry,
            PullRequestRemoteGitService remoteGitService) {
        this.configRepository = configRepository;
        this.properties = properties;
        this.providerRegistry = providerRegistry;
        this.remoteGitService = remoteGitService;
    }

    public ResolvedRepositoryConfig resolve(java.util.UUID organizationId, java.util.UUID projectId) {
        ProjectRepositoryConfigEntity entity = configRepository
                .findByOrganizationIdAndProjectId(organizationId, projectId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST, "PR_REMOTE_NOT_CONFIGURED", "Project repository is not configured"));

        if (!entity.isEnabled()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "PR_REMOTE_NOT_CONFIGURED", "Project repository configuration is disabled");
        }

        String remoteUrl = entity.getRemoteUrl();
        boolean fileRemote = remoteGitService.isFileRemote(remoteUrl);
        validateHost(entity.getRepositoryHost(), remoteUrl, fileRemote);

        String effectiveProvider = resolveEffectiveProvider(entity.getProvider());
        if (!providerRegistry.supports(effectiveProvider)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "PR_PROVIDER_UNSUPPORTED", "Provider is not supported: " + effectiveProvider);
        }

        String targetBranch = entity.getTargetBaseRef();
        if (targetBranch == null || targetBranch.isBlank()) {
            targetBranch = properties.getTargetBaseRef();
        }
        validateTargetBranch(targetBranch);

        String token = resolveToken(fileRemote);
        RepositoryRef repositoryRef =
                new RepositoryRef(entity.getRepositoryHost(), entity.getRepositoryOwner(), entity.getRepositoryName());

        return new ResolvedRepositoryConfig(
                entity,
                repositoryRef,
                effectiveProvider,
                remoteUrl,
                properties.getRemoteName(),
                targetBranch,
                token,
                fileRemote);
    }

    private String resolveEffectiveProvider(String configuredProvider) {
        String configured = configuredProvider == null ? "" : configuredProvider.trim().toUpperCase(Locale.ROOT);
        String configuredProperty = properties.getProvider() == null
                ? "GITHUB"
                : properties.getProvider().trim().toUpperCase(Locale.ROOT);
        if ("LOCAL".equals(configuredProperty)) {
            return "LOCAL";
        }
        return configured.isBlank() ? configuredProperty : configured;
    }

    private void validateHost(String configuredHost, String remoteUrl, boolean fileRemote) {
        if (fileRemote) {
            return;
        }
        String resolvedHost = configuredHost == null ? "" : configuredHost.trim().toLowerCase(Locale.ROOT);
        if (resolvedHost.isBlank()) {
            resolvedHost = extractHost(remoteUrl);
        }
        if (resolvedHost.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PR_REMOTE_NOT_ALLOWED", "Repository host is missing");
        }
        final String host = resolvedHost;
        boolean allowed = properties.getAllowedRepositoryHosts().stream()
                .map(value -> value == null ? "" : value.trim().toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.equals(host));
        if (!allowed) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "PR_REMOTE_NOT_ALLOWED", "Repository host is not allowlisted: " + host);
        }
    }

    private void validateTargetBranch(String targetBranch) {
        String normalized = targetBranch.trim();
        boolean allowed = properties.getAllowedBaseRefs().stream()
                .map(value -> value == null ? "" : value.trim())
                .anyMatch(value -> value.equals(normalized));
        if (!allowed) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PR_TARGET_BRANCH_NOT_ALLOWED",
                    "Target base branch is not allowlisted: " + normalized);
        }
    }

    private String resolveToken(boolean fileRemote) {
        if (fileRemote) {
            return null;
        }
        String token = properties.getGithubToken();
        if (token == null || token.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "PR_CREDENTIALS_MISSING", "Remote credentials are not configured");
        }
        return token.trim();
    }

    private static String extractHost(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(remoteUrl.trim());
            if (uri.getHost() != null) {
                return uri.getHost().toLowerCase(Locale.ROOT);
            }
        } catch (IllegalArgumentException ignored) {
            // fall through
        }
        return "";
    }
}
