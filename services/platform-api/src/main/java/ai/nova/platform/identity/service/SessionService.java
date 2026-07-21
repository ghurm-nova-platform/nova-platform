package ai.nova.platform.identity.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.configuration.IdentityProperties;
import ai.nova.platform.identity.entity.IdentitySessionEntity;
import ai.nova.platform.identity.entity.SessionStatus;
import ai.nova.platform.identity.repository.IdentitySessionRepository;
import ai.nova.platform.web.error.ApiException;
import ai.nova.platform.web.error.ResourceNotFoundException;

@Service
public class SessionService {

    private final IdentityProperties properties;
    private final IdentitySessionRepository sessionRepository;

    public SessionService(IdentityProperties properties, IdentitySessionRepository sessionRepository) {
        this.properties = properties;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public IdentitySessionEntity createSession(
            UUID organizationId,
            UUID identityUserId,
            UUID platformUserId,
            String ipAddress,
            String userAgent) {
        enforceConcurrentSessionLimit(identityUserId);
        Instant now = Instant.now();
        IdentitySessionEntity session = new IdentitySessionEntity(
                UUID.randomUUID(),
                organizationId,
                identityUserId,
                platformUserId,
                ipAddress,
                userAgent,
                now,
                now.plus(properties.getSessionAbsoluteTimeout()));
        return sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public IdentitySessionEntity requireActiveSession(UUID sessionId) {
        IdentitySessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        Instant now = Instant.now();
        if (!session.isActive(now)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "SESSION_INVALID", "Session is not active");
        }
        if (session.getLastAccessedAt().plus(properties.getSessionIdleTimeout()).isBefore(now)) {
            session.expire(now);
            sessionRepository.save(session);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED", "Session idle timeout exceeded");
        }
        return session;
    }

    @Transactional
    public void touchSession(UUID sessionId) {
        Instant now = Instant.now();
        IdentitySessionEntity session = requireActiveSession(sessionId);
        session.touch(now);
        sessionRepository.save(session);
    }

    @Transactional
    public void revokeSession(UUID sessionId) {
        IdentitySessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        session.revoke(Instant.now());
        sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public List<IdentitySessionEntity> listActiveSessions(UUID organizationId) {
        return sessionRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(
                organizationId, SessionStatus.ACTIVE);
    }

    private void enforceConcurrentSessionLimit(UUID identityUserId) {
        int max = properties.getMaxConcurrentSessions();
        if (max <= 0) {
            return;
        }
        long active = sessionRepository.countByIdentityUserIdAndStatus(identityUserId, SessionStatus.ACTIVE);
        if (active >= max) {
            List<IdentitySessionEntity> sessions =
                    sessionRepository.findByIdentityUserIdAndStatus(identityUserId, SessionStatus.ACTIVE);
            sessions.stream()
                    .sorted(java.util.Comparator.comparing(IdentitySessionEntity::getCreatedAt))
                    .limit(active - max + 1)
                    .forEach(session -> {
                        session.revoke(Instant.now());
                        sessionRepository.save(session);
                    });
        }
    }
}
