package ai.nova.platform.planner.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import ai.nova.platform.orchestration.dto.OrchestrationDtos.RunResponse;
import ai.nova.platform.planner.dto.PlannerDtos.ImportPlanRequest;
import ai.nova.platform.planner.dto.PlannerDtos.PlanAndCreateResponse;
import ai.nova.platform.planner.dto.PlannerDtos.PlannerRequest;
import ai.nova.platform.planner.dto.PlannerDtos.PlannerResponse;
import ai.nova.platform.planner.entity.PlannerTemplate;
import ai.nova.platform.planner.service.PlannerImportService;
import ai.nova.platform.planner.service.PlannerService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/planner")
@Validated
public class PlannerController {

    private final PlannerService plannerService;
    private final PlannerImportService importService;

    public PlannerController(PlannerService plannerService, PlannerImportService importService) {
        this.plannerService = plannerService;
        this.importService = importService;
    }

    @PostMapping("/plan")
    public PlannerResponse plan(
            @Valid @RequestBody PlannerRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return plannerService.plan(request, user);
    }

    @PostMapping("/plan-and-create")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanAndCreateResponse planAndCreate(
            @Valid @RequestBody PlannerRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return importService.planAndCreate(request, user);
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public RunResponse importPlan(
            @Valid @RequestBody ImportPlanRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return importService.importPlan(request, user);
    }

    @GetMapping("/templates")
    public List<TemplateResponse> templates(
            @RequestParam UUID projectId, @AuthenticationPrincipal AuthenticatedUser user) {
        return plannerService.listTemplates(projectId, user).stream().map(TemplateResponse::from).toList();
    }

    public record TemplateResponse(
            UUID id,
            UUID organizationId,
            UUID projectId,
            String name,
            String description,
            String templateType,
            boolean enabled) {
        static TemplateResponse from(PlannerTemplate template) {
            return new TemplateResponse(
                    template.getId(),
                    template.getOrganizationId(),
                    template.getProjectId(),
                    template.getName(),
                    template.getDescription(),
                    template.getTemplateType().name(),
                    template.isEnabled());
        }
    }
}
