package ai.nova.platform.prreview.service;

import java.util.List;

public record ReviewContext(String content, List<String> changedFiles, String commitSha) {

    public ReviewContext {
        changedFiles = changedFiles == null ? List.of() : List.copyOf(changedFiles);
    }

    public static ReviewContext of(String content) {
        return new ReviewContext(content, List.of(), null);
    }

    public String safeContent() {
        return content == null ? "" : content;
    }
}
