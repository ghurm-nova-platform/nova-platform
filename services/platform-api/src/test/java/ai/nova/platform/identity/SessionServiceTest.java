package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.entity.IdentitySessionEntity;
import ai.nova.platform.identity.entity.SessionStatus;
import ai.nova.platform.identity.repository.IdentitySessionRepository;
import ai.nova.platform.identity.service.SessionService;
import ai.nova.platform.identity.support.IdentityTestFixture;

/**
 * Alias coverage for session create/revoke (see also {@link IdentitySessionTest}).
 */
@SpringBootTest
class SessionServiceTest {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private IdentitySessionRepository sessionRepository;

    @Test
    @Transactional
    void createSessionAndRevokeSession() {
        IdentitySessionEntity session = sessionService.createSession(
                IdentityTestFixture.ORG_ID,
                IdentityTestFixture.IDENTITY_USER_ID,
                IdentityTestFixture.USER_ID,
                "127.0.0.1",
                "SessionServiceTest");

        assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(sessionRepository.findById(session.getId())).isPresent();

        sessionService.revokeSession(session.getId());
        IdentitySessionEntity revoked = sessionRepository.findById(session.getId()).orElseThrow();
        assertThat(revoked.getStatus()).isEqualTo(SessionStatus.REVOKED);
        assertThat(revoked.getRevokedAt()).isNotNull();
    }
}
