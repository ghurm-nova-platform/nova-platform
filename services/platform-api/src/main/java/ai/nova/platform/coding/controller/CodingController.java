package ai.nova.platform.coding.controller;

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

import ai.nova.platform.coding.dto.CodingDtos.CodeGenerationRequest;
import ai.nova.platform.coding.dto.CodingDtos.CodingResult;
import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.coding.service.CodingAgentService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/coding")
@Validated
public class CodingController {

    private final CodingAgentService codingAgentService;

    public CodingController(CodingAgentService codingAgentService) {
        this.codingAgentService = codingAgentService;
    }

    @PostMapping("/generate")
    public CodingResult generate(
            @Valid @RequestBody CodeGenerationRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return codingAgentService.generate(request, user);
    }

    @GetMapping("/artifacts/{taskId}")
    public List<GeneratedArtifactResponse> artifacts(
            @PathVariable UUID taskId, @AuthenticationPrincipal AuthenticatedUser user) {
        return codingAgentService.listArtifacts(taskId, user);
    }
}
