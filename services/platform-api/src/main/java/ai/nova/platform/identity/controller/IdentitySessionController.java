package ai.nova.platform.identity.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.identity.dto.IdentityDtos.SessionView;
import ai.nova.platform.identity.security.IdentityAuthorizationService;
import ai.nova.platform.identity.service.SessionService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/identity/sessions")
public class IdentitySessionController {

    private final SessionService sessionService;
    private final IdentityAuthorizationService authorizationService;

    public IdentitySessionController(
            SessionService sessionService, IdentityAuthorizationService authorizationService) {
        this.sessionService = sessionService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<SessionView> listSessions(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return sessionService.listActiveSessions(user.getOrganizationId());
    }

    @GetMapping("/{id}")
    public SessionView getSession(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        authorizationService.requireRead(user);
        return sessionService.getSession(user.getOrganizationId(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeSession(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        authorizationService.requireSessionAdmin(user);
        sessionService.revokeSession(id);
    }

    @DeleteMapping("/revoke-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeAllSessions(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireSessionAdmin(user);
        sessionService.revokeAllSessions(user.getOrganizationId());
    }
}
