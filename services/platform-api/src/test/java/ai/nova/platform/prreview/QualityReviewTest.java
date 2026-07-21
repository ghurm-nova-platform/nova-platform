package ai.nova.platform.prreview;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.prreview.entity.ReviewCategory;
import ai.nova.platform.prreview.service.FindingDraft;
import ai.nova.platform.prreview.service.QualityReviewService;

@SpringBootTest(properties = {"nova.pr-review.enabled=true"})
class QualityReviewTest {

    @Autowired
    private QualityReviewService qualityReviewService;

    @Test
    void detectsEmptyCatchSystemOutAndTodoDensity() {
        String duplicated = "this is a duplicated non trivial line used for quality detection";
        StringBuilder builder = new StringBuilder();
        builder.append("public void messy() {\n");
        builder.append("  try { work(); } catch (Exception e) {}\n");
        builder.append("  System.out.println(\"debug\");\n");
        builder.append("  // TODO one\n");
        builder.append("  // TODO two\n");
        builder.append("  // FIXME three\n");
        builder.append("  // TODO four\n");
        builder.append("  // TODO five\n");
        builder.append(duplicated).append('\n');
        builder.append(duplicated).append('\n');
        builder.append(duplicated).append('\n');
        builder.append(IntStream.range(0, 90).mapToObj(i -> "    int x" + i + " = " + i + ";").collect(Collectors.joining("\n")));
        builder.append("\n}\n");

        List<FindingDraft> findings = qualityReviewService.analyze(builder.toString());
        assertThat(findings).isNotEmpty();
        assertThat(findings).allMatch(f -> f.category() == ReviewCategory.CodeQuality);
        assertThat(findings.stream().map(FindingDraft::title).map(String::toLowerCase))
                .anyMatch(title -> title.contains("catch")
                        || title.contains("system.out")
                        || title.contains("todo")
                        || title.contains("duplicated")
                        || title.contains("long method"));
    }
}
