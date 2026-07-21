package ai.nova.platform.prreview;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.prreview.entity.ReviewCategory;
import ai.nova.platform.prreview.entity.ReviewSeverity;
import ai.nova.platform.prreview.service.FindingDraft;
import ai.nova.platform.prreview.service.SecurityReviewService;

@SpringBootTest(properties = {"nova.pr-review.enabled=true"})
class SecurityReviewTest {

    @Autowired
    private SecurityReviewService securityReviewService;

    @Test
    void detectsSecretsAndSqlConcat() {
        String diff = """
                String password = "x";
                String sql = "SELECT * FROM users WHERE id=" + request.getId();
                element.innerHTML = html;
                Runtime.exec("rm -rf /");
                ObjectInputStream in = null;
                Path path = Paths.get("../etc/passwd");
                Authorization: Bearer abc.def.ghi
                String secret = "tok";
                String api_key = "k";
                """;
        List<FindingDraft> findings = securityReviewService.analyze(diff);
        assertThat(findings).isNotEmpty();
        assertThat(findings).allMatch(f -> f.category() == ReviewCategory.Security);
        assertThat(findings).anyMatch(f -> f.severity() == ReviewSeverity.BLOCKER);
        assertThat(findings.stream().map(FindingDraft::title).toList())
                .anyMatch(title -> title.toLowerCase().contains("password")
                        || title.toLowerCase().contains("sql")
                        || title.toLowerCase().contains("api"));
    }
}
