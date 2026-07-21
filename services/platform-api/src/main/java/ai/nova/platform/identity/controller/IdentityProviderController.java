package ai.nova.platform.identity.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

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

import ai.nova.platform.identity.dto.IdentityDtos.CreateProviderRequest;
import ai.nova.platform.identity.dto.IdentityDtos.ProviderTestResponse;
import ai.nova.platform.identity.dto.IdentityDtos.ProviderView;
import ai.nova.platform.identity.dto.IdentityDtos.UpdateProviderRequest;
import ai.nova.platform.identity.security.IdentityAuthorizationService;
import ai.nova.platform.identity.service.IdentityProviderService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/identity/providers")
public class IdentityProviderController {

    private final IdentityProviderService identityProviderService;
    private final IdentityAuthorizationService authorizationService;

    public IdentityProviderController(
            IdentityProviderService identityProviderService, IdentityAuthorizationService authorizationService) {
        this.identityProviderService = identityProviderService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<ProviderView> listProviders(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return identityProviderService.listProviders(user.getOrganizationId());
    }

    @PostMapping
    public ProviderView createProvider(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CreateProviderRequest request) {
        authorizationService.requireProviderAdmin(user);
        return identityProviderService.createProvider(user.getOrganizationId(), request);
    }

    @GetMapping("/{id}")
    public ProviderView getProvider(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        authorizationService.requireRead(user);
        return identityProviderService.getProvider(user.getOrganizationId(), id);
    }

    @PutMapping("/{id}")
    public ProviderView updateProvider(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProviderRequest request) {
        authorizationService.requireProviderAdmin(user);
        return identityProviderService.updateProvider(user.getOrganizationId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProvider(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        authorizationService.requireProviderAdmin(user);
        identityProviderService.deleteProvider(user.getOrganizationId(), id);
    }

    @PostMapping("/{id}/test")
    public ProviderTestResponse testProvider(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        authorizationService.requireProviderAdmin(user);
        return identityProviderService.testProvider(user.getOrganizationId(), id);
    }

    @PostMapping("/{id}/sync")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void syncProvider(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        authorizationService.requireProviderAdmin(user);
        identityProviderService.syncProvider(user.getOrganizationId(), id);
    }
}
