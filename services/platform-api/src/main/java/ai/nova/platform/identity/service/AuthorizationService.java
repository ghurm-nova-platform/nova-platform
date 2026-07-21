package ai.nova.platform.identity.service;

import org.springframework.stereotype.Service;

import ai.nova.platform.identity.security.IdentityAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;

@Service
public class AuthorizationService {

    private final IdentityAuthorizationService identityAuthorizationService;

    public AuthorizationService(IdentityAuthorizationService identityAuthorizationService) {
        this.identityAuthorizationService = identityAuthorizationService;
    }

    public void requireIdentityRead(AuthenticatedUser user) {
        identityAuthorizationService.requireRead(user);
    }

    public void requireIdentityAdmin(AuthenticatedUser user) {
        identityAuthorizationService.requireAdmin(user);
    }
}
