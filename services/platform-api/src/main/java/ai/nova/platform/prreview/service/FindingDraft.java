package ai.nova.platform.prreview.service;

import java.util.List;
import java.util.UUID;

import ai.nova.platform.prreview.entity.ReviewCategory;
import ai.nova.platform.prreview.entity.ReviewSeverity;

public record FindingDraft(
        ReviewCategory category,
        ReviewSeverity severity,
        String title,
        String description,
        String recommendation,
        String filePath,
        Integer lineHint,
        List<String> references,
        List<UUID> knowledgeDocumentIds,
        String ruleCode,
        String evidenceExcerpt) {

    public FindingDraft(
            ReviewCategory category,
            ReviewSeverity severity,
            String title,
            String description,
            String recommendation) {
        this(category, severity, title, description, recommendation, null, null, List.of(), List.of(), null, null);
    }

    public FindingDraft(
            ReviewCategory category,
            ReviewSeverity severity,
            String title,
            String description,
            String recommendation,
            String filePath,
            Integer lineHint,
            List<String> references,
            List<UUID> knowledgeDocumentIds) {
        this(
                category,
                severity,
                title,
                description,
                recommendation,
                filePath,
                lineHint,
                references,
                knowledgeDocumentIds,
                null,
                null);
    }

    public FindingDraft withKnowledgeDocumentIds(List<UUID> ids) {
        return new FindingDraft(
                category,
                severity,
                title,
                description,
                recommendation,
                filePath,
                lineHint,
                references,
                ids == null ? List.of() : List.copyOf(ids),
                ruleCode,
                evidenceExcerpt);
    }

    public static FindingDraft of(
            ReviewCategory category,
            ReviewSeverity severity,
            String ruleCode,
            String title,
            String description,
            String recommendation,
            String evidenceExcerpt) {
        return new FindingDraft(
                category,
                severity,
                title,
                description,
                recommendation,
                null,
                null,
                List.of(),
                List.of(),
                ruleCode,
                evidenceExcerpt);
    }
}
