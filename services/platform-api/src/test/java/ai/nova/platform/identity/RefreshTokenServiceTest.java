package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.entity.IdentityRefreshTokenEntity;
import ai.nova.platform.identity.entity.IdentitySessionEntity;
import ai.nova.platform.identity.service.RefreshTokenService;
import ai.nova.platform.identity.service.SessionService;
import ai.nova.platform.identity.support.IdentityTestFixture;
import ai.nova.platform.user.UserAccount;
import ai.nova.platform.user.UserAccountRepository;

@SpringBootTest
class RefreshTokenServiceTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    void generateAndHashRoundtrip() {
        String token = refreshTokenService.generateRefreshToken();
        assertThat(token).isNotBlank();

        String hash = refreshTokenService.hashToken(token);
        assertThat(hash).hasSize(64);
        assertThat(refreshTokenService.hashToken(token)).isEqualTo(hash);
        assertThat(refreshTokenService.hashToken(token + "x")).isNotEqualTo(hash);
    }

    @Test
    @Transactional
    void issueAndRevokeRefreshToken() {
        IdentitySessionEntity session = sessionService.createSession(
                IdentityTestFixture.ORG_ID,
                IdentityTestFixture.IDENTITY_USER_ID,
                IdentityTestFixture.USER_ID,
                "127.0.0.1",
                "RefreshTokenServiceTest");

        UserAccount platformUser = userAccountRepository.findById(IdentityTestFixture.USER_ID).orElseThrow();
        String raw = refreshTokenService.issueRefreshTokens(session, IdentityTestFixture.IDENTITY_USER_ID, platformUser);

        IdentityRefreshTokenEntity active = refreshTokenService.requireActiveIdentityRefreshToken(raw);
        assertThat(active).isNotNull();
        assertThat(active.getSessionId()).isEqualTo(session.getId());

        refreshTokenService.revokeRefreshToken(raw);
        assertThat(refreshTokenService.requireActiveIdentityRefreshToken(raw)).isNull();
    }
}
