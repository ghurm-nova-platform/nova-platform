package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.service.PermissionService;
import ai.nova.platform.identity.support.IdentityTestFixture;

@SpringBootTest
class PermissionServiceTest {

    @Autowired
    private PermissionService permissionService;

    @Test
    @Transactional
    void listPermissions() {
        var permissions = permissionService.listPermissions(IdentityTestFixture.ORG_ID);
        assertThat(permissions).isNotNull();
    }
}
