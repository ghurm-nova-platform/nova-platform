package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.dto.IdentityDtos.ScimUserRequest;
import ai.nova.platform.identity.dto.IdentityDtos.ScimUserResponse;
import ai.nova.platform.identity.scim.ScimUserService;
import ai.nova.platform.identity.support.IdentityTestFixture;

@SpringBootTest
class ScimSynchronizationServiceTest {

    @Autowired
    private ScimUserService scimUserService;

    @Test
    @Transactional
    void createAndListScimUsers() {
        String userName = "scim-sync-" + System.nanoTime() + "@nova.local";
        ScimUserResponse created = scimUserService.createUser(
                IdentityTestFixture.ORG_ID, new ScimUserRequest(userName, "SCIM Sync User", true));

        assertThat(created.userName()).isEqualTo(userName);
        assertThat(created.active()).isTrue();

        assertThat(scimUserService.listUsers(IdentityTestFixture.ORG_ID))
                .anyMatch(user -> userName.equals(user.userName()));
    }
}
