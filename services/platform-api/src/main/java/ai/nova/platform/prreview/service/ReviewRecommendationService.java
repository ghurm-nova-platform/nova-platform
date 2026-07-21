package ai.nova.platform.prreview.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import ai.nova.platform.prreview.entity.RecommendationPriority;
import ai.nova.platform.prreview.entity.ReviewSeverity;

@Service
public class ReviewRecommendationService {

    public List<RecommendationDraft> fromFindings(List<FindingDraft> findings) {
        List<RecommendationDraft> recommendations = new ArrayList<>();
        if (findings == null) {
            return recommendations;
        }
        for (FindingDraft finding : findings) {
            if (!isWarningOrHigher(finding.severity())) {
                continue;
            }
            recommendations.add(new RecommendationDraft(
                    finding,
                    null,
                    priorityFor(finding.severity()),
                    finding.title(),
                    finding.recommendation(),
                    finding.knowledgeDocumentIds()));
        }
        return recommendations;
    }

    public RecommendationDraft bindFinding(RecommendationDraft draft, UUID findingId) {
        return new RecommendationDraft(
                draft.sourceFinding(),
                findingId,
                draft.priority(),
                draft.title(),
                draft.description(),
                draft.knowledgeDocumentIds());
    }

    private boolean isWarningOrHigher(ReviewSeverity severity) {
        return severity == ReviewSeverity.WARNING
                || severity == ReviewSeverity.ERROR
                || severity == ReviewSeverity.BLOCKER;
    }

    private RecommendationPriority priorityFor(ReviewSeverity severity) {
        return switch (severity) {
            case BLOCKER, ERROR -> RecommendationPriority.HIGH;
            case WARNING -> RecommendationPriority.MEDIUM;
            default -> RecommendationPriority.LOW;
        };
    }

    public record RecommendationDraft(
            FindingDraft sourceFinding,
            UUID findingId,
            RecommendationPriority priority,
            String title,
            String description,
            List<UUID> knowledgeDocumentIds) {
    }
}
