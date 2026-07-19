package ai.nova.platform.repair.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.dto.PatchDtos.ParsedPatchOutput;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.patch.entity.PatchStatus;
import ai.nova.platform.patch.service.PatchDiffParser.ParsedDiff;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.repair.config.RepairProperties;
import ai.nova.platform.repair.dto.RepairDtos.RepairOperation;
import ai.nova.platform.repair.dto.RepairDtos.RepairRunRequest;
import ai.nova.platform.repair.dto.RepairDtos.TimelineEvent;
import ai.nova.platform.repair.entity.RepairStatus;
import ai.nova.platform.repair.security.RepairAuthorizationService;
import ai.nova.platform.repair.service.RepairFingerprint.InputLine;
import ai.nova.platform.repair.service.RepairInputCollector.CollectedInput;
import ai.nova.platform.repair.service.RepairJsonParser.ParsedRepairOutput;
import ai.nova.platform.review.service.ReviewStorageService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

/**
 * Repair Agent: proposes NEW PatchResults after review, testing, or CI failures.
 * Never overwrites prior patches, merges, approves, deploys, or executes shell/CI.
 */
@Service
public class RepairAgentService {

    private final RepairAuthorizationService authorizationService;
    private final RepairProperties properties;
    private final AgentOrchestrationTaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ArtifactStorageService artifactStorageService;
    private final PatchStorageService patchStorageService;
    private final RepairInputCollector inputCollector;
    private final ReviewStorageService reviewStorageService;
    private final StandardRepairStrategy repairStrategy;
    private final RepairJsonParser jsonParser;
    private final RepairValidator validator;
    private final RepairStorageService storageService;

    public RepairAgentService(
            RepairAuthorizationService authorizationService,
            RepairProperties properties,
            AgentOrchestrationTaskRepository taskRepository,
            ProjectRepository projectRepository,
            ArtifactStorageService artifactStorageService,
            PatchStorageService patchStorageService,
            RepairInputCollector inputCollector,
            ReviewStorageService reviewStorageService,
            StandardRepairStrategy repairStrategy,
            RepairJsonParser jsonParser,
            RepairValidator validator,
            RepairStorageService storageService) {
        this.authorizationService = authorizationService;
        this.properties = properties;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.artifactStorageService = artifactStorageService;
        this.patchStorageService = patchStorageService;
        this.inputCollector = inputCollector;
        this.reviewStorageService = reviewStorageService;
        this.repairStrategy = repairStrategy;
        this.jsonParser = jsonParser;
        this.validator = validator;
        this.storageService = storageService;
    }

