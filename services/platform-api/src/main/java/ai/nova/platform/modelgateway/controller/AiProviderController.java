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

import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.CreateProviderRequest;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.ProviderAdaptersResponse;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.ProviderResponse;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.UpdateProviderRequest;
import ai.nova.platform.modelgateway.entity.AiProviderStatus;
import ai.nova.platform.modelgateway.service.AiProviderService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/model-providers")
public class AiProviderController {

    private final AiProviderService providerService;

    public AiProviderController(AiProviderService providerService) {
        this.providerService = providerService;
    }

    @GetMapping
    public Page<ProviderResponse> list(
            @RequestParam(required = false) AiProviderStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return providerService.list(status, search, pageable, user);
    }

    @GetMapping("/adapters")
    public ProviderAdaptersResponse adapters(@AuthenticationPrincipal AuthenticatedUser user) {
        return providerService.listAdapters(user);
    }

    @GetMapping("/{providerId}")
    public ProviderResponse get(@PathVariable UUID providerId, @AuthenticationPrincipal AuthenticatedUser user) {
        return providerService.get(providerId, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProviderResponse create(
            @Valid @RequestBody CreateProviderRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return providerService.create(request, user);
    }

    @PutMapping("/{providerId}")
    public ProviderResponse update(
            @PathVariable UUID providerId,
            @Valid @RequestBody UpdateProviderRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return providerService.update(providerId, request, user);
    }

    @PostMapping("/{providerId}/activate")
    public ProviderResponse activate(@PathVariable UUID providerId, @AuthenticationPrincipal AuthenticatedUser user) {
        return providerService.activate(providerId, user);
    }

    @PostMapping("/{providerId}/disable")
    public ProviderResponse disable(@PathVariable UUID providerId, @AuthenticationPrincipal AuthenticatedUser user) {
        return providerService.disable(providerId, user);
    }

    @PostMapping("/{providerId}/archive")
    public ProviderResponse archive(@PathVariable UUID providerId, @AuthenticationPrincipal AuthenticatedUser user) {
        return providerService.archive(providerId, user);
    }
}
