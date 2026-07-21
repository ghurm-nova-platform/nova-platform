package ai.nova.platform.identity.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.identity.dto.IdentityDtos.ConfigResponse;
import ai.nova.platform.identity.dto.IdentityDtos.DashboardView;
import ai.nova.platform.identity.dto.IdentityDtos.LoginHistoryView;
import ai.nova.platform.identity.dto.IdentityDtos.SecurityEventView;
import ai.nova.platform.identity.dto.IdentityDtos.SummaryView;
import ai.nova.platform.identity.security.IdentityAuthorizationService;
import ai.nova.platform.identity.service.IdentityExportService;
import ai.nova.platform.identity.service.IdentityExportService.ExportPayload;
import ai.nova.platform.identity.service.IdentityService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/identity")
public class IdentityController {

    private final IdentityService identityService;
    private final IdentityExportService identityExportService;
    private final IdentityAuthorizationService authorizationService;

    public IdentityController(
            IdentityService identityService,
            IdentityExportService identityExportService,
            IdentityAuthorizationService authorizationService) {
        this.identityService = identityService;
        this.identityExportService = identityExportService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/config")
    public ConfigResponse config(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return identityService.getConfig();
    }

    @GetMapping("/summary")
    public SummaryView summary(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return identityService.summary(user.getOrganizationId());
    }

    @GetMapping("/dashboard")
    public DashboardView dashboard(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return identityService.dashboard(user.getOrganizationId());
    }

    @GetMapping("/security-events")
    public List<SecurityEventView> securityEvents(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireAuditRead(user);
        return identityService.securityEvents(user.getOrganizationId());
    }

    @GetMapping("/login-history")
    public List<LoginHistoryView> loginHistory(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireAuditRead(user);
        return identityService.loginHistory(user.getOrganizationId());
    }

    @GetMapping("/export/{resource}")
    public ResponseEntity<byte[]> exportGet(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String resource,
            @RequestParam(defaultValue = "csv") String format) {
        authorizationService.requireRead(user);
        ExportPayload payload = identityExportService.export(user.getOrganizationId(), resource, format);
        return toExportResponse(payload);
    }

    @PostMapping("/export/{resource}")
    public ResponseEntity<byte[]> exportPost(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String resource,
            @RequestParam(defaultValue = "csv") String format) {
        authorizationService.requireRead(user);
        ExportPayload payload = identityExportService.export(user.getOrganizationId(), resource, format);
        return toExportResponse(payload);
    }

    private ResponseEntity<byte[]> toExportResponse(ExportPayload payload) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + payload.filename() + "\"")
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .body(payload.content());
    }
}
