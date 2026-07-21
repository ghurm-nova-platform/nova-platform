package ai.nova.platform.prreview;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.prreview.entity.ReviewCategory;
import ai.nova.platform.prreview.service.DatabaseReviewService;
import ai.nova.platform.prreview.service.DocumentationReviewService;
import ai.nova.platform.prreview.service.FindingDraft;
import ai.nova.platform.prreview.service.InfrastructureReviewService;
import ai.nova.platform.prreview.service.ReviewContext;
import ai.nova.platform.prreview.service.ReviewRiskScoreService;
import ai.nova.platform.prreview.service.TestingReviewService;

@SpringBootTest(properties = {"nova.pr-review.enabled=true"})
class AdditionalReviewServicesTest {

    @Autowired
    private DatabaseReviewService databaseReviewService;

    @Autowired
    private DocumentationReviewService documentationReviewService;

    @Autowired
    private TestingReviewService testingReviewService;

    @Autowired
    private InfrastructureReviewService infrastructureReviewService;

    @Autowired
    private ReviewRiskScoreService reviewRiskScoreService;

    @Test
    void databaseDetectsDestructiveDdl() {
        List<FindingDraft> findings = databaseReviewService.analyze("DROP TABLE users;");
        assertThat(findings).anyMatch(f -> f.category() == ReviewCategory.Database);
    }

    @Test
    void documentationFlagsCodeWithoutReadme() {
        List<FindingDraft> findings = documentationReviewService.analyze(new ReviewContext(
                "public class X {}", List.of("src/main/java/X.java"), null));
        assertThat(findings).anyMatch(f -> f.ruleCode() != null && f.ruleCode().contains("README"));
    }

    @Test
    void testingFlagsProductionWithoutTests() {
        List<FindingDraft> findings = testingReviewService.analyze(new ReviewContext(
                "public class Svc {}", List.of("src/main/java/Svc.java"), null));
        assertThat(findings).anyMatch(f -> f.category() == ReviewCategory.Testing);
    }

    @Test
    void infrastructureDetectsLatestTag() {
        List<FindingDraft> findings = infrastructureReviewService.analyze("image: nginx:latest\n");
        assertThat(findings).isNotEmpty();
    }

    @Test
    void riskScoreInvertsOverall() {
        assertThat(reviewRiskScoreService.riskScore(100)).isZero();
        assertThat(reviewRiskScoreService.riskScore(40)).isEqualTo(60);
    }
}
