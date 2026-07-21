package ai.nova.platform.identity.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.identity.dto.IdentityDtos.ScimListResponse;
import ai.nova.platform.identity.dto.IdentityDtos.ScimUserRequest;
import ai.nova.platform.identity.dto.IdentityDtos.ScimUserResponse;
import ai.nova.platform.identity.entity.IdentityGroupEntity;
import ai.nova.platform.identity.scim.ScimGroupService;
import ai.nova.platform.identity.scim.ScimUserService;
import ai.nova.platform.identity.security.IdentityAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/scim/v2")
public class ScimController {

    private final ScimUserService scimUserService;
    private final ScimGroupService scimGroupService;
    private final IdentityAuthorizationService authorizationService;

    public ScimController(
            ScimUserService scimUserService,
            ScimGroupService scimGroupService,
            IdentityAuthorizationService authorizationService) {
        this.scimUserService = scimUserService;
        this.scimGroupService = scimGroupService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/Users")
    public ScimListResponse<ScimUserResponse> listUsers(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireScimProvision(user);
        List<ScimUserResponse> users = scimUserService.listUsers(user.getOrganizationId());
        return new ScimListResponse<>(users, users.size());
    }

    @PostMapping("/Users")
    public ScimUserResponse createUser(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody ScimUserRequest request) {
        authorizationService.requireScimProvision(user);
        return scimUserService.createUser(user.getOrganizationId(), request);
    }

    @GetMapping("/Groups")
    public ScimListResponse<Map<String, Object>> listGroups(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireScimProvision(user);
        List<Map<String, Object>> groups = scimGroupService.listGroups(user.getOrganizationId()).stream()
                .map(this::toGroupMap)
                .toList();
        return new ScimListResponse<>(groups, groups.size());
    }

    @PostMapping("/Groups")
    public Map<String, Object> createGroup(
            @AuthenticationPrincipal AuthenticatedUser user, @RequestBody Map<String, String> request) {
        authorizationService.requireScimProvision(user);
        IdentityGroupEntity group = scimGroupService.createGroup(
                user.getOrganizationId(), request.getOrDefault("displayName", "group"));
        return toGroupMap(group);
    }

    private Map<String, Object> toGroupMap(IdentityGroupEntity group) {
        return Map.of("id", group.getId().toString(), "displayName", group.getName());
    }
}
