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

import ai.nova.platform.identity.dto.IdentityDtos.CreateGroupRequest;
import ai.nova.platform.identity.dto.IdentityDtos.GroupView;
import ai.nova.platform.identity.dto.IdentityDtos.UpdateGroupRequest;
import ai.nova.platform.identity.security.IdentityAuthorizationService;
import ai.nova.platform.identity.service.GroupService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/identity/groups")
public class IdentityGroupController {

    private final GroupService groupService;
    private final IdentityAuthorizationService authorizationService;

    public IdentityGroupController(GroupService groupService, IdentityAuthorizationService authorizationService) {
        this.groupService = groupService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<GroupView> listGroups(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return groupService.listGroups(user.getOrganizationId());
    }

    @PostMapping
    public GroupView createGroup(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CreateGroupRequest request) {
        authorizationService.requireGroupAdmin(user);
        return groupService.createGroup(user.getOrganizationId(), request);
    }

    @GetMapping("/{id}")
    public GroupView getGroup(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        authorizationService.requireRead(user);
        return groupService.getGroup(user.getOrganizationId(), id);
    }

    @PutMapping("/{id}")
    public GroupView updateGroup(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGroupRequest request) {
        authorizationService.requireGroupAdmin(user);
        return groupService.updateGroup(user.getOrganizationId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        authorizationService.requireGroupAdmin(user);
        groupService.deleteGroup(user.getOrganizationId(), id);
    }

    @PostMapping("/{id}/sync")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void syncGroup(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        authorizationService.requireGroupAdmin(user);
        groupService.syncGroup(user.getOrganizationId(), id);
    }
}
