package ai.nova.platform.llm.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.llm.dto.LlmDtos.ConfigEntryView;
import ai.nova.platform.llm.dto.LlmDtos.ConfigResponse;
import ai.nova.platform.llm.dto.LlmDtos.HealthResponse;
import ai.nova.platform.llm.dto.LlmDtos.MetricsSummaryResponse;
import ai.nova.platform.llm.dto.LlmDtos.SetConfigRequest;
import ai.nova.platform.llm.service.HealthCheckService;
import ai.nova.platform.llm.service.LlmAuthorizationService;
import ai.nova.platform.llm.service.LlmRuntimeInfoService;
import ai.nova.platform.llm.service.RuntimeConfigurationService;
import ai.nova.platform.security.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/llm")
public class LlmController {

    private final LlmRuntimeInfoService runtimeInfoService;
    private final HealthCheckService healthCheckService;
    private final RuntimeConfigurationService runtimeConfigurationService;
    private final LlmAuthorizationService authorizationService;

    public LlmController(
            LlmRuntimeInfoService runtimeInfoService,
            HealthCheckService healthCheckService,
            RuntimeConfigurationService runtimeConfigurationService,
            LlmAuthorizationService authorizationService) {
        this.runtimeInfoService = runtimeInfoService;
        this.healthCheckService = healthCheckService;
        this.runtimeConfigurationService = runtimeConfigurationService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/config")
    public ConfigResponse config(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return runtimeInfoService.config();
    }

    @GetMapping("/runtime-config")
    public List<ConfigEntryView> runtimeConfig(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return runtimeConfigurationService.list(user.getOrganizationId());
    }

    @PostMapping("/runtime-config")
    public ConfigEntryView setRuntimeConfig(
            @Valid @RequestBody SetConfigRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireAdmin(user);
        return runtimeConfigurationService.set(request.key(), request.value(), user);
    }

    @GetMapping("/health")
    public HealthResponse health(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return new HealthResponse(healthCheckService.checkAll(user.getOrganizationId()));
    }

    @GetMapping("/metrics")
    public MetricsSummaryResponse metrics(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return new MetricsSummaryResponse(runtimeInfoService.metricsSummary());
    }
}
