package ai.nova.platform.repair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.patch.config.PatchProperties;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.repair.config.RepairProperties;
import ai.nova.platform.repair.service.RepairJsonParser;
import ai.nova.platform.repair.service.RepairJsonParser.ParsedRepairOutput;
import ai.nova.platform.repair.service.RepairValidator;
import ai.nova.platform.web.error.ApiException;

class RepairValidationTest {

    static final String VALID_PATCH = """
            --- a/src/LoginService.java
            +++ b/src/LoginService.java
            @@ -1,1 +1,2 @@
             class LoginService {}
            +// validated
            """;

    private RepairJsonParser parser;
    private RepairValidator validator;

    @BeforeEach
    void setUp() {
        RepairProperties repairProperties = new RepairProperties();
        repairProperties.setMaxFiles(20);
        repairProperties.setMaxGeneratedLines(1000);
        PatchProperties patchProperties = new PatchProperties();
        parser = new RepairJsonParser(new ObjectMapper());
        validator = new RepairValidator(new PatchDiffParser(patchProperties), repairProperties);
    }

    @Test
    void rejectsInvalidJson() {
        assertThatThrownBy(() -> parser.parse("not-json"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REPAIR_INVALID_JSON");
    }

    @Test
    void rejectsEmptyPatch() {
        ParsedRepairOutput output = parser.parse("""
                {"summary":"x","patch":" ","status":"VALID","confidence":0.5}
                """);
        assertThatThrownBy(() -> validator.validate(output))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REPAIR_EMPTY");
    }

    @Test
    void rejectsTooManyFiles() {
        RepairProperties repairProperties = new RepairProperties();
        repairProperties.setMaxFiles(0);
        RepairValidator strictValidator =
                new RepairValidator(new PatchDiffParser(new PatchProperties()), repairProperties);
        ParsedRepairOutput output = parser.parse("""
                {
                  "summary":"x",
                  "patch":%s,
                  "status":"VALID",
                  "confidence":0.5
                }
                """.formatted(jsonString(VALID_PATCH)));
        assertThatThrownBy(() -> strictValidator.validate(output))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REPAIR_TOO_MANY_FILES");
    }

    @Test
    void acceptsValidUnifiedDiff() {
        ParsedRepairOutput output = parser.parse("""
                {
                  "summary":"Repair patch",
                  "reason":"Fix tests",
                  "confidence":0.9,
                  "filesChanged":1,
                  "insertions":1,
                  "deletions":0,
                  "repairedFiles":["src/LoginService.java"],
                  "patch":%s,
                  "status":"VALID"
                }
                """.formatted(jsonString(VALID_PATCH)));
        var parsed = validator.validate(output);
        assertThat(parsed.filesChanged()).isEqualTo(1);
        assertThat(parsed.insertions()).isEqualTo(1);
    }

    private static String jsonString(String value) {
        try {
            return new ObjectMapper().writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