    public RepairOperation run(RepairRunRequest request, AuthenticatedUser user) {
        authorizationService.require(user, RepairAuthorizationService.REPAIR_RUN);
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "REPAIR_DISABLED", "Repair agent is disabled");
        }
        if (request == null || request.taskId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REPAIR_INVALID_REQUEST", "Task id is required");
        }

        Instant startedAt = Instant.now();
        List<TimelineEvent> timeline = new ArrayList<>();
        timeline.add(new TimelineEvent("STARTED", startedAt, "Repair agent started"));

        AgentOrchestrationTask task = requireTask(request.taskId(), user.getOrganizationId());
        projectRepository
                .findByIdAndOrganizationId(task.getProjectId(), user.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));

        PatchResult priorPatch = patchStorageService.findLatest(task.getId(), user.getOrganizationId());
        if (priorPatch == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "REPAIR_INPUT_NOT_FOUND",
                    "No prior patch result found for task; run Patch Agent first");
        }

        if (properties.isRequireReviewBeforeRepair()) {
            if (reviewStorageService.findLatest(task.getId(), user.getOrganizationId()) == null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "REPAIR_INVALID_STATE",
                        "Review result required before repair");
            }
        }

        List<CollectedInput> failureInputs = inputCollector.collect(task, priorPatch);
        if (failureInputs.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REPAIR_INVALID_STATE", "No failures");
        }

        String fingerprint = RepairFingerprint.compute(
                failureInputs.stream()
                        .map(i -> new InputLine(i.sourceType(), i.detail()))
                        .toList(),
                priorPatch.id());

        RepairOperation existing = storageService.findSucceededByFingerprint(
                user.getOrganizationId(), task.getId(), fingerprint);
        if (existing != null) {
            return existing;
        }

        long priorAttempts = storageService.countByTask(task.getId(), user.getOrganizationId());
        if (priorAttempts >= properties.getMaxAttempts()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "REPAIR_LIMIT_EXCEEDED",
                    "Maximum repair attempts exceeded for task");
        }

        RepairOperation inFlight = storageService.findByFingerprint(
                user.getOrganizationId(), task.getId(), fingerprint);
        if (inFlight != null && inFlight.status() != RepairStatus.SUCCEEDED && inFlight.status() != RepairStatus.FAILED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "REPAIR_ALREADY_EXISTS",
                    "Repair operation with same fingerprint is already in progress");
        }

        int attemptNumber = (int) priorAttempts + 1;
        String reason = buildReason(failureInputs);
        UUID operationId = UUID.randomUUID();

        List<GeneratedArtifactResponse> artifacts =
                artifactStorageService.listByTask(task.getId(), user.getOrganizationId());

        String provider = properties.getDefaultProvider();
        String model = resolveModel(task);

        try {
            storageService.startPending(
                    operationId, task, attemptNumber, priorPatch.id(), fingerprint, reason, startedAt, timeline);
            timeline.add(new TimelineEvent("PENDING", Instant.now(), "Operation " + operationId));

            storageService.updateStatus(operationId, RepairStatus.COLLECTING, timeline);
            storageService.saveInputs(operationId, failureInputs, timeline);

            storageService.updateStatus(operationId, RepairStatus.ANALYZING, timeline);

            storageService.updateStatus(operationId, RepairStatus.GENERATING_PATCH, timeline);
            RepairContext context = new RepairContext(
                    task, priorPatch, artifacts, failureInputs, provider, model, task.getOrganizationId(), task.getProjectId());
            RepairProposal proposal = repairStrategy.propose(context);

            storageService.updateStatus(operationId, RepairStatus.VALIDATING, timeline);
            ParsedRepairOutput parsedRepair = new ParsedRepairOutput(
                    proposal.summary(),
                    null,
                    null,
                    null,
                    proposal.unifiedDiffPatch(),
                    PatchStatus.VALID,
                    proposal.confidence(),
                    proposal.reason(),
                    proposal.repairedFiles());
            ParsedDiff diff = validator.validate(parsedRepair);
            ParsedPatchOutput patchOutput = jsonParser.toPatchOutput(parsedRepair);

            PatchResult newPatch = patchStorageService.appendResult(
                    task,
                    artifacts,
                    patchOutput,
                    diff,
                    null,
                    model,
                    provider,
                    null);

            storageService.saveActions(operationId, proposal.actions(), timeline);
            RepairOperation result = storageService.markSucceeded(
                    operationId,
                    newPatch.id(),
                    proposal.summary(),
                    proposal.confidence(),
                    proposal.reason(),
                    proposal.repairedFiles(),
                    Instant.now(),
                    timeline);
            timeline.add(new TimelineEvent("COMPLETED", Instant.now(), "SUCCEEDED"));
            return result;
        } catch (DataIntegrityViolationException ex) {
            RepairOperation duplicate = storageService.findByFingerprint(
                    user.getOrganizationId(), task.getId(), fingerprint);
            if (duplicate != null && duplicate.status() == RepairStatus.SUCCEEDED) {
                return duplicate;
            }
            storageService.markFailed(operationId, "REPAIR_ALREADY_EXISTS", ex.getMessage(), Instant.now(), timeline);
            throw new ApiException(
                    HttpStatus.CONFLICT, "REPAIR_ALREADY_EXISTS", "Repair operation with same fingerprint already exists");
        } catch (ApiException ex) {
            if (operationId != null) {
                try {
                    storageService.markFailed(operationId, ex.getCode(), ex.getMessage(), Instant.now(), timeline);
                } catch (Exception ignored) {
                    // Operation may not have been persisted yet.
                }
            }
            throw ex;
        } catch (RuntimeException ex) {
            if (operationId != null) {
                try {
                    storageService.markFailed(operationId, "REPAIR_FAILED", ex.getMessage(), Instant.now(), timeline);
                } catch (Exception ignored) {
                    // Operation may not have been persisted yet.
                }
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public RepairOperation getLatest(UUID taskId, AuthenticatedUser user) {
        authorizationService.require(user, RepairAuthorizationService.REPAIR_READ);
        requireTask(taskId, user.getOrganizationId());
        RepairOperation result = storageService.findLatest(taskId, user.getOrganizationId());
        if (result == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "REPAIR_NOT_FOUND", "No repair operation found for task");
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<RepairOperation> getHistory(UUID taskId, AuthenticatedUser user) {
        authorizationService.require(user, RepairAuthorizationService.REPAIR_READ);
        requireTask(taskId, user.getOrganizationId());
        return storageService.findHistory(taskId, user.getOrganizationId());
    }

    private AgentOrchestrationTask requireTask(UUID taskId, UUID organizationId) {
        return taskRepository
                .findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "REPAIR_TASK_NOT_FOUND", "Orchestration task not found"));
    }

    private String resolveModel(AgentOrchestrationTask task) {
        if (task.getModelReference() != null && !task.getModelReference().isBlank()) {
            return task.getModelReference().trim();
        }
        return properties.getDefaultModel();
    }

    private static String buildReason(List<CollectedInput> inputs) {
        if (inputs.isEmpty()) {
            return "Repair requested";
        }
        CollectedInput primary = inputs.get(0);
        String reason = primary.sourceType().name() + ": " + primary.detail();
        return reason.length() <= 2000 ? reason : reason.substring(0, 2000);
    }
}
