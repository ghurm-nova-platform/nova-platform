package ai.nova.platform.planner.service;

import org.springframework.stereotype.Service;

import ai.nova.platform.orchestration.dto.OrchestrationDtos.RunResponse;
import ai.nova.platform.planner.dto.PlannerDtos.ImportPlanRequest;
import ai.nova.platform.planner.dto.PlannerDtos.PlanAndCreateResponse;
import ai.nova.platform.planner.dto.PlannerDtos.PlannerRequest;
import ai.nova.platform.planner.dto.PlannerDtos.PlannerResponse;
import ai.nova.platform.planner.security.PlannerAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;

/**
 * Orchestrates planning and import. External AI planning stays outside DB transactions;
 * durable import is delegated to {@link PlannerPlanImporter} through the Spring proxy.
 */
@Service
public class PlannerImportService {

    private final PlannerAuthorizationService authorizationService;
    private final PlannerService plannerService;
    private final PlannerPlanImporter planImporter;

    public PlannerImportService(
            PlannerAuthorizationService authorizationService,
            PlannerService plannerService,
            PlannerPlanImporter planImporter) {
        this.authorizationService = authorizationService;
        this.plannerService = plannerService;
        this.planImporter = planImporter;
    }

    public PlanAndCreateResponse planAndCreate(PlannerRequest request, AuthenticatedUser user) {
        authorizationService.require(user, PlannerAuthorizationService.PLANNER_PLAN);
        authorizationService.require(user, PlannerAuthorizationService.PLANNER_IMPORT);
        // External Agent Runtime / model call — must not run inside a DB transaction.
        PlannerResponse planner = plannerService.plan(request, user);
        String runName = request.runName() == null || request.runName().isBlank()
                ? truncateName(planner.plan().objective())
                : request.runName().trim();
        RunResponse draft = planImporter.importPlan(
                new ImportPlanRequest(request.projectId(), runName, planner.plan()), user);
        return new PlanAndCreateResponse(planner, draft);
    }

    public RunResponse importPlan(ImportPlanRequest request, AuthenticatedUser user) {
        return planImporter.importPlan(request, user);
    }

    private static String truncateName(String objective) {
        String name = "Plan: " + objective.trim();
        return name.length() <= 255 ? name : name.substring(0, 255);
    }
}
