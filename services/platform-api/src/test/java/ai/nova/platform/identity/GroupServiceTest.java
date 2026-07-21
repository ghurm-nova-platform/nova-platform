package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.service.GroupService;
import ai.nova.platform.identity.support.IdentityTestFixture;

@SpringBootTest
class GroupServiceTest {

    @Autowired
    private GroupService groupService;

    @Test
    @Transactional
    void listGroups() {
        var groups = groupService.listGroups(IdentityTestFixture.ORG_ID);
        assertThat(groups).isNotNull();
    }
}
