package ai.nova.platform.pullrequest.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.pull-request")
public class PullRequestProperties {

    private boolean enabled = false;
    private String provider = "GITHUB";
    private String remoteName = "origin";
    private String targetBaseRef = "main";
    private List<String> allowedBaseRefs = new ArrayList<>(List.of("main", "develop"));
    private List<String> allowedRepositoryHosts = new ArrayList<>(List.of("github.com"));
    private boolean allowForcePush = false;
    private boolean draftByDefault = true;
    private boolean deleteBranchOnClose = false;
    private int requestTimeoutSeconds = 30;
    private String githubApiBaseUrl = "https://api.github.com";
    private String githubToken = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getRemoteName() {
        return remoteName;
    }

    public void setRemoteName(String remoteName) {
        this.remoteName = remoteName;
    }

    public String getTargetBaseRef() {
        return targetBaseRef;
    }

    public void setTargetBaseRef(String targetBaseRef) {
        this.targetBaseRef = targetBaseRef;
    }

    public List<String> getAllowedBaseRefs() {
        return allowedBaseRefs;
    }

    public void setAllowedBaseRefs(List<String> allowedBaseRefs) {
        this.allowedBaseRefs = allowedBaseRefs;
    }

    public List<String> getAllowedRepositoryHosts() {
        return allowedRepositoryHosts;
    }

    public void setAllowedRepositoryHosts(List<String> allowedRepositoryHosts) {
        this.allowedRepositoryHosts = allowedRepositoryHosts;
    }

    public boolean isAllowForcePush() {
        return allowForcePush;
    }

    public void setAllowForcePush(boolean allowForcePush) {
        this.allowForcePush = allowForcePush;
    }

    public boolean isDraftByDefault() {
        return draftByDefault;
    }

    public void setDraftByDefault(boolean draftByDefault) {
        this.draftByDefault = draftByDefault;
    }

    public boolean isDeleteBranchOnClose() {
        return deleteBranchOnClose;
    }

    public void setDeleteBranchOnClose(boolean deleteBranchOnClose) {
        this.deleteBranchOnClose = deleteBranchOnClose;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    public String getGithubApiBaseUrl() {
        return githubApiBaseUrl;
    }

    public void setGithubApiBaseUrl(String githubApiBaseUrl) {
        this.githubApiBaseUrl = githubApiBaseUrl;
    }

    public String getGithubToken() {
        return githubToken;
    }

    public void setGithubToken(String githubToken) {
        this.githubToken = githubToken;
    }
}
