package ai.nova.platform.llm.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.llm.dto.LlmDtos.ProviderStatusView;
import ai.nova.platform.llm.service.HealthCheckService;
import ai.nova.platform.llm.service.LlmAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/llm/providers")
public class LlmProviderController {

    private final HealthCheckService healthCheckService;
    private final LlmAuthorizationService authorizationService;

    public LlmProviderController(
            HealthCheckService healthCheckService, LlmAuthorizationService authorizationService) {
        this.healthCheckService = healthCheckService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<ProviderStatusView> list(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return healthCheckService.list(user.getOrganizationId());
    }

    @PostMapping("/health")
    public List<ProviderStatusView> health(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return healthCheckService.checkAll(user.getOrganizationId());
    }
}
