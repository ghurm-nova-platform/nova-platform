package ai.nova.platform.policy.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.policy.dto.PolicyDtos.CreatePolicyRequest;
import ai.nova.platform.policy.dto.PolicyDtos.EvaluatePolicyRequest;
import ai.nova.platform.policy.dto.PolicyDtos.Policy;
import ai.nova.platform.policy.service.ReleasePolicyService;
import ai.nova.platform.security.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/policies")
@Validated
public class PolicyController {

    private final ReleasePolicyService releasePolicyService;

    public PolicyController(ReleasePolicyService releasePolicyService) {
        this.releasePolicyService = releasePolicyService;
    }

    @PostMapping
    public Policy create(
            @Valid @RequestBody CreatePolicyRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return releasePolicyService.create(request, user);
    }

    @PostMapping("/{id}/evaluate")
    public Policy evaluate(
            @PathVariable("id") UUID id,
            @Valid @RequestBody EvaluatePolicyRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return releasePolicyService.evaluate(id, request, user);
    }

    @PostMapping("/{id}/enable")
    public Policy enable(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return releasePolicyService.enable(id, user);
    }

    @PostMapping("/{id}/disable")
    public Policy disable(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return releasePolicyService.disable(id, user);
    }

    @GetMapping
    public List<Policy> list(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return releasePolicyService.list(projectId, user);
    }

    @GetMapping("/{id}")
    public Policy get(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return releasePolicyService.get(id, user);
    }

    @GetMapping("/{id}/history")
    public Policy history(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return releasePolicyService.history(id, user);
    }
}
