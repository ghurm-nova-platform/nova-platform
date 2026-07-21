package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.dto.IdentityDtos.ApiTokenCreateResponse;
import ai.nova.platform.identity.service.ApiTokenService;
import ai.nova.platform.identity.support.IdentityTestFixture;

@SpringBootTest
class ApiTokenServiceTest {

    @Autowired
    private ApiTokenService apiTokenService;

    @Test
    @Transactional
    void listAndCreateToken() {
        var before = apiTokenService.listTokens(IdentityTestFixture.ORG_ID);
        assertThat(before).isNotNull();

        ApiTokenCreateResponse created = apiTokenService.createToken(
                IdentityTestFixture.ORG_ID, IdentityTestFixture.IDENTITY_USER_ID, "service-test-token");

        assertThat(created.token()).startsWith(ApiTokenService.TOKEN_PREFIX);
        assertThat(apiTokenService.listTokens(IdentityTestFixture.ORG_ID)).hasSize(before.size() + 1);
    }
}
