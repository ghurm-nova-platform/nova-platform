package ai.nova.platform.identity.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.configuration.IdentityProperties;
import ai.nova.platform.identity.dto.IdentityDtos.SessionView;
import ai.nova.platform.identity.entity.IdentitySessionEntity;
import ai.nova.platform.identity.entity.SessionStatus;
import ai.nova.platform.identity.error.IdentityErrorCodes;
import ai.nova.platform.identity.mapper.IdentityEntityMapper;
import ai.nova.platform.identity.repository.IdentityRefreshTokenRepository;
import ai.nova.platform.identity.repository.IdentitySessionRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class SessionService {

    private final IdentityProperties properties;
    private final IdentitySessionRepository sessionRepository;
    private final IdentityRefreshTokenRepository refreshTokenRepository;

    public SessionService(
            IdentityProperties properties,
            IdentitySessionRepository sessionRepository,
            IdentityRefreshTokenRepository refreshTokenRepository) {
        this.properties = properties;
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
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
    public SessionView getSession(UUID organizationId, UUID sessionId) {
        return IdentityEntityMapper.toSessionView(requireOrgSession(organizationId, sessionId));
    }

    @Transactional(readOnly = true)
    public IdentitySessionEntity requireActiveSession(UUID sessionId) {
        IdentitySessionEntity session = sessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, IdentityErrorCodes.SESSION_NOT_FOUND, "Session not found"));
        Instant now = Instant.now();
        if (!session.isActive(now)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, IdentityErrorCodes.TOKEN_INVALID, "Session is not active");
        }
        if (session.getLastAccessedAt().plus(properties.getSessionIdleTimeout()).isBefore(now)) {
            session.expire(now);
            sessionRepository.save(session);
            throw new ApiException(HttpStatus.UNAUTHORIZED, IdentityErrorCodes.TOKEN_EXPIRED, "Session idle timeout exceeded");
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
        IdentitySessionEntity session = sessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, IdentityErrorCodes.SESSION_NOT_FOUND, "Session not found"));
        session.revoke(Instant.now());
        sessionRepository.save(session);
    }

    @Transactional
    public void revokeAllSessions(UUID organizationId) {
        Instant now = Instant.now();
        sessionRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(organizationId, SessionStatus.ACTIVE)
                .forEach(session -> {
                    session.revoke(now);
                    sessionRepository.save(session);
                });
    }

    @Transactional
    public void revokeAllSessionsForUser(UUID identityUserId) {
        Instant now = Instant.now();
        sessionRepository.findByIdentityUserIdAndStatus(identityUserId, SessionStatus.ACTIVE).forEach(session -> {
            session.revoke(now);
            sessionRepository.save(session);
        });
        refreshTokenRepository.findByIdentityUserId(identityUserId).forEach(token -> {
            token.revoke(now);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional(readOnly = true)
    public List<SessionView> listActiveSessions(UUID organizationId) {
        return sessionRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(organizationId, SessionStatus.ACTIVE)
                .stream()
                .map(IdentityEntityMapper::toSessionView)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countActiveSessions(UUID organizationId) {
        return sessionRepository.countByOrganizationIdAndStatus(organizationId, SessionStatus.ACTIVE);
    }

    private IdentitySessionEntity requireOrgSession(UUID organizationId, UUID sessionId) {
        return sessionRepository
                .findById(sessionId)
                .filter(s -> s.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, IdentityErrorCodes.SESSION_NOT_FOUND, "Session not found"));
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
