package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.entity.ProviderType;
import ai.nova.platform.identity.service.UserSynchronizationService;
import ai.nova.platform.identity.support.IdentityTestFixture;

@SpringBootTest
class UserSynchronizationServiceTest {

    @Autowired
    private UserSynchronizationService userSynchronizationService;

    @Test
    @Transactional
    void syncReturnsWithoutExternalDirectory() {
        assertThatCode(() -> userSynchronizationService.synchronizeUser(
                        IdentityTestFixture.ORG_ID, IdentityTestFixture.USER_ID))
                .doesNotThrowAnyException();

        assertThatCode(() -> userSynchronizationService.synchronizeProvider(
                        IdentityTestFixture.ORG_ID, IdentityTestFixture.ORG_ID, ProviderType.LOCAL))
                .doesNotThrowAnyException();

        assertThatCode(() -> userSynchronizationService.synchronizeGroup(
                        IdentityTestFixture.ORG_ID, IdentityTestFixture.ORG_ID))
                .doesNotThrowAnyException();
    }
}
