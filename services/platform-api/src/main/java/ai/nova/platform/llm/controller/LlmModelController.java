package ai.nova.platform.llm.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.llm.dto.LlmDtos.ModelView;
import ai.nova.platform.llm.dto.LlmDtos.RegisterModelRequest;
import ai.nova.platform.llm.dto.LlmDtos.UpdateModelRequest;
import ai.nova.platform.llm.service.LlmAuthorizationService;
import ai.nova.platform.llm.service.ModelLifecycleService;
import ai.nova.platform.llm.service.ModelRegistryService;
import ai.nova.platform.security.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/llm/models")
public class LlmModelController {

    private final ModelRegistryService modelRegistryService;
    private final ModelLifecycleService modelLifecycleService;
    private final LlmAuthorizationService authorizationService;

    public LlmModelController(
            ModelRegistryService modelRegistryService,
            ModelLifecycleService modelLifecycleService,
            LlmAuthorizationService authorizationService) {
        this.modelRegistryService = modelRegistryService;
        this.modelLifecycleService = modelLifecycleService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<ModelView> list(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return modelRegistryService.list(user.getOrganizationId());
    }

    @GetMapping("/{id}")
    public ModelView get(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return modelRegistryService.get(user.getOrganizationId(), id);
    }

    @PostMapping
    public ModelView register(
            @Valid @RequestBody RegisterModelRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireModelAdmin(user);
        return modelRegistryService.register(request, user);
    }

    @PutMapping("/{id}")
    public ModelView update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateModelRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireModelAdmin(user);
        return modelRegistryService.update(id, request, user);
    }

    @PostMapping("/{id}/enable")
    public ModelView enable(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireModelAdmin(user);
        return modelRegistryService.enable(id, user);
    }

    @PostMapping("/{id}/disable")
    public ModelView disable(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireModelAdmin(user);
        return modelRegistryService.disable(id, user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireModelAdmin(user);
        modelRegistryService.delete(id, user);
    }

    @PostMapping("/{id}/install")
    public ModelView install(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireModelAdmin(user);
        return modelLifecycleService.install(id, user);
    }

    @PostMapping("/{id}/download")
    public ModelView download(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireModelAdmin(user);
        return modelLifecycleService.download(id, user);
    }

    @PostMapping("/{id}/load")
    public ModelView load(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireModelAdmin(user);
        return modelLifecycleService.load(id, user);
    }

    @PostMapping("/{id}/unload")
    public ModelView unload(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireModelAdmin(user);
        return modelLifecycleService.unload(id, user);
    }

    @PostMapping("/{id}/start")
    public ModelView start(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireModelAdmin(user);
        return modelLifecycleService.start(id, user);
    }

    @PostMapping("/{id}/stop")
    public ModelView stop(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireModelAdmin(user);
        return modelLifecycleService.stop(id, user);
    }

    @PostMapping("/{id}/restart")
    public ModelView restart(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireModelAdmin(user);
        return modelLifecycleService.restart(id, user);
    }

    @PostMapping("/{id}/warmup")
    public ModelView warmup(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireModelAdmin(user);
        return modelLifecycleService.warmup(id, user);
    }

    @GetMapping("/{id}/health")
    public Map<String, Object> health(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return modelLifecycleService.health(id, user);
    }
}
