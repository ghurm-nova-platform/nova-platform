package ai.nova.platform.repair.support;

import java.util.List;
import java.util.UUID;

import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactDraft;
import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.coding.entity.ArtifactLanguage;
import ai.nova.platform.coding.entity.ArtifactType;
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.patch.dto.PatchDtos.ParsedPatchOutput;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.patch.entity.PatchStatus;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.review.dto.ReviewDtos.ParsedReviewOutput;
import ai.nova.platform.review.dto.ReviewDtos.ReviewFindingDraft;
import ai.nova.platform.review.entity.ReviewCategory;
import ai.nova.platform.review.entity.ReviewSeverity;
import ai.nova.platform.review.service.ReviewStorageService;
import ai.nova.platform.security.AuthenticatedUser;

/**
 * Shared Repair Agent test fixture: seeds task, prior patch, and failed review inputs.
 */
public final class RepairTestFixture {

    public static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    public static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");

    public static final String VALID_PATCH = """
            --- a/src/LoginService.java
            +++ b/src/LoginService.java
            @@ -1,1 +1,2 @@
             class LoginService {}
            +// validated
            """;

    private RepairTestFixture() {
    }

    public static AuthenticatedUser adminUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "admin@nova.local",
                "Nova Admin",
                List.of("ORG_ADMIN"),
                List.of("REPAIR_RUN", "REPAIR_READ", "PATCH_RUN", "PATCH_READ"),
                true);
    }

    public static void seedArtifact(AgentOrchestrationTask task, ArtifactStorageService artifactStorageService) {
        artifactStorageService.replaceArtifacts(
                task,
                List.of(new GeneratedArtifactDraft(
                        ArtifactType.SOURCE_FILE,
                        ArtifactLanguage.JAVA,
                        "src/LoginService.java",
                        "LoginService.java",
                        "class LoginService {}")),
                10L,
                "coding-local",
                "LOCAL",
                5L);
    }

    public static PatchResult seedPriorPatch(
            AgentOrchestrationTask task,
            ArtifactStorageService artifactStorageService,
            PatchStorageService patchStorageService,
            PatchDiffParser patchDiffParser,
            String validPatch) {
        List<GeneratedArtifactResponse> artifacts =
                artifactStorageService.listByTask(task.getId(), task.getOrganizationId());
        ParsedPatchOutput parsed = new ParsedPatchOutput(
                "Prior patch", 1, 1, 0, validPatch, PatchStatus.VALID);
        return patchStorageService.replaceResult(
                task,
                artifacts,
                parsed,
                patchDiffParser.parseAndValidate(validPatch),
                5L,
                "patch-local",
                "LOCAL",
                3L);
    }

    public static void seedFailedReview(
            AgentOrchestrationTask task,
            ArtifactStorageService artifactStorageService,
            ReviewStorageService reviewStorageService) {
        List<GeneratedArtifactResponse> artifacts =
                artifactStorageService.listByTask(task.getId(), task.getOrganizationId());
        reviewStorageService.replaceReview(
                task,
                artifacts,
                new ParsedReviewOutput(
                        "Needs fixes",
                        42,
                        false,
                        List.of(new ReviewFindingDraft(
                                ReviewSeverity.HIGH,
                                ReviewCategory.SECURITY,
                                "Missing validation",
                                "Password check absent",
                                "Add validation before login",
                                "src/LoginService.java"))),
                2L,
                "review-local",
                "LOCAL",
                3L);
    }
}
