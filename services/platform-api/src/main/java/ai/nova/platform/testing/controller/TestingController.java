package ai.nova.platform.testing.controller;

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

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.testing.dto.TestingDtos.TestingResult;
import ai.nova.platform.testing.dto.TestingDtos.TestingRunRequest;
import ai.nova.platform.testing.service.TestingAgentService;

@RestController
@RequestMapping("/api/testing")
@Validated
public class TestingController {

    private final TestingAgentService testingAgentService;

    public TestingController(TestingAgentService testingAgentService) {
        this.testingAgentService = testingAgentService;
    }

    @PostMapping("/run")
    public TestingResult run(
            @Valid @RequestBody TestingRunRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return testingAgentService.run(request, user);
    }

    @GetMapping("/{taskId}")
    public TestingResult getLatest(
            @PathVariable UUID taskId, @AuthenticationPrincipal AuthenticatedUser user) {
        return testingAgentService.getLatest(taskId, user);
    }
}
