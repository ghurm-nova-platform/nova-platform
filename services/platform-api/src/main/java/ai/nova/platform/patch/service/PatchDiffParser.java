package ai.nova.platform.patch.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.patch.config.PatchProperties;
import ai.nova.platform.patch.entity.PatchChangeType;
import ai.nova.platform.web.error.ApiException;

/**
 * Parses and validates Git Unified Diff text without invoking git.
 */
@Service
public class PatchDiffParser {

    private static final Pattern HUNK_HEADER =
            Pattern.compile("^@@\\s+-(\\d+)(?:,(\\d+))?\\s+\\+(\\d+)(?:,(\\d+))?\\s+@@.*$");

    private final PatchProperties properties;

    public PatchDiffParser(PatchProperties properties) {
        this.properties = properties;
    }

    public ParsedDiff parseAndValidate(String patch) {
        if (patch == null || patch.isBlank()) {
            throw code("PATCH_EMPTY", "Patch content is empty");
        }
        String normalized = patch.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (normalized.isEmpty()) {
            throw code("PATCH_EMPTY", "Patch content is empty");
        }
        if (normalized.length() > properties.getMaxPatchChars()) {
            throw code(
                    "PATCH_TOO_LARGE",
                    "Patch exceeds max size of " + properties.getMaxPatchChars() + " characters");
        }

        String[] lines = normalized.split("\n", -1);
        List<FileDiff> files = new ArrayList<>();
        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            if (line.startsWith("diff --git ")) {
                i++;
                continue;
            }
            if (line.startsWith("--- ")) {
                String oldHeader = line;
                if (i + 1 >= lines.length || !lines[i + 1].startsWith("+++ ")) {
                    throw code("PATCH_MISSING_HEADERS", "Unified diff requires --- and +++ headers");
                }
                String newHeader = lines[i + 1];
                String oldPath = stripPrefix(oldHeader.substring(4).trim());
                String newPath = stripPrefix(newHeader.substring(4).trim());
                validatePath(oldPath, true);
                validatePath(newPath, true);

                i += 2;
                StringBuilder excerpt = new StringBuilder();
                excerpt.append(oldHeader).append('\n').append(newHeader).append('\n');
                int insertions = 0;
                int deletions = 0;
                boolean sawHunk = false;
                while (i < lines.length) {
                    String body = lines[i];
                    if (body.startsWith("--- ") || body.startsWith("diff --git ")) {
                        break;
                    }
                    excerpt.append(body).append('\n');
                    if (body.startsWith("@@")) {
                        Matcher matcher = HUNK_HEADER.matcher(body);
                        if (!matcher.matches()) {
                            throw code("PATCH_MALFORMED_HUNK", "Malformed hunk header: " + body);
                        }
                        sawHunk = true;
                        i++;
                        continue;
                    }
                    if (body.startsWith("+") && !body.startsWith("+++")) {
                        insertions++;
                    } else if (body.startsWith("-") && !body.startsWith("---")) {
                        deletions++;
                    } else if (!(body.startsWith(" ")
                            || body.isEmpty()
                            || body.startsWith("\\")
                            || body.startsWith("index ")
                            || body.startsWith("new file mode")
                            || body.startsWith("deleted file mode")
                            || body.startsWith("similarity index")
                            || body.startsWith("rename from")
                            || body.startsWith("rename to")
                            || body.startsWith("Binary files"))) {
                        // Allow common git metadata; reject unexpected control lines.
                        if (body.startsWith("@")) {
                            throw code("PATCH_MALFORMED_HUNK", "Malformed hunk marker: " + body);
                        }
                    }
                    i++;
                }
                if (!sawHunk && !"/dev/null".equals(oldPath) && !"/dev/null".equals(newPath)) {
                    throw code("PATCH_MALFORMED_HUNK", "File diff missing hunk headers for " + displayPath(oldPath, newPath));
                }
                PatchChangeType changeType = resolveChangeType(oldPath, newPath);
                String path = "/dev/null".equals(newPath) ? oldPath : newPath;
                files.add(new FileDiff(
                        path,
                        "/dev/null".equals(oldPath) ? null : oldPath,
                        "/dev/null".equals(newPath) ? null : newPath,
                        changeType,
                        insertions,
                        deletions,
                        excerpt.toString().trim()));
                continue;
            }
            if (line.isBlank() || line.startsWith("index ") || line.startsWith("Binary files")) {
                i++;
                continue;
            }
            throw code("PATCH_INVALID_DIFF", "Unexpected unified diff line: " + truncate(line));
        }

        if (files.isEmpty()) {
            throw code("PATCH_MISSING_HEADERS", "Patch contains no file headers (--- / +++)");
        }
        if (files.size() > properties.getMaxFiles()) {
            throw code("PATCH_TOO_MANY_FILES", "Too many files in patch (max " + properties.getMaxFiles() + ")");
        }

        int totalInsertions = files.stream().mapToInt(FileDiff::insertions).sum();
        int totalDeletions = files.stream().mapToInt(FileDiff::deletions).sum();
        return new ParsedDiff(normalized, List.copyOf(files), files.size(), totalInsertions, totalDeletions);
    }

    private void validatePath(String path, boolean allowDevNull) {
        if (path == null || path.isBlank()) {
            throw code("PATCH_INVALID_PATH", "File path is required");
        }
        if (allowDevNull && "/dev/null".equals(path)) {
            return;
        }
        if (path.indexOf('\0') >= 0) {
            throw code("PATCH_INVALID_PATH", "File path contains null byte");
        }
        if (path.length() > properties.getMaxPathLength()) {
            throw code("PATCH_INVALID_PATH", "File path exceeds max length");
        }
        String lower = path.toLowerCase(Locale.ROOT);
        if (path.startsWith("/")
                || path.startsWith("\\")
                || lower.matches("^[a-z]:\\\\.*")
                || lower.matches("^[a-z]:/.*")
                || path.contains("..")) {
            throw code("PATCH_INVALID_PATH", "Invalid file path: " + path);
        }
    }

    private static PatchChangeType resolveChangeType(String oldPath, String newPath) {
        if ("/dev/null".equals(oldPath)) {
            return PatchChangeType.ADD;
        }
        if ("/dev/null".equals(newPath)) {
            return PatchChangeType.DELETE;
        }
        if (!oldPath.equals(newPath)) {
            return PatchChangeType.RENAME;
        }
        return PatchChangeType.MODIFY;
    }

    private static String stripPrefix(String raw) {
        String path = raw.trim();
        int tab = path.indexOf('\t');
        if (tab > 0) {
            path = path.substring(0, tab).trim();
        }
        if (path.startsWith("a/") || path.startsWith("b/")) {
            return path.substring(2);
        }
        return path;
    }

    private static String displayPath(String oldPath, String newPath) {
        if ("/dev/null".equals(newPath)) {
            return oldPath;
        }
        return newPath;
    }

    private static String truncate(String value) {
        return value.length() <= 120 ? value : value.substring(0, 120) + "...";
    }

    private static ApiException code(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public record FileDiff(
            String path,
            String oldPath,
            String newPath,
            PatchChangeType changeType,
            int insertions,
            int deletions,
            String excerpt) {
    }

    public record ParsedDiff(
            String normalizedPatch, List<FileDiff> files, int filesChanged, int insertions, int deletions) {
    }
}
