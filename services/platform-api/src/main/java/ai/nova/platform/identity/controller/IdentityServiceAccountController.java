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

import ai.nova.platform.identity.dto.IdentityDtos.CreateServiceAccountRequest;
import ai.nova.platform.identity.dto.IdentityDtos.ServiceAccountCreateResponse;
import ai.nova.platform.identity.dto.IdentityDtos.ServiceAccountView;
import ai.nova.platform.identity.dto.IdentityDtos.UpdateServiceAccountRequest;
import ai.nova.platform.identity.security.IdentityAuthorizationService;
import ai.nova.platform.identity.service.ServiceAccountService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/identity/service-accounts")
public class IdentityServiceAccountController {

    private final ServiceAccountService serviceAccountService;
    private final IdentityAuthorizationService authorizationService;

    public IdentityServiceAccountController(
            ServiceAccountService serviceAccountService, IdentityAuthorizationService authorizationService) {
        this.serviceAccountService = serviceAccountService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<ServiceAccountView> listServiceAccounts(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireAdmin(user);
        return serviceAccountService.listServiceAccounts(user.getOrganizationId());
    }

    @PostMapping
    public ServiceAccountCreateResponse createServiceAccount(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CreateServiceAccountRequest request) {
        authorizationService.requireAdmin(user);
        return serviceAccountService.createServiceAccount(user.getOrganizationId(), request);
    }

    @PutMapping("/{id}")
    public ServiceAccountView updateServiceAccount(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServiceAccountRequest request) {
        authorizationService.requireAdmin(user);
        return serviceAccountService.updateServiceAccount(user.getOrganizationId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteServiceAccount(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        authorizationService.requireAdmin(user);
        serviceAccountService.deleteServiceAccount(user.getOrganizationId(), id);
    }
}
