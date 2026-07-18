package ai.nova.platform.patch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.patch.config.PatchProperties;
import ai.nova.platform.patch.dto.PatchDtos.ParsedPatchOutput;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.patch.service.PatchJsonParser;
import ai.nova.platform.patch.service.PatchValidator;
import ai.nova.platform.web.error.ApiException;

class PatchValidationTest {

    static final String VALID_PATCH = """
            --- a/src/LoginService.java
            +++ b/src/LoginService.java
            @@ -1,1 +1,2 @@
             class LoginService {}
            +// validated
            """;

    private PatchJsonParser parser;
    private PatchValidator validator;

    @BeforeEach
    void setUp() {
        PatchProperties properties = new PatchProperties();
        parser = new PatchJsonParser(new ObjectMapper());
        validator = new PatchValidator(new PatchDiffParser(properties));
    }

    @Test
    void rejectsInvalidJson() {
        assertThatThrownBy(() -> parser.parse("not-json"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PATCH_INVALID_JSON");
    }

    @Test
    void rejectsEmptyPatch() {
        ParsedPatchOutput output = parser.parse("""
                {"summary":"x","patch":" ","status":"VALID"}
                """);
        assertThatThrownBy(() -> validator.validate(output))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PATCH_EMPTY");
    }

    @Test
    void rejectsMissingHeaders() {
        ParsedPatchOutput output = parser.parse("""
                {"summary":"x","patch":"@@ -1,1 +1,1 @@\\n context","status":"VALID"}
                """);
        assertThatThrownBy(() -> validator.validate(output))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isIn("PATCH_MISSING_HEADERS", "PATCH_INVALID_DIFF");
    }

    @Test
    void rejectsMalformedHunks() {
        ParsedPatchOutput output = parser.parse("""
                {
                  "summary":"x",
                  "patch":"--- a/src/A.java\\n+++ b/src/A.java\\n@@ broken @@\\n+line\\n",
                  "status":"VALID"
                }
                """);
        assertThatThrownBy(() -> validator.validate(output))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PATCH_MALFORMED_HUNK");
    }

    @Test
    void rejectsInvalidPaths() {
        ParsedPatchOutput output = parser.parse("""
                {
                  "summary":"x",
                  "patch":"--- a/../secret.java\\n+++ b/../secret.java\\n@@ -1,0 +1,1 @@\\n+x\\n",
                  "status":"VALID"
                }
                """);
        assertThatThrownBy(() -> validator.validate(output))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PATCH_INVALID_PATH");
    }

    @Test
    void acceptsValidUnifiedDiff() {
        ParsedPatchOutput output = parser.parse("""
                {
                  "summary":"Generated patch",
                  "filesChanged":1,
                  "insertions":1,
                  "deletions":0,
                  "patch":%s,
                  "status":"VALID"
                }
                """.formatted(jsonString(VALID_PATCH)));
        var parsed = validator.validate(output);
        assertThat(parsed.filesChanged()).isEqualTo(1);
        assertThat(parsed.insertions()).isEqualTo(1);
        assertThat(parsed.deletions()).isEqualTo(0);
    }

    private static String jsonString(String value) {
        try {
            return new ObjectMapper().writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
