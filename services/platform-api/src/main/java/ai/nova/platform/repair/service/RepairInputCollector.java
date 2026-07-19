package ai.nova.platform.repair.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import ai.nova.platform.ci.dto.CiDtos.CiObservationOperation;
import ai.nova.platform.ci.entity.CiOverallStatus;
import ai.nova.platform.ci.service.CiStorageService;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.repair.entity.RepairInputSource;
import ai.nova.platform.review.dto.ReviewDtos.ReviewFinding;
import ai.nova.platform.review.dto.ReviewDtos.ReviewResult;
import ai.nova.platform.review.service.ReviewStorageService;
import ai.nova.platform.testing.dto.TestingDtos.TestingResult;
import ai.nova.platform.testing.service.TestingStorageService;

@Service
public class RepairInputCollector {

    private final ReviewStorageService reviewStorageService;
    private final TestingStorageService testingStorageService;
    private final CiStorageService ciStorageService;

    public RepairInputCollector(
            ReviewStorageService reviewStorageService,
            TestingStorageService testingStorageService,
            CiStorageService ciStorageService) {
        this.reviewStorageService = reviewStorageService;
        this.testingStorageService = testingStorageService;
        this.ciStorageService = ciStorageService;
    }

    public record CollectedInput(RepairInputSource sourceType, String sourceRef, int priority, String detail) {
    }

    public List<CollectedInput> collect(AgentOrchestrationTask task, PatchResult priorPatch) {
        UUID taskId = task.getId();
        UUID organizationId = task.getOrganizationId();
        List<CollectedInput> inputs = new ArrayList<>();

        ReviewResult review = reviewStorageService.findLatest(taskId, organizationId);
        if (review != null && !review.approved()) {
            if (review.findings() != null && !review.findings().isEmpty()) {
                for (ReviewFinding finding : review.findings()) {
                    String detail = finding.title() + ": " + finding.description();
                    if (finding.recommendation() != null && !finding.recommendation().isBlank()) {
                        detail = detail + " | recommendation=" + finding.recommendation();
                    }
                    inputs.add(new CollectedInput(
                            RepairInputSource.REVIEW,
                            review.id().toString(),
                            RepairInputSource.REVIEW.priority(),
                            truncate(detail)));
                }
            } else {
                inputs.add(new CollectedInput(
                        RepairInputSource.REVIEW,
                        review.id().toString(),
                        RepairInputSource.REVIEW.priority(),
                        truncate(review.summary() == null ? "Review not approved" : review.summary())));
            }
        }

        TestingResult testing = testingStorageService.findLatest(taskId, organizationId);
        if (testing != null) {
            boolean lowCoverage = testing.coverageEstimate() < 50;
            boolean summaryFails = containsFail(testing.summary());
            if (lowCoverage) {
                inputs.add(new CollectedInput(
                        RepairInputSource.COVERAGE,
                        testing.id().toString(),
                        RepairInputSource.COVERAGE.priority(),
                        truncate("Coverage estimate "
                                + testing.coverageEstimate()
                                + "% below threshold: "
                                + (testing.summary() == null ? "" : testing.summary()))));
            }
            if (summaryFails) {
                inputs.add(new CollectedInput(
                        RepairInputSource.TEST,
                        testing.id().toString(),
                        RepairInputSource.TEST.priority(),
                        truncate(testing.summary())));
            }
        }

        CiObservationOperation ci = ciStorageService.findLatest(taskId, organizationId);
        if (ci != null && isCiFailure(ci.overallStatus())) {
            List<String> messages = ciMessages(ci);
            if (messages.isEmpty()) {
                inputs.add(new CollectedInput(
                        RepairInputSource.CI,
                        ci.id().toString(),
                        RepairInputSource.CI.priority(),
                        truncate("CI overall status " + ci.overallStatus().name())));
            } else {
                for (String message : messages) {
                    inputs.add(new CollectedInput(
                            RepairInputSource.CI,
                            ci.id().toString(),
                            RepairInputSource.CI.priority(),
                            truncate(message)));
                    addHeuristicInputs(inputs, ci.id().toString(), message);
                }
            }
        }

        if (priorPatch != null && priorPatch.validation() != null && !priorPatch.validation().valid()) {
            inputs.add(new CollectedInput(
                    RepairInputSource.STATIC_ANALYSIS,
                    priorPatch.id().toString(),
                    RepairInputSource.STATIC_ANALYSIS.priority(),
                    truncate(priorPatch.validation().message())));
        }

        inputs.sort(java.util.Comparator.comparingInt(CollectedInput::priority));
        return List.copyOf(inputs);
    }

    private static boolean isCiFailure(CiOverallStatus status) {
        return status == CiOverallStatus.FAILED
                || status == CiOverallStatus.CANCELLED
                || status == CiOverallStatus.TIMED_OUT;
    }

    private static List<String> ciMessages(CiObservationOperation ci) {
        List<String> messages = new ArrayList<>();
        if (ci.failureSummary() != null && ci.failureSummary().errorMessages() != null) {
            messages.addAll(ci.failureSummary().errorMessages());
        }
        if (ci.errorMessage() != null && !ci.errorMessage().isBlank()) {
            messages.add(ci.errorMessage());
        }
        return messages.stream().filter(m -> m != null && !m.isBlank()).distinct().toList();
    }

    private static void addHeuristicInputs(List<CollectedInput> inputs, String sourceRef, String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        Set<RepairInputSource> added = new LinkedHashSet<>();
        if (containsAny(lower, "compile", "compilation", "javac", "build failed", "mvn", "gradle build")) {
            added.add(RepairInputSource.COMPILE);
        }
        if (containsAny(lower, "lint", "checkstyle", "static analysis", "sonar", "spotbugs", "pmd")) {
            added.add(RepairInputSource.STATIC_ANALYSIS);
        }
        if (containsAny(lower, "format", "prettier", "spotless", "google-java-format")) {
            added.add(RepairInputSource.FORMATTING);
        }
        if (containsAny(lower, "dependency", "maven dependency", "gradle dependency", "npm err", "could not resolve")) {
            added.add(RepairInputSource.DEPENDENCY);
        }
        for (RepairInputSource source : added) {
            inputs.add(new CollectedInput(source, sourceRef, source.priority(), truncate(message)));
        }
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsFail(String summary) {
        return summary != null && summary.toLowerCase(Locale.ROOT).contains("fail");
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= 3990 ? trimmed : trimmed.substring(0, 3990) + "...";
    }
}
