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

import ai.nova.platform.approval.dto.ApprovalDtos.ApprovalDecision;
import ai.nova.platform.approval.dto.ApprovalDtos.ApprovalRequirement;
import ai.nova.platform.approval.dto.ApprovalDtos.ApprovalRunRequest;
import ai.nova.platform.approval.dto.ApprovalDtos.HumanActionRequest;
import ai.nova.platform.approval.service.ApprovalGateService;
import ai.nova.platform.security.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/approval-gate")
@Validated
public class ApprovalGateController {

    private final ApprovalGateService approvalGateService;

    public ApprovalGateController(ApprovalGateService approvalGateService) {
        this.approvalGateService = approvalGateService;
    }

    @PostMapping("/run")
    public ApprovalDecision run(
            @Valid @RequestBody ApprovalRunRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return approvalGateService.run(request, user);
    }

    @GetMapping("/{taskId}")
    public ApprovalDecision getLatest(
            @PathVariable UUID taskId, @AuthenticationPrincipal AuthenticatedUser user) {
        return approvalGateService.getLatest(taskId, user);
    }

    @GetMapping("/{taskId}/history")
    public List<ApprovalDecision> getHistory(
            @PathVariable UUID taskId, @AuthenticationPrincipal AuthenticatedUser user) {
        return approvalGateService.getHistory(taskId, user);
    }

    @GetMapping("/{taskId}/requirements")
    public List<ApprovalRequirement> getRequirements(
            @PathVariable UUID taskId, @AuthenticationPrincipal AuthenticatedUser user) {
        return approvalGateService.getRequirements(taskId, user);
    }

    @PostMapping("/{taskId}/approve")
    public ApprovalDecision approve(
            @PathVariable UUID taskId,
            @RequestBody(required = false) HumanActionRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return approvalGateService.approve(taskId, request, user);
    }

    @PostMapping("/{taskId}/reject")
    public ApprovalDecision reject(
            @PathVariable UUID taskId,
            @Valid @RequestBody HumanActionRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return approvalGateService.reject(taskId, request, user);
    }
}
