package ai.nova.platform.prreview.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import ai.nova.platform.prreview.entity.ReviewCategory;
import ai.nova.platform.prreview.entity.ReviewSeverity;

@Service
public class ReviewRiskScoreService {

    public enum ScoreBucket {
        ARCHITECTURE,
        SECURITY,
        PERFORMANCE,
        QUALITY,
        TESTING,
        DOCUMENTATION
    }

    public Map<ScoreBucket, Integer> computeCategoryScores(List<FindingDraft> findings) {
        Map<ScoreBucket, Integer> scores = new EnumMap<>(ScoreBucket.class);
        for (ScoreBucket bucket : ScoreBucket.values()) {
            scores.put(bucket, 100);
        }
        if (findings == null) {
            return scores;
        }
        for (FindingDraft finding : findings) {
            ScoreBucket bucket = bucketFor(finding.category());
            int penalty = penaltyFor(finding.severity());
            scores.put(bucket, Math.max(0, scores.get(bucket) - penalty));
        }
        return scores;
    }

    public int overallScore(Map<ScoreBucket, Integer> categoryScores) {
        if (categoryScores == null || categoryScores.isEmpty()) {
            return 100;
        }
        int sum = 0;
        for (ScoreBucket bucket : ScoreBucket.values()) {
            sum += categoryScores.getOrDefault(bucket, 100);
        }
        return sum / ScoreBucket.values().length;
    }

    /** Risk score is the inverse of overall quality (0 = lowest risk, 100 = highest risk). */
    public int riskScore(int overallScore) {
        return Math.max(0, Math.min(100, 100 - overallScore));
    }

    public int riskScore(List<FindingDraft> findings) {
        Map<ScoreBucket, Integer> scores = computeCategoryScores(findings);
        return riskScore(overallScore(scores));
    }

    private int penaltyFor(ReviewSeverity severity) {
        return switch (severity) {
            case BLOCKER -> 40;
            case ERROR -> 20;
            case WARNING -> 10;
            case SUGGESTION -> 3;
            case INFO -> 1;
        };
    }

    private ScoreBucket bucketFor(ReviewCategory category) {
        return switch (category) {
            case Architecture -> ScoreBucket.ARCHITECTURE;
            case Security -> ScoreBucket.SECURITY;
            case Performance -> ScoreBucket.PERFORMANCE;
            case Testing -> ScoreBucket.TESTING;
            case Documentation -> ScoreBucket.DOCUMENTATION;
            case CodeQuality, Maintainability, Database, ApiDesign, Frontend, Backend, Infrastructure ->
                    ScoreBucket.QUALITY;
        };
    }
}
