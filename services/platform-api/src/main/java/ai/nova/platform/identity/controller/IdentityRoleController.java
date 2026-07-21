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

import ai.nova.platform.identity.dto.IdentityDtos.CreateRoleRequest;
import ai.nova.platform.identity.dto.IdentityDtos.RoleView;
import ai.nova.platform.identity.dto.IdentityDtos.UpdateRoleRequest;
import ai.nova.platform.identity.security.IdentityAuthorizationService;
import ai.nova.platform.identity.service.RoleService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/identity/roles")
public class IdentityRoleController {

    private final RoleService roleService;
    private final IdentityAuthorizationService authorizationService;

    public IdentityRoleController(RoleService roleService, IdentityAuthorizationService authorizationService) {
        this.roleService = roleService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<RoleView> listRoles(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return roleService.listRoles(user.getOrganizationId());
    }

    @PostMapping
    public RoleView createRole(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CreateRoleRequest request) {
        authorizationService.requireRoleAdmin(user);
        return roleService.createRole(user.getOrganizationId(), request);
    }

    @GetMapping("/{id}")
    public RoleView getRole(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        authorizationService.requireRead(user);
        return roleService.getRole(user.getOrganizationId(), id);
    }

    @PutMapping("/{id}")
    public RoleView updateRole(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request) {
        authorizationService.requireRoleAdmin(user);
        return roleService.updateRole(user.getOrganizationId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRole(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        authorizationService.requireRoleAdmin(user);
        roleService.deleteRole(user.getOrganizationId(), id);
    }
}
