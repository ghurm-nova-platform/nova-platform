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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.identity.dto.IdentityDtos.ApiTokenCreateResponse;
import ai.nova.platform.identity.dto.IdentityDtos.ApiTokenView;
import ai.nova.platform.identity.dto.IdentityDtos.CreateApiTokenRequest;
import ai.nova.platform.identity.entity.IdentityUserEntity;
import ai.nova.platform.identity.error.IdentityErrorCodes;
import ai.nova.platform.identity.repository.IdentityUserRepository;
import ai.nova.platform.identity.security.IdentityAuthorizationService;
import ai.nova.platform.identity.service.ApiTokenService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@RestController
@RequestMapping("/api/identity/api-tokens")
public class IdentityApiTokenController {

    private final ApiTokenService apiTokenService;
    private final IdentityAuthorizationService authorizationService;
    private final IdentityUserRepository identityUserRepository;

    public IdentityApiTokenController(
            ApiTokenService apiTokenService,
            IdentityAuthorizationService authorizationService,
            IdentityUserRepository identityUserRepository) {
        this.apiTokenService = apiTokenService;
        this.authorizationService = authorizationService;
        this.identityUserRepository = identityUserRepository;
    }

    @GetMapping
    public List<ApiTokenView> listTokens(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireAdmin(user);
        return apiTokenService.listTokens(user.getOrganizationId());
    }

    @PostMapping
    public ApiTokenCreateResponse createToken(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CreateApiTokenRequest request) {
        authorizationService.requireAdmin(user);
        UUID identityUserId = requireIdentityUserId(user);
        return apiTokenService.createToken(user.getOrganizationId(), identityUserId, request.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteToken(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        authorizationService.requireAdmin(user);
        apiTokenService.deleteToken(user.getOrganizationId(), id);
    }

    @PostMapping("/{id}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeToken(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        authorizationService.requireAdmin(user);
        apiTokenService.revokeToken(user.getOrganizationId(), id);
    }

    private UUID requireIdentityUserId(AuthenticatedUser user) {
        return identityUserRepository.findByPlatformUserId(user.getUserId())
                .map(IdentityUserEntity::getId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, IdentityErrorCodes.IDENTITY_USER_NOT_FOUND, "Identity user not found"));
    }
}
