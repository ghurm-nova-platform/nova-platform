package ai.nova.platform.modelcatalog.controller;

import java.util.List;
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

import ai.nova.platform.modelcatalog.dto.ModelCatalogDtos.AliasResponse;
import ai.nova.platform.modelcatalog.dto.ModelCatalogDtos.CatalogModelResponse;
import ai.nova.platform.modelcatalog.dto.ModelCatalogDtos.CreateAliasRequest;
import ai.nova.platform.modelcatalog.dto.ModelCatalogDtos.CreateCatalogModelRequest;
import ai.nova.platform.modelcatalog.dto.ModelCatalogDtos.ReplaceCapabilitiesRequest;
import ai.nova.platform.modelcatalog.dto.ModelCatalogDtos.UpdateCatalogModelRequest;
import ai.nova.platform.modelcatalog.entity.AiModelCapability;
import ai.nova.platform.modelcatalog.service.AiModelCatalogService;
import ai.nova.platform.modelcatalog.service.ModelAliasService;
import ai.nova.platform.modelgateway.entity.AiModelSource;
import ai.nova.platform.modelgateway.entity.AiModelStatus;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/ai-models")
public class AiModelCatalogController {

    private final AiModelCatalogService catalogService;
    private final ModelAliasService aliasService;

    public AiModelCatalogController(AiModelCatalogService catalogService, ModelAliasService aliasService) {
        this.catalogService = catalogService;
        this.aliasService = aliasService;
    }

    @GetMapping
    public Page<CatalogModelResponse> list(
            @RequestParam(required = false) AiModelStatus status,
            @RequestParam(required = false) UUID providerId,
            @RequestParam(required = false) AiModelSource source,
            @RequestParam(required = false) AiModelCapability capability,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return catalogService.list(status, providerId, source, capability, search, pageable, user);
    }

    @GetMapping("/{modelId}")
    public CatalogModelResponse get(@PathVariable UUID modelId, @AuthenticationPrincipal AuthenticatedUser user) {
        return catalogService.get(modelId, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogModelResponse create(
            @Valid @RequestBody CreateCatalogModelRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return catalogService.create(request, user);
    }

    @PutMapping("/{modelId}")
    public CatalogModelResponse update(
            @PathVariable UUID modelId,
            @Valid @RequestBody UpdateCatalogModelRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return catalogService.update(modelId, request, user);
    }

    @PostMapping("/{modelId}/activate")
    public CatalogModelResponse activate(
            @PathVariable UUID modelId, @AuthenticationPrincipal AuthenticatedUser user) {
        return catalogService.activate(modelId, user);
    }

    @PostMapping("/{modelId}/disable")
    public CatalogModelResponse disable(
            @PathVariable UUID modelId, @AuthenticationPrincipal AuthenticatedUser user) {
        return catalogService.disable(modelId, user);
    }

    @PostMapping("/{modelId}/deprecate")
    public CatalogModelResponse deprecate(
            @PathVariable UUID modelId, @AuthenticationPrincipal AuthenticatedUser user) {
        return catalogService.deprecate(modelId, user);
    }

    @PostMapping("/{modelId}/archive")
    public CatalogModelResponse archive(
            @PathVariable UUID modelId, @AuthenticationPrincipal AuthenticatedUser user) {
        return catalogService.archive(modelId, user);
    }

    @PutMapping("/{modelId}/capabilities")
    public CatalogModelResponse replaceCapabilities(
            @PathVariable UUID modelId,
            @Valid @RequestBody ReplaceCapabilitiesRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return catalogService.replaceCapabilities(modelId, request, user);
    }

    @GetMapping("/{modelId}/aliases")
    public List<AliasResponse> listAliases(
            @PathVariable UUID modelId, @AuthenticationPrincipal AuthenticatedUser user) {
        return aliasService.list(modelId, user);
    }

    @PostMapping("/{modelId}/aliases")
    @ResponseStatus(HttpStatus.CREATED)
    public AliasResponse createAlias(
            @PathVariable UUID modelId,
            @Valid @RequestBody CreateAliasRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return aliasService.create(modelId, request, user);
    }
}
