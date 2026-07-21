package ai.nova.platform.identity.controller;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.auth.AuthDtos.TokenResponse;
import ai.nova.platform.identity.dto.IdentityDtos.ChangePasswordRequest;
import ai.nova.platform.identity.dto.IdentityDtos.ForgotPasswordRequest;
import ai.nova.platform.identity.dto.IdentityDtos.GenericMessageResponse;
import ai.nova.platform.identity.dto.IdentityDtos.LoginRequest;
import ai.nova.platform.identity.dto.IdentityDtos.LogoutRequest;
import ai.nova.platform.identity.dto.IdentityDtos.MfaEnrollResponse;
import ai.nova.platform.identity.dto.IdentityDtos.MfaVerifyRequest;
import ai.nova.platform.identity.dto.IdentityDtos.RefreshTokenRequest;
import ai.nova.platform.identity.dto.IdentityDtos.ResetPasswordRequest;
import ai.nova.platform.identity.dto.IdentityDtos.ValidateTokenRequest;
import ai.nova.platform.identity.dto.IdentityDtos.ValidateTokenResponse;
import ai.nova.platform.identity.entity.IdentityUserEntity;
import ai.nova.platform.identity.error.IdentityErrorCodes;
import ai.nova.platform.identity.repository.IdentityUserRepository;
import ai.nova.platform.identity.security.IdentityAuthorizationService;
import ai.nova.platform.identity.service.AuthenticationService;
import ai.nova.platform.identity.service.MfaService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@RestController
@RequestMapping("/api/identity")
public class IdentityAuthController {

    private final AuthenticationService authenticationService;
    private final MfaService mfaService;
    private final IdentityAuthorizationService authorizationService;
    private final IdentityUserRepository identityUserRepository;

    public IdentityAuthController(
            AuthenticationService authenticationService,
            MfaService mfaService,
            IdentityAuthorizationService authorizationService,
            IdentityUserRepository identityUserRepository) {
        this.authenticationService = authenticationService;
        this.mfaService = mfaService;
        this.authorizationService = authorizationService;
        this.identityUserRepository = identityUserRepository;
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

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request, HttpServletRequest httpRequest) {
        authenticationService.logout(
                request.refreshToken(), httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(@AuthenticationPrincipal AuthenticatedUser user, HttpServletRequest httpRequest) {
        authenticationService.logoutAll(
                requireIdentityUserId(user), httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/refresh-token")
    public TokenResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return authenticationService.refresh(request.refreshToken());
    }

    @PostMapping("/validate-token")
    public ValidateTokenResponse validateToken(@Valid @RequestBody ValidateTokenRequest request) {
        return authenticationService.validateToken(request.accessToken());
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody ChangePasswordRequest request) {
        authenticationService.changePassword(
                requireIdentityUserId(user), request.currentPassword(), request.newPassword());
    }

    @PostMapping("/forgot-password")
    public GenericMessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authenticationService.forgotPassword(request.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(request.token(), request.newPassword());
    }

    @PostMapping({"/enroll-mfa", "/mfa/enroll"})
    public MfaEnrollResponse enrollMfa(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireMfaManage(user);
        return mfaService.enroll(requireIdentityUserId(user));
    }

    @PostMapping({"/verify-mfa", "/mfa/verify"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyMfa(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody MfaVerifyRequest request) {
        mfaService.confirmEnrollment(requireIdentityUserId(user), request.mfaCode());
    }

    @PostMapping("/disable-mfa")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableMfa(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireMfaManage(user);
        mfaService.disable(requireIdentityUserId(user));
    }

    private UUID requireIdentityUserId(AuthenticatedUser user) {
        return identityUserRepository.findByPlatformUserId(user.getUserId())
                .map(IdentityUserEntity::getId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, IdentityErrorCodes.IDENTITY_USER_NOT_FOUND, "Identity user not found"));
    }
}
