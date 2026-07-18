package ai.nova.platform.modelgateway.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.CreateRoutingPolicyRequest;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.RoutingPolicyResponse;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.UpdateRoutingPolicyRequest;
import ai.nova.platform.modelgateway.entity.RoutingPolicyStatus;
import ai.nova.platform.modelgateway.service.ModelRoutingPolicyService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/projects/{projectId}/model-routing-policies")
public class ModelRoutingPolicyController {

    private final ModelRoutingPolicyService policyService;

    public ModelRoutingPolicyController(ModelRoutingPolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    public Page<RoutingPolicyResponse> list(
            @PathVariable UUID projectId,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) RoutingPolicyStatus status,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return policyService.list(projectId, agentId, status, pageable, user);
    }

    @GetMapping("/{policyId}")
    public RoutingPolicyResponse get(
            @PathVariable UUID projectId,
            @PathVariable UUID policyId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return policyService.get(projectId, policyId, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoutingPolicyResponse create(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateRoutingPolicyRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return policyService.create(projectId, request, user);
    }

    @PutMapping("/{policyId}")
    public RoutingPolicyResponse update(
            @PathVariable UUID projectId,
            @PathVariable UUID policyId,
            @Valid @RequestBody UpdateRoutingPolicyRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return policyService.update(projectId, policyId, request, user);
    }

    @PostMapping("/{policyId}/activate")
    public RoutingPolicyResponse activate(
            @PathVariable UUID projectId,
            @PathVariable UUID policyId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return policyService.activate(projectId, policyId, user);
    }

    @PostMapping("/{policyId}/archive")
    public RoutingPolicyResponse archive(
            @PathVariable UUID projectId,
            @PathVariable UUID policyId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return policyService.archive(projectId, policyId, user);
    }
}
