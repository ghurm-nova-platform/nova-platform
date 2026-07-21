package ai.nova.platform.identity.controller;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.auth.AuthDtos.TokenResponse;
import ai.nova.platform.identity.dto.IdentityDtos.ConfigResponse;
import ai.nova.platform.identity.dto.IdentityDtos.CreateProviderRequest;
import ai.nova.platform.identity.dto.IdentityDtos.LoginHistoryView;
import ai.nova.platform.identity.dto.IdentityDtos.LoginRequest;
import ai.nova.platform.identity.dto.IdentityDtos.MfaEnrollResponse;
import ai.nova.platform.identity.dto.IdentityDtos.MfaVerifyRequest;
import ai.nova.platform.identity.dto.IdentityDtos.ProviderView;
import ai.nova.platform.identity.dto.IdentityDtos.SessionView;
import ai.nova.platform.identity.dto.IdentityDtos.SummaryView;
import ai.nova.platform.identity.entity.IdentityUserEntity;
import ai.nova.platform.identity.repository.IdentityUserRepository;
import ai.nova.platform.identity.security.IdentityAuthorizationService;
import ai.nova.platform.identity.service.AuthenticationService;
import ai.nova.platform.identity.service.IdentityProviderService;
import ai.nova.platform.identity.service.IdentityService;
import ai.nova.platform.identity.service.MfaService;
import ai.nova.platform.identity.service.SessionService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@RestController
@RequestMapping("/api/identity")
public class IdentityController {

    private final IdentityService identityService;
    private final IdentityProviderService identityProviderService;
    private final AuthenticationService authenticationService;
    private final SessionService sessionService;
    private final MfaService mfaService;
    private final IdentityAuthorizationService authorizationService;
    private final IdentityUserRepository identityUserRepository;

    public IdentityController(
            IdentityService identityService,
            IdentityProviderService identityProviderService,
            AuthenticationService authenticationService,
            SessionService sessionService,
            MfaService mfaService,
            IdentityAuthorizationService authorizationService,
            IdentityUserRepository identityUserRepository) {
        this.identityService = identityService;
        this.identityProviderService = identityProviderService;
        this.authenticationService = authenticationService;
        this.sessionService = sessionService;
        this.mfaService = mfaService;
        this.authorizationService = authorizationService;
        this.identityUserRepository = identityUserRepository;
    }

    @GetMapping("/config")
    public ConfigResponse config(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return identityService.getConfig();
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authenticationService.login(
                request.email(),
                request.password(),
                request.mfaCode(),
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/mfa/enroll")
    public MfaEnrollResponse enrollMfa(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireMfaManage(user);
        return mfaService.enroll(requireIdentityUserId(user));
    }

    @PostMapping("/mfa/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyMfa(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody MfaVerifyRequest request) {
        mfaService.confirmEnrollment(requireIdentityUserId(user), request.mfaCode());
    }

    @GetMapping("/providers")
    public List<ProviderView> providers(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return identityProviderService.listProviders(user.getOrganizationId());
    }

    @PostMapping("/providers")
    public ProviderView createProvider(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CreateProviderRequest request) {
        authorizationService.requireProviderManage(user);
        return identityProviderService.createProvider(user.getOrganizationId(), request);
    }

    @GetMapping("/sessions")
    public List<SessionView> sessions(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return identityService.listSessions(user.getOrganizationId());
    }

    @PostMapping("/sessions/{sessionId}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeSession(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID sessionId) {
        authorizationService.requireAdmin(user);
        sessionService.revokeSession(sessionId);
    }

    @GetMapping("/login-history")
    public List<LoginHistoryView> loginHistory(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return identityService.loginHistory(user.getOrganizationId());
    }

    @GetMapping("/summary")
    public SummaryView summary(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return identityService.summary(user.getOrganizationId());
    }

    private UUID requireIdentityUserId(AuthenticatedUser user) {
        return identityUserRepository.findByPlatformUserId(user.getUserId())
                .map(IdentityUserEntity::getId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "IDENTITY_USER_NOT_FOUND", "Identity user not found"));
    }
}
