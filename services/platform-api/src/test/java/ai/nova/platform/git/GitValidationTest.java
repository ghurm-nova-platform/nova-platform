package ai.nova.platform.git;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ai.nova.platform.git.service.GitBranchStrategy;
import ai.nova.platform.git.service.GitValidator;
import ai.nova.platform.patch.config.PatchProperties;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.patch.dto.PatchDtos.PatchStatistics;
import ai.nova.platform.patch.dto.PatchDtos.PatchValidation;
import ai.nova.platform.patch.entity.PatchStatus;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.web.error.ApiException;

class GitValidationTest {

    private static final String VALID_PATCH = """
            --- a/src/LoginService.java
            +++ b/src/LoginService.java
            @@ -1,1 +1,2 @@
             class LoginService {}
            +// validated
            """;

    private GitValidator validator;

    @BeforeEach
    void setUp() {
        validator = new GitValidator(new PatchDiffParser(new PatchProperties()), new GitBranchStrategy());
    }

    @Test
    void rejectsMissingPatch() {
        assertThatThrownBy(() -> validator.requireApprovedPatch(null))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("GIT_PATCH_NOT_APPROVED");
    }

    @Test
    void rejectsInvalidPatchStatus() {
        PatchResult patch = sample(PatchStatus.INVALID, true, VALID_PATCH);
        assertThatThrownBy(() -> validator.requireApprovedPatch(patch))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("GIT_PATCH_NOT_APPROVED");
    }

    @Test
    void rejectsFailedValidationFlag() {
        PatchResult patch = sample(PatchStatus.VALID, false, VALID_PATCH);
        assertThatThrownBy(() -> validator.requireApprovedPatch(patch))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("GIT_PATCH_VALIDATION_FAILED");
    }

    @Test
    void rejectsMalformedUnifiedDiff() {
        PatchResult patch = sample(PatchStatus.VALID, true, "not a diff");
        assertThatThrownBy(() -> validator.requireApprovedPatch(patch))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("GIT_PATCH_VALIDATION_FAILED");
    }

    @Test
    void acceptsValidApprovedPatch() {
        validator.requireApprovedPatch(sample(PatchStatus.VALID, true, VALID_PATCH));
    }

    private static PatchResult sample(PatchStatus status, boolean valid, String patch) {
        return new PatchResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "summary",
                status,
                new PatchStatistics(1, 1, 0, patch.length()),
                patch,
                List.of(),
                List.of(),
                new PatchValidation(valid, valid ? "ok" : "bad"),
                1L,
                "patch-local",
                "LOCAL",
                1L,
                java.time.Instant.now());
    }
}
