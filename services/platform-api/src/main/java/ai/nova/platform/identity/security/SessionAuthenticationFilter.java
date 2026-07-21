package ai.nova.platform.identity.security;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import ai.nova.platform.identity.entity.IdentitySessionEntity;
import ai.nova.platform.identity.repository.IdentityUserRepository;
import ai.nova.platform.identity.service.SessionService;
import ai.nova.platform.permission.Permission;
import ai.nova.platform.role.Role;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.user.UserAccount;
import ai.nova.platform.user.UserAccountRepository;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    public static final String SESSION_HEADER = "X-Nova-Session";

    private final SessionService sessionService;
    private final IdentityUserRepository identityUserRepository;
    private final UserAccountRepository userAccountRepository;

    public SessionAuthenticationFilter(
            SessionService sessionService,
            IdentityUserRepository identityUserRepository,
            UserAccountRepository userAccountRepository) {
        this.sessionService = sessionService;
        this.identityUserRepository = identityUserRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String sessionHeader = request.getHeader(SESSION_HEADER);
            if (sessionHeader != null && !sessionHeader.isBlank()) {
                try {
                    IdentitySessionEntity session = sessionService.requireActiveSession(UUID.fromString(sessionHeader));
                    sessionService.touchSession(session.getId());
                    identityUserRepository.findById(session.getIdentityUserId()).ifPresent(identityUser -> {
                        if (identityUser.getPlatformUserId() != null) {
                            userAccountRepository.findById(identityUser.getPlatformUserId()).ifPresent(user -> authenticate(request, user));
                        }
                    });
                } catch (Exception ignored) {
                    SecurityContextHolder.clearContext();
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, UserAccount user) {
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(),
                user.getOrganization().getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRoles().stream().map(Role::getCode).sorted().toList(),
                user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(Permission::getCode)
                        .distinct()
                        .sorted()
                        .toList(),
                user.isEnabled());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
