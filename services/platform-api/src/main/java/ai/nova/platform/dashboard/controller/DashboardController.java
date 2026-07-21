package ai.nova.platform.dashboard.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.audit.dto.AuditDtos.AuditSearchRequest;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSeverity;
import ai.nova.platform.dashboard.dto.DashboardDtos.ApprovalsSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.AuditSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.CiSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.CostSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.DashboardConfigResponse;
import ai.nova.platform.dashboard.dto.DashboardDtos.DashboardRefreshResponse;
import ai.nova.platform.dashboard.dto.DashboardDtos.DashboardSnapshot;
import ai.nova.platform.dashboard.dto.DashboardDtos.DeploymentsSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.EnvironmentsSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.OverviewSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.PipelineSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.ReleasesSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.RollbacksSection;
import ai.nova.platform.dashboard.service.DashboardExportService.ExportPayload;
import ai.nova.platform.dashboard.service.DashboardService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardSnapshot snapshot(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return dashboardService.getSnapshot(user, projectId);
    }

    @GetMapping("/config")
    public DashboardConfigResponse config(@AuthenticationPrincipal AuthenticatedUser user) {
        return dashboardService.getConfig(user);
    }

    @GetMapping("/overview")
    public OverviewSection overview(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return dashboardService.getOverview(user, projectId);
    }

    @GetMapping("/pipeline")
    public PipelineSection pipeline(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return dashboardService.getPipeline(user, projectId);
    }

    @GetMapping("/deployments")
    public DeploymentsSection deployments(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return dashboardService.getDeployments(user, projectId);
    }

    @GetMapping("/releases")
    public ReleasesSection releases(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return dashboardService.getReleases(user, projectId);
    }

    @GetMapping("/environments")
    public EnvironmentsSection environments(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return dashboardService.getEnvironments(user, projectId);
    }

    @GetMapping("/audit")
    public AuditSection audit(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @RequestParam(value = "from", required = false) Instant from,
            @RequestParam(value = "to", required = false) Instant to,
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestParam(value = "entityType", required = false) AuditEntityType entityType,
            @RequestParam(value = "entityId", required = false) UUID entityId,
            @RequestParam(value = "action", required = false) AuditAction action,
            @RequestParam(value = "severity", required = false) AuditSeverity severity,
            @RequestParam(value = "result", required = false) AuditResult result,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size,
            @AuthenticationPrincipal AuthenticatedUser user) {
        AuditSearchRequest filters = new AuditSearchRequest(
                from, to, projectId, userId, entityType, entityId, action, severity, result, null, null, page, size);
        return dashboardService.getAudit(user, projectId, filters);
    }

    @GetMapping("/approvals")
    public ApprovalsSection approvals(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return dashboardService.getApprovals(user, projectId);
    }

    @GetMapping("/ci")
    public CiSection ci(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return dashboardService.getCi(user, projectId);
    }

    @GetMapping("/rollbacks")
    public RollbacksSection rollbacks(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return dashboardService.getRollbacks(user, projectId);
    }

    @GetMapping("/cost")
    public CostSection cost(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return dashboardService.getCost(user, projectId);
    }

    @PostMapping("/refresh")
    public DashboardRefreshResponse refresh(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return dashboardService.refresh(user, projectId);
    }

    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> export(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @RequestParam(value = "format", defaultValue = "csv") String format,
            @RequestParam(value = "section", defaultValue = "overview") String section,
            @AuthenticationPrincipal AuthenticatedUser user) {
        ExportPayload payload = dashboardService.export(user, projectId, format, section);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + payload.filename() + "\"")
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .body(new ByteArrayResource(payload.content()));
    }
}
