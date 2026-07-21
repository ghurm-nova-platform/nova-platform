package ai.nova.platform.identity.service;

import java.time.Duration;

import org.springframework.stereotype.Service;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.security.JwtService;

@Service
public class IdentityJwtService {

    private final JwtService jwtService;

    public IdentityJwtService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public String createAccessToken(AuthenticatedUser user) {
        return jwtService.createAccessToken(user);
    }

    public AuthenticatedUser parseAccessToken(String token) {
        return jwtService.parseAccessToken(token);
    }

    public Duration getAccessTokenTtl() {
        return jwtService.getAccessTokenTtl();
    }

    public Duration getRefreshTokenTtl() {
        return jwtService.getRefreshTokenTtl();
    }
}
