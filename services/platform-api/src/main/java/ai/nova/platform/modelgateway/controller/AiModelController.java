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

import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.CreateModelRequest;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.ModelResponse;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.UpdateModelRequest;
import ai.nova.platform.modelgateway.entity.AiModelStatus;
import ai.nova.platform.modelgateway.service.AiModelService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/model-providers/{providerId}/models")
public class AiModelController {

    private final AiModelService modelService;

    public AiModelController(AiModelService modelService) {
        this.modelService = modelService;
    }

    @GetMapping
    public Page<ModelResponse> list(
            @PathVariable UUID providerId,
            @RequestParam(required = false) AiModelStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return modelService.list(providerId, status, search, pageable, user);
    }

    @GetMapping("/{modelId}")
    public ModelResponse get(
            @PathVariable UUID providerId,
            @PathVariable UUID modelId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return modelService.get(providerId, modelId, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ModelResponse create(
            @PathVariable UUID providerId,
            @Valid @RequestBody CreateModelRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return modelService.create(providerId, request, user);
    }

    @PutMapping("/{modelId}")
    public ModelResponse update(
            @PathVariable UUID providerId,
            @PathVariable UUID modelId,
            @Valid @RequestBody UpdateModelRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return modelService.update(providerId, modelId, request, user);
    }

    @PostMapping("/{modelId}/activate")
    public ModelResponse activate(
            @PathVariable UUID providerId,
            @PathVariable UUID modelId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return modelService.activate(providerId, modelId, user);
    }

    @PostMapping("/{modelId}/disable")
    public ModelResponse disable(
            @PathVariable UUID providerId,
            @PathVariable UUID modelId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return modelService.disable(providerId, modelId, user);
    }

    @PostMapping("/{modelId}/archive")
    public ModelResponse archive(
            @PathVariable UUID providerId,
            @PathVariable UUID modelId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return modelService.archive(providerId, modelId, user);
    }
}
