package ai.nova.platform.git.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.git")
public class GitProperties {

    private boolean enabled = true;
    private String workspaceRoot = "./.nova-git-workspaces";
    private String baseRef = "main";
    private String authorName = "Nova Git Agent";
    private String authorEmail = "git-agent@nova.local";
    /** Production default false. Source repos must already exist. Test fixtures may create sources explicitly. */
    private boolean allowInitRepository = false;
    /** When false (default), failed operation workspaces are preserved for diagnosis. */
    private boolean cleanupFailedWorkspaces = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWorkspaceRoot() {
        return workspaceRoot;
    }

    public void setWorkspaceRoot(String workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }

    public String getBaseRef() {
        return baseRef;
    }

    public void setBaseRef(String baseRef) {
        this.baseRef = baseRef;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public boolean isAllowInitRepository() {
        return allowInitRepository;
    }

    public void setAllowInitRepository(boolean allowInitRepository) {
        this.allowInitRepository = allowInitRepository;
    }

    public boolean isCleanupFailedWorkspaces() {
        return cleanupFailedWorkspaces;
    }

    public void setCleanupFailedWorkspaces(boolean cleanupFailedWorkspaces) {
        this.cleanupFailedWorkspaces = cleanupFailedWorkspaces;
    }
}
