package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.entity.IdentityProviderEntity;
import ai.nova.platform.identity.entity.ProviderType;
import ai.nova.platform.identity.service.IdentityProviderService;
import ai.nova.platform.identity.support.IdentityTestFixture;

@SpringBootTest
class IdentityProviderServiceTest {

    @Autowired
    private IdentityProviderService identityProviderService;

    @Test
    @Transactional
    void resolveLocalProviderAndList() {
        IdentityProviderEntity local = identityProviderService.resolveLocalProvider(IdentityTestFixture.ORG_ID);
        assertThat(local.getProviderType()).isEqualTo(ProviderType.LOCAL);
        assertThat(local.getName()).isNotBlank();

        var providers = identityProviderService.listProviders(IdentityTestFixture.ORG_ID);
        assertThat(providers).isNotEmpty();
        assertThat(providers).anyMatch(p -> "Local Authentication".equals(p.name()));
    }
}
