package ai.nova.platform.approval.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.approval.dto.ApprovalDtos.ApprovalPolicyView;
import ai.nova.platform.approval.dto.ApprovalDtos.CreateApprovalPolicyRequest;
import ai.nova.platform.approval.dto.ApprovalDtos.CreateApprovalPolicyVersionRequest;
import ai.nova.platform.approval.service.ApprovalPolicyAdminService;
import ai.nova.platform.security.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/approval-policies")
@Validated
public class ApprovalPolicyController {

    private final ApprovalPolicyAdminService policyAdminService;

    public ApprovalPolicyController(ApprovalPolicyAdminService policyAdminService) {
        this.policyAdminService = policyAdminService;
    }

    @GetMapping
    public List<ApprovalPolicyView> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return policyAdminService.list(user);
    }

    @GetMapping("/{id}")
    public ApprovalPolicyView get(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return policyAdminService.get(id, user);
    }

    @PostMapping
    public ApprovalPolicyView create(
            @Valid @RequestBody CreateApprovalPolicyRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return policyAdminService.create(request, user);
    }

    @PostMapping("/{id}/versions")
    public ApprovalPolicyView createVersion(
            @PathVariable UUID id,
            @Valid @RequestBody CreateApprovalPolicyVersionRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return policyAdminService.createVersion(id, request, user);
    }

    @PostMapping("/{id}/activate")
    public ApprovalPolicyView activate(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return policyAdminService.activate(id, user);
    }
}
