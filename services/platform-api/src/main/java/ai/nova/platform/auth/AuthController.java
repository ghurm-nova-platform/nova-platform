package ai.nova.platform.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.auth.AuthDtos.LoginRequest;
import ai.nova.platform.auth.AuthDtos.LogoutRequest;
import ai.nova.platform.auth.AuthDtos.MeResponse;
import ai.nova.platform.auth.AuthDtos.RefreshRequest;
import ai.nova.platform.auth.AuthDtos.TokenResponse;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(
                request.email(),
                request.password(),
                request.mfaCode(),
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request, HttpServletRequest httpRequest) {
        authService.logout(
                request.refreshToken(), httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return authService.me(user);
    }
}
