package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.service.ServiceAccountService;
import ai.nova.platform.identity.support.IdentityTestFixture;

@SpringBootTest
class ServiceAccountServiceTest {

    @Autowired
    private ServiceAccountService serviceAccountService;

    @Test
    @Transactional
    void listServiceAccounts() {
        var accounts = serviceAccountService.listServiceAccounts(IdentityTestFixture.ORG_ID);
        assertThat(accounts).isNotNull();
    }
}
