package ai.nova.platform.repair.controller;

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

import jakarta.validation.Valid;

import ai.nova.platform.repair.dto.RepairDtos.RepairOperation;
import ai.nova.platform.repair.dto.RepairDtos.RepairRunRequest;
import ai.nova.platform.repair.service.RepairAgentService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/repair")
@Validated
public class RepairController {

    private final RepairAgentService repairAgentService;

    public RepairController(RepairAgentService repairAgentService) {
        this.repairAgentService = repairAgentService;
    }

    @PostMapping("/run")
    public RepairOperation run(@Valid @RequestBody RepairRunRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return repairAgentService.run(request, user);
    }

    @GetMapping("/{taskId}")
    public RepairOperation getLatest(@PathVariable UUID taskId, @AuthenticationPrincipal AuthenticatedUser user) {
        return repairAgentService.getLatest(taskId, user);
    }

    @GetMapping("/{taskId}/history")
    public List<RepairOperation> getHistory(@PathVariable UUID taskId, @AuthenticationPrincipal AuthenticatedUser user) {
        return repairAgentService.getHistory(taskId, user);
    }
}
