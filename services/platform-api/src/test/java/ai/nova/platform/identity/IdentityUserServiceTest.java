package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.service.IdentityUserService;
import ai.nova.platform.identity.support.IdentityTestFixture;

@SpringBootTest
class IdentityUserServiceTest {

    @Autowired
    private IdentityUserService identityUserService;

    @Test
    @Transactional
    void listAndUnlockUser() {
        var users = identityUserService.listUsers(IdentityTestFixture.ORG_ID);
        assertThat(users).isNotEmpty();
        assertThat(users.getFirst().email()).isEqualTo("admin@nova.local");

        var unlocked = identityUserService.unlockUser(IdentityTestFixture.ORG_ID, IdentityTestFixture.IDENTITY_USER_ID);
        assertThat(unlocked.locked()).isFalse();
    }
}
