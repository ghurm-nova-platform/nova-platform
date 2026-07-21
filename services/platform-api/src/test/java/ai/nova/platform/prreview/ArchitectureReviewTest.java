package ai.nova.platform.prreview;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.prreview.entity.ReviewCategory;
import ai.nova.platform.prreview.entity.ReviewSeverity;
import ai.nova.platform.prreview.service.ArchitectureReviewService;
import ai.nova.platform.prreview.service.FindingDraft;

@SpringBootTest(properties = {"nova.pr-review.enabled=true"})
class ArchitectureReviewTest {

    @Autowired
    private ArchitectureReviewService architectureReviewService;

    @Test
    void detectsControllerRepositoryLayerSkip() {
        String diff = """
                @RestController
                class OrderController {
                    private final OrderRepository repository;
                }
                """;
        List<FindingDraft> findings = architectureReviewService.analyze(diff);
        assertThat(findings)
                .anyMatch(f -> f.category() == ReviewCategory.Architecture
                        && f.severity() == ReviewSeverity.WARNING
                        && f.title().toLowerCase().contains("layer skip"));
    }

    @Test
    void detectsMixedCasePackage() {
        List<FindingDraft> findings = architectureReviewService.analyze("package ai.Nova.Sample;\n");
        assertThat(findings).anyMatch(f -> f.title().toLowerCase().contains("mixed-case"));
    }
}
