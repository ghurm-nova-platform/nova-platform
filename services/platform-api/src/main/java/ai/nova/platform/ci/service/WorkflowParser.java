package ai.nova.platform.ci.service;

import java.util.Locale;

import ai.nova.platform.ci.entity.CiOverallStatus;

public final class WorkflowParser {

    private WorkflowParser() {
    }

    /**
     * Maps GitHub workflow/job/step conclusion to overall CI status.
     * success→SUCCESS, failure→FAILED, cancelled→CANCELLED, timed_out→TIMED_OUT,
     * null/in_progress→IN_PROGRESS, else UNKNOWN.
     */
    public static CiOverallStatus mapConclusion(String status, String conclusion) {
        if (conclusion != null && !conclusion.isBlank()) {
            return switch (conclusion.trim().toLowerCase(Locale.ROOT)) {
                case "success" -> CiOverallStatus.SUCCESS;
                case "failure", "failed" -> CiOverallStatus.FAILED;
                case "cancelled", "canceled" -> CiOverallStatus.CANCELLED;
                case "timed_out" -> CiOverallStatus.TIMED_OUT;
                case "skipped", "neutral" -> CiOverallStatus.SUCCESS;
                default -> CiOverallStatus.UNKNOWN;
            };
        }
        if (status != null) {
            String normalized = status.trim().toLowerCase(Locale.ROOT);
            if ("in_progress".equals(normalized) || "queued".equals(normalized) || "pending".equals(normalized)) {
                return CiOverallStatus.IN_PROGRESS;
            }
            if ("completed".equals(normalized)) {
                return CiOverallStatus.UNKNOWN;
            }
        }
        return CiOverallStatus.IN_PROGRESS;
    }

    public static boolean isFailed(CiOverallStatus status) {
        return status == CiOverallStatus.FAILED
                || status == CiOverallStatus.CANCELLED
                || status == CiOverallStatus.TIMED_OUT;
    }
}
