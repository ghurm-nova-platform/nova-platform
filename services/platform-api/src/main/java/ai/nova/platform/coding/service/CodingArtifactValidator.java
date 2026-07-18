package ai.nova.platform.coding.service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.coding.config.CodingProperties;
import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactDraft;
import ai.nova.platform.coding.dto.CodingDtos.ParsedCodingOutput;
import ai.nova.platform.web.error.ApiException;

@Service
public class CodingArtifactValidator {

    private final CodingProperties properties;

    public CodingArtifactValidator(CodingProperties properties) {
        this.properties = properties;
    }

    public void validate(ParsedCodingOutput output) {
        if (output == null || output.artifacts() == null || output.artifacts().isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "CODING_EMPTY_ARTIFACTS", "Coding output must include artifacts");
        }
        if (output.artifacts().size() > properties.getMaxArtifacts()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "CODING_TOO_MANY_ARTIFACTS",
                    "Too many artifacts (max " + properties.getMaxArtifacts() + ")");
        }
        Set<String> paths = new HashSet<>();
        for (GeneratedArtifactDraft artifact : output.artifacts()) {
            validateArtifact(artifact, paths);
        }
    }

    private void validateArtifact(GeneratedArtifactDraft artifact, Set<String> paths) {
        if (artifact.type() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "CODING_INVALID_ARTIFACT_TYPE", "Artifact type is required");
        }
        if (artifact.language() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "CODING_INVALID_LANGUAGE", "Artifact language is required");
        }
        String content = artifact.content();
        if (content == null || content.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "CODING_EMPTY_CONTENT", "Artifact content must not be empty");
        }
        if (content.length() > properties.getMaxContentChars()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "CODING_CONTENT_TOO_LARGE",
                    "Artifact content exceeds maximum length");
        }
        if (containsBinary(content)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "CODING_BINARY_CONTENT", "Artifact content must be text only");
        }

        String path = normalizePath(artifact.path());
        String filename = artifact.filename() == null ? "" : artifact.filename().trim();
        if (filename.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CODING_INVALID_OUTPUT", "Artifact filename is required");
        }
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "CODING_PATH_TRAVERSAL", "Artifact filename is invalid");
        }

        if (!paths.add(path.toLowerCase(Locale.ROOT))) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "CODING_DUPLICATE_PATH", "Duplicate artifact path: " + path);
        }
    }

    String normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CODING_INVALID_OUTPUT", "Artifact path is required");
        }
        String path = rawPath.trim().replace('\\', '/');
        if (path.length() > properties.getMaxPathLength()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CODING_INVALID_OUTPUT", "Artifact path is too long");
        }
        if (path.startsWith("/") || path.matches("^[A-Za-z]:.*") || path.startsWith("//")) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "CODING_ABSOLUTE_PATH", "Artifact path must be relative");
        }
        if (path.contains("\0")) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "CODING_BINARY_CONTENT", "Artifact path contains binary data");
        }
        List<String> segments = List.of(path.split("/"));
        for (String segment : segments) {
            if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "CODING_PATH_TRAVERSAL",
                        "Artifact path must not contain traversal segments");
            }
        }
        return path;
    }

    private static boolean containsBinary(String content) {
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == 0) {
                return true;
            }
            if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') {
                return true;
            }
        }
        return false;
    }
}
