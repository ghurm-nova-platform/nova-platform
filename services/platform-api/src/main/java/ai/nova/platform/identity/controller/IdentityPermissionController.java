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

import ai.nova.platform.identity.dto.IdentityDtos.CreatePermissionRequest;
import ai.nova.platform.identity.dto.IdentityDtos.PermissionView;
import ai.nova.platform.identity.dto.IdentityDtos.UpdatePermissionRequest;
import ai.nova.platform.identity.security.IdentityAuthorizationService;
import ai.nova.platform.identity.service.PermissionService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/identity/permissions")
public class IdentityPermissionController {

    private final PermissionService permissionService;
    private final IdentityAuthorizationService authorizationService;

    public IdentityPermissionController(
            PermissionService permissionService, IdentityAuthorizationService authorizationService) {
        this.permissionService = permissionService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<PermissionView> listPermissions(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return permissionService.listPermissions(user.getOrganizationId());
    }

    @PostMapping
    public PermissionView createPermission(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CreatePermissionRequest request) {
        authorizationService.requirePermissionAdmin(user);
        return permissionService.createPermission(user.getOrganizationId(), request);
    }

    @GetMapping("/{id}")
    public PermissionView getPermission(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        authorizationService.requireRead(user);
        return permissionService.getPermission(user.getOrganizationId(), id);
    }

    @PutMapping("/{id}")
    public PermissionView updatePermission(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePermissionRequest request) {
        authorizationService.requirePermissionAdmin(user);
        return permissionService.updatePermission(user.getOrganizationId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermission(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        authorizationService.requirePermissionAdmin(user);
        permissionService.deletePermission(user.getOrganizationId(), id);
    }
}
