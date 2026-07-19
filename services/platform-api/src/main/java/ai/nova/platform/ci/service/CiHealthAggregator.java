package ai.nova.platform.ci.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import ai.nova.platform.ci.dto.CiDtos.CiFailureSummary;
import ai.nova.platform.ci.entity.CiOverallStatus;
import ai.nova.platform.ci.provider.ProviderJob;
import ai.nova.platform.ci.provider.ProviderStep;
import ai.nova.platform.ci.provider.ProviderWorkflowRun;

@Service
public class CiHealthAggregator {

    public record AggregatedHealth(
            CiOverallStatus overallStatus,
            CiFailureSummary failureSummary,
            String retryRecommendation,
            String failureSummaryText) {
    }

    public AggregatedHealth aggregate(List<ProviderWorkflowRun> runs, List<List<ProviderJob>> jobsPerRun) {
        int failedWorkflows = 0;
        int failedJobs = 0;
        int failedSteps = 0;
        long totalDurationMs = 0;
        List<String> errorMessages = new ArrayList<>();
        List<String> affectedFiles = new ArrayList<>();

        CiOverallStatus worst = CiOverallStatus.SUCCESS;

        for (int i = 0; i < runs.size(); i++) {
            ProviderWorkflowRun run = runs.get(i);
            CiOverallStatus runStatus = WorkflowParser.mapConclusion(run.status(), run.conclusion());
            worst = maxSeverity(worst, runStatus);
            if (WorkflowParser.isFailed(runStatus)) {
                failedWorkflows++;
                if (run.failureReason() != null) {
                    errorMessages.add(run.workflowName() + ": " + run.failureReason());
                }
            }
            if (run.durationMs() != null) {
                totalDurationMs += run.durationMs();
            }

            List<ProviderJob> jobs = i < jobsPerRun.size() ? jobsPerRun.get(i) : List.of();
            for (ProviderJob job : jobs) {
                CiOverallStatus jobStatus = WorkflowParser.mapConclusion(job.status(), job.conclusion());
                worst = maxSeverity(worst, jobStatus);
                if (WorkflowParser.isFailed(jobStatus)) {
                    failedJobs++;
                    if (job.failureReason() != null) {
                        errorMessages.add(job.jobName() + ": " + job.failureReason());
                    }
                }
                if (job.durationMs() != null) {
                    totalDurationMs += job.durationMs();
                }
                for (ProviderStep step : job.steps()) {
                    CiOverallStatus stepStatus = WorkflowParser.mapConclusion(step.status(), step.conclusion());
                    worst = maxSeverity(worst, stepStatus);
                    if (WorkflowParser.isFailed(stepStatus)) {
                        failedSteps++;
                        if (step.failureReason() != null) {
                            errorMessages.add(step.stepName() + ": " + step.failureReason());
                        }
                        extractAffectedFile(step.stepName(), affectedFiles);
                    }
                }
            }
        }

        if (runs.isEmpty()) {
            worst = CiOverallStatus.UNKNOWN;
        }

        CiFailureSummary summary = new CiFailureSummary(
                failedWorkflows,
                failedJobs,
                failedSteps,
                totalDurationMs,
                List.copyOf(errorMessages),
                List.copyOf(affectedFiles));

        String failureSummaryText = buildFailureSummaryText(summary);
        String retryRecommendation = buildRetryRecommendation(worst, summary);

        return new AggregatedHealth(worst, summary, retryRecommendation, failureSummaryText);
    }

    private static CiOverallStatus maxSeverity(CiOverallStatus current, CiOverallStatus candidate) {
        return severityRank(candidate) > severityRank(current) ? candidate : current;
    }

    private static int severityRank(CiOverallStatus status) {
        return switch (status) {
            case FAILED -> 5;
            case TIMED_OUT -> 4;
            case CANCELLED -> 3;
            case IN_PROGRESS -> 2;
            case UNKNOWN -> 1;
            case SUCCESS -> 0;
        };
    }

    private static String buildFailureSummaryText(CiFailureSummary summary) {
        if (summary.failedWorkflows() == 0 && summary.failedJobs() == 0 && summary.failedSteps() == 0) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Failed workflows: ")
                .append(summary.failedWorkflows())
                .append(", jobs: ")
                .append(summary.failedJobs())
                .append(", steps: ")
                .append(summary.failedSteps());
        if (!summary.errorMessages().isEmpty()) {
            builder.append(". ").append(String.join("; ", summary.errorMessages()));
        }
        String text = builder.toString();
        return text.length() > 4000 ? text.substring(0, 3997) + "..." : text;
    }

    private static String buildRetryRecommendation(CiOverallStatus overall, CiFailureSummary summary) {
        if (overall == CiOverallStatus.SUCCESS) {
            return "CI passed; no retry needed";
        }
        if (overall == CiOverallStatus.IN_PROGRESS) {
            return "CI still running; wait for completion before investigating";
        }
        if (summary.failedJobs() > 0 || summary.failedSteps() > 0) {
            return "Investigate failed jobs; do not auto-rerun from Nova";
        }
        return "Investigate failed workflows; do not auto-rerun from Nova";
    }

    private static void extractAffectedFile(String stepName, List<String> affectedFiles) {
        if (stepName == null || stepName.isBlank()) {
            return;
        }
        String lower = stepName.toLowerCase(Locale.ROOT);
        int srcIdx = lower.indexOf("src/");
        if (srcIdx >= 0) {
            String candidate = stepName.substring(srcIdx).split("\\s")[0];
            if (!affectedFiles.contains(candidate)) {
                affectedFiles.add(candidate);
            }
        }
    }
}
