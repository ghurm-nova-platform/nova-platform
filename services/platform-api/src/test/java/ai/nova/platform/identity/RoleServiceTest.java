package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.service.RoleService;
import ai.nova.platform.identity.support.IdentityTestFixture;

@SpringBootTest
class RoleServiceTest {

    @Autowired
    private RoleService roleService;

    @Test
    @Transactional
    void listRoles() {
        var roles = roleService.listRoles(IdentityTestFixture.ORG_ID);
        assertThat(roles).isNotNull();
    }
}
