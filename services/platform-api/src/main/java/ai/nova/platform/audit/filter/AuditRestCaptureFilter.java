package ai.nova.platform.audit.filter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import ai.nova.platform.audit.config.AuditProperties;
import ai.nova.platform.audit.dto.AuditDtos.RecordAuditEventRequest;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSeverity;
import ai.nova.platform.audit.entity.AuditSource;
import ai.nova.platform.audit.service.AuditPublisher;
import ai.nova.platform.security.AuthenticatedUser;

@Component
public class AuditRestCaptureFilter extends OncePerRequestFilter {

    private final AuditProperties properties;
    private final AuditPublisher auditPublisher;

    public AuditRestCaptureFilter(AuditProperties properties, AuditPublisher auditPublisher) {
        this.properties = properties;
        this.auditPublisher = auditPublisher;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled() || !properties.isCaptureRestApi()) {
            return true;
        }
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        if (path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/refresh")
                || path.startsWith("/api/auth/logout")
                || path.startsWith("/api/v1/health")
                || path.startsWith("/actuator/health")
                || path.startsWith("/api/audit")) {
            return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(request, response);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();
        int status = response.getStatus();
        AuditResult result = status >= 400 ? AuditResult.FAILURE : AuditResult.SUCCESS;
        AuditSeverity severity = status >= 500 ? AuditSeverity.HIGH : AuditSeverity.LOW;

        try {
            auditPublisher.record(new RecordAuditEventRequest(
                    user.getOrganizationId(),
                    null,
                    user.getUserId(),
                    user.getDisplayName(),
                    null,
                    AuditEntityType.CONFIGURATION,
                    null,
                    path,
                    AuditAction.ACCESS,
                    result,
                    severity,
                    AuditSource.REST_API,
                    request.getHeader("X-Correlation-Id"),
                    request.getHeader("X-Request-Id"),
                    request.getRemoteAddr(),
                    truncate(request.getHeader("User-Agent"), 500),
                    Map.of("method", method, "path", path, "status", status)));
        } catch (RuntimeException ignored) {
            // Audit must never fail the HTTP response.
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
