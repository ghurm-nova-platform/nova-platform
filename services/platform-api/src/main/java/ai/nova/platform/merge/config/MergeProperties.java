package ai.nova.platform.merge.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import ai.nova.platform.merge.entity.MergeMethod;

@Component
@ConfigurationProperties(prefix = "nova.merge")
public class MergeProperties {

    private boolean enabled = false;
    private MergeMethod defaultMethod = MergeMethod.SQUASH;
    private boolean verifyBeforeMerge = true;
    private boolean requireProtectedBranch = true;
    private boolean allowAutoDeleteBranch = false;
    private List<String> allowedMethods = new ArrayList<>(List.of("MERGE", "SQUASH", "REBASE"));
    private String provider = "GITHUB";
    private String githubApiBaseUrl = "https://api.github.com";
    private String githubToken = "";
    private int requestTimeoutSeconds = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public MergeMethod getDefaultMethod() {
        return defaultMethod;
    }

    public void setDefaultMethod(MergeMethod defaultMethod) {
        this.defaultMethod = defaultMethod;
    }

    public void setDefaultMethod(String defaultMethod) {
        if (defaultMethod == null || defaultMethod.isBlank()) {
            return;
        }
        this.defaultMethod = MergeMethod.valueOf(defaultMethod.trim().toUpperCase(Locale.ROOT));
    }

    public boolean isVerifyBeforeMerge() {
        return verifyBeforeMerge;
    }

    public void setVerifyBeforeMerge(boolean verifyBeforeMerge) {
        this.verifyBeforeMerge = verifyBeforeMerge;
    }

    public boolean isRequireProtectedBranch() {
        return requireProtectedBranch;
    }

    public void setRequireProtectedBranch(boolean requireProtectedBranch) {
        this.requireProtectedBranch = requireProtectedBranch;
    }

    public boolean isAllowAutoDeleteBranch() {
        return allowAutoDeleteBranch;
    }

    public void setAllowAutoDeleteBranch(boolean allowAutoDeleteBranch) {
        this.allowAutoDeleteBranch = allowAutoDeleteBranch;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public void setAllowedMethods(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.isBlank()) {
            return;
        }
        this.allowedMethods = Arrays.stream(commaSeparated.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<MergeMethod> resolvedAllowedMethods() {
        List<MergeMethod> methods = new ArrayList<>();
        for (String raw : allowedMethods) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            methods.add(MergeMethod.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        }
        if (methods.isEmpty()) {
            methods.add(MergeMethod.SQUASH);
        }
        return methods;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }
}
