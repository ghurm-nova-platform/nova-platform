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

import ai.nova.platform.identity.dto.IdentityDtos.CreateUserRequest;
import ai.nova.platform.identity.dto.IdentityDtos.AdminResetPasswordRequest;
import ai.nova.platform.identity.dto.IdentityDtos.UpdateUserRequest;
import ai.nova.platform.identity.dto.IdentityDtos.UserView;
import ai.nova.platform.identity.security.IdentityAuthorizationService;
import ai.nova.platform.identity.service.IdentityUserService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/identity/users")
public class IdentityUserController {

    private final IdentityUserService identityUserService;
    private final IdentityAuthorizationService authorizationService;

    public IdentityUserController(
            IdentityUserService identityUserService, IdentityAuthorizationService authorizationService) {
        this.identityUserService = identityUserService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<UserView> listUsers(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireUserAdmin(user);
        return identityUserService.listUsers(user.getOrganizationId());
    }

    @PostMapping
    public UserView createUser(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CreateUserRequest request) {
        authorizationService.requireUserAdmin(user);
        return identityUserService.createUser(user.getOrganizationId(), request);
    }

    @GetMapping("/{id}")
    public UserView getUser(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        authorizationService.requireUserAdmin(user);
        return identityUserService.getUser(user.getOrganizationId(), id);
    }

    @PutMapping("/{id}")
    public UserView updateUser(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        authorizationService.requireUserAdmin(user);
        return identityUserService.updateUser(user.getOrganizationId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        authorizationService.requireUserAdmin(user);
        identityUserService.deleteUser(user.getOrganizationId(), id);
    }

    @PostMapping("/{id}/enable")
    public UserView enableUser(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        authorizationService.requireUserAdmin(user);
        return identityUserService.enableUser(user.getOrganizationId(), id);
    }

    @PostMapping("/{id}/disable")
    public UserView disableUser(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        authorizationService.requireUserAdmin(user);
        return identityUserService.disableUser(user.getOrganizationId(), id);
    }

    @PostMapping("/{id}/unlock")
    public UserView unlockUser(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        authorizationService.requireUserAdmin(user);
        return identityUserService.unlockUser(user.getOrganizationId(), id);
    }

    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        authorizationService.requireUserAdmin(user);
        identityUserService.resetPassword(user.getOrganizationId(), id, request.newPassword());
    }
}
