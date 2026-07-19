package ai.nova.platform.ci.controller;

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

import ai.nova.platform.ci.dto.CiDtos.CiObservationOperation;
import ai.nova.platform.ci.dto.CiDtos.CiRunRequest;
import ai.nova.platform.ci.service.CiObservationAgentService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/ci")
@Validated
public class CiController {

    private final CiObservationAgentService ciObservationAgentService;

    public CiController(CiObservationAgentService ciObservationAgentService) {
        this.ciObservationAgentService = ciObservationAgentService;
    }

    @PostMapping("/run")
    public CiObservationOperation run(
            @Valid @RequestBody CiRunRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return ciObservationAgentService.run(request, user);
    }

    @GetMapping("/{taskId}")
    public CiObservationOperation getLatest(
            @PathVariable UUID taskId, @AuthenticationPrincipal AuthenticatedUser user) {
        return ciObservationAgentService.getLatest(taskId, user);
    }

    @GetMapping("/{taskId}/history")
    public List<CiObservationOperation> getHistory(
            @PathVariable UUID taskId, @AuthenticationPrincipal AuthenticatedUser user) {
        return ciObservationAgentService.getHistory(taskId, user);
    }
}
