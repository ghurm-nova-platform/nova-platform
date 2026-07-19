package ai.nova.platform.approval.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.approval.config.ApprovalGateProperties;
import ai.nova.platform.ci.dto.CiDtos.CiObservationOperation;
import ai.nova.platform.ci.entity.CiOverallStatus;
import ai.nova.platform.ci.repository.CiObservationOperationRepository;
import ai.nova.platform.ci.service.CiStorageService;
import ai.nova.platform.git.dto.GitDtos.GitOperation;
import ai.nova.platform.git.service.GitStorageService;
import ai.nova.platform.git.service.GitValidator;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.project.Project;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestOperation;
import ai.nova.platform.pullrequest.service.PullRequestStorageService;
import ai.nova.platform.repair.dto.RepairDtos.RepairOperation;
import ai.nova.platform.repair.entity.RepairStatus;
import ai.nova.platform.repair.repository.RepairOperationRepository;
import ai.nova.platform.repair.service.RepairStorageService;
import ai.nova.platform.review.dto.ReviewDtos.ReviewResult;
import ai.nova.platform.review.service.ReviewStorageService;
import ai.nova.platform.testing.dto.TestingDtos.TestingResult;
import ai.nova.platform.testing.service.TestingStorageService;
import ai.nova.platform.web.error.ApiException;

@Service
public class ApprovalEvidenceCollector {

    private final ReviewStorageService reviewStorageService;
    private final TestingStorageService testingStorageService;
    private final PatchStorageService patchStorageService;
    private final GitStorageService gitStorageService;
    private final PullRequestStorageService pullRequestStorageService;
    private final CiStorageService ciStorageService;
    private final RepairStorageService repairStorageService;
    private final ProjectRepository projectRepository;
    private final GitValidator gitValidator;
    private final CiObservationOperationRepository ciObservationOperationRepository;
    private final RepairOperationRepository repairOperationRepository;
    private final ApprovalGateProperties properties;

    public ApprovalEvidenceCollector(
            ReviewStorageService reviewStorageService,
            TestingStorageService testingStorageService,
            PatchStorageService patchStorageService,
            GitStorageService gitStorageService,
            PullRequestStorageService pullRequestStorageService,
            CiStorageService ciStorageService,
            RepairStorageService repairStorageService,
            ProjectRepository projectRepository,
            GitValidator gitValidator,
            CiObservationOperationRepository ciObservationOperationRepository,
            RepairOperationRepository repairOperationRepository,
            ApprovalGateProperties properties) {
        this.reviewStorageService = reviewStorageService;
        this.testingStorageService = testingStorageService;
        this.patchStorageService = patchStorageService;
        this.gitStorageService = gitStorageService;
        this.pullRequestStorageService = pullRequestStorageService;
        this.ciStorageService = ciStorageService;
        this.repairStorageService = repairStorageService;
        this.projectRepository = projectRepository;
        this.gitValidator = gitValidator;
        this.ciObservationOperationRepository = ciObservationOperationRepository;
        this.repairOperationRepository = repairOperationRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public ApprovalEvidenceBundle collect(AgentOrchestrationTask task) {
        UUID taskId = task.getId();
        UUID orgId = task.getOrganizationId();
        Project project = projectRepository
                .findByIdAndOrganizationId(task.getProjectId(), orgId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found for task"));

        ReviewResult review = reviewStorageService.findLatest(taskId, orgId);
        TestingResult testing = testingStorageService.findLatest(taskId, orgId);
        PatchResult patch = patchStorageService.findLatest(taskId, orgId);
        GitOperation git = gitStorageService.findLatest(taskId, orgId);
        PullRequestOperation pullRequest = pullRequestStorageService.findLatest(taskId, orgId);
        CiObservationOperation ci = ciStorageService.findLatest(taskId, orgId);
        RepairOperation repair = repairStorageService.findLatest(taskId, orgId);

        String computedPatchHash = null;
        if (patch != null && patch.patch() != null && !patch.patch().isBlank()) {
            computedPatchHash = gitValidator.sha256(patch.patch());
        }

        boolean ciHistoryHasFailure = ciObservationOperationRepository
                .findByTaskIdAndOrganizationIdOrderByCreatedAtDesc(taskId, orgId)
                .stream()
                .anyMatch(op -> op.getOverallStatus() == CiOverallStatus.FAILED);

        boolean repairSucceededAfterFailure = false;
        if (ciHistoryHasFailure) {
            repairSucceededAfterFailure = repairOperationRepository
                    .findByTaskIdAndOrganizationIdOrderByCreatedAtDesc(taskId, orgId)
                    .stream()
                    .anyMatch(op -> op.getStatus() == RepairStatus.SUCCEEDED);
        }

        return new ApprovalEvidenceBundle(
                task,
                project,
                review,
                testing,
                patch,
                git,
                pullRequest,
                ci,
                repair,
                computedPatchHash,
                review != null ? review.id() : null,
                testing != null ? testing.id() : null,
                patch != null ? patch.id() : null,
                git != null ? git.id() : null,
                pullRequest != null ? pullRequest.id() : null,
                ci != null ? ci.id() : null,
                repair != null ? repair.id() : null,
                ciHistoryHasFailure,
                repairSucceededAfterFailure);
    }
}
