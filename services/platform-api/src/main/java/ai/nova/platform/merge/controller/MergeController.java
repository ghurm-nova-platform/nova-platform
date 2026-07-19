package ai.nova.platform.merge.controller;

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

import ai.nova.platform.merge.dto.MergeDtos.MergeOperation;
import ai.nova.platform.merge.dto.MergeDtos.MergeRunRequest;
import ai.nova.platform.merge.service.MergeAgentService;
import ai.nova.platform.security.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/merge")
@Validated
public class MergeController {

    private final MergeAgentService mergeAgentService;

    public MergeController(MergeAgentService mergeAgentService) {
        this.mergeAgentService = mergeAgentService;
    }

    @PostMapping("/run")
    public MergeOperation run(@Valid @RequestBody MergeRunRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return mergeAgentService.run(request, user);
    }

    @GetMapping("/{taskId}")
    public MergeOperation getLatest(@PathVariable UUID taskId, @AuthenticationPrincipal AuthenticatedUser user) {
        return mergeAgentService.getLatest(taskId, user);
    }

    @GetMapping("/{taskId}/history")
    public List<MergeOperation> getHistory(@PathVariable UUID taskId, @AuthenticationPrincipal AuthenticatedUser user) {
        return mergeAgentService.getHistory(taskId, user);
    }
}
