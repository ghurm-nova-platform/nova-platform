package ai.nova.platform.prreview;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.prreview.entity.ReviewCategory;
import ai.nova.platform.prreview.service.FindingDraft;
import ai.nova.platform.prreview.service.PerformanceReviewService;

@SpringBootTest(properties = {"nova.pr-review.enabled=true"})
class PerformanceReviewTest {

    @Autowired
    private PerformanceReviewService performanceReviewService;

    @Test
    void detectsFindAllInLoopAndSelectStar() {
        String diff = """
                for (Order order : orders) {
                    List<Item> items = itemRepository.findAll();
                }
                String sql = "SELECT * FROM huge_table";
                byte[] buffer = new byte[10000000];
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < m; j++) {
                        repository.save(entity);
                    }
                }
                """;
        List<FindingDraft> findings = performanceReviewService.analyze(diff);
        assertThat(findings).isNotEmpty();
        assertThat(findings).allMatch(f -> f.category() == ReviewCategory.Performance);
        assertThat(findings.stream().map(FindingDraft::title).map(String::toLowerCase))
                .anyMatch(title -> title.contains("findall") || title.contains("select *") || title.contains("nested"));
    }
}
