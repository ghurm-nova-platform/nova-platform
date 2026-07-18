package ai.nova.platform.modelgateway.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.ModelUsageResponse;
import ai.nova.platform.modelgateway.service.ModelUsageService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/projects/{projectId}/model-usage")
public class ModelUsageController {

    private final ModelUsageService usageService;

    public ModelUsageController(ModelUsageService usageService) {
        this.usageService = usageService;
    }

    @GetMapping
    public ModelUsageResponse getUsage(
            @PathVariable UUID projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return usageService.getUsage(projectId, from, to, user);
    }
}
