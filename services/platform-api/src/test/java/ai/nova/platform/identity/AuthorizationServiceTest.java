package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.identity.error.IdentityErrorCodes;
import ai.nova.platform.identity.permission.IdentityPermissionCodes;
import ai.nova.platform.identity.security.IdentityAuthorizationService;
import ai.nova.platform.identity.support.IdentityTestFixture;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest
class AuthorizationServiceTest {

    @Autowired
    private IdentityAuthorizationService authorizationService;

    @Test
    void denyWithoutPermission() {
        AuthenticatedUser reader = IdentityTestFixture.identityReadUser();

        assertThatThrownBy(() -> authorizationService.requireAdmin(reader))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(IdentityErrorCodes.PERMISSION_DENIED);
    }

    @Test
    void allowWithIdentityAdmin() {
        AuthenticatedUser admin = new AuthenticatedUser(
                IdentityTestFixture.USER_ID,
                IdentityTestFixture.ORG_ID,
                "identity-admin@nova.local",
                "Identity Admin",
                List.of("USER"),
                List.of(IdentityPermissionCodes.IDENTITY_ADMIN),
                true);

        assertThatCode(() -> authorizationService.requireAdmin(admin)).doesNotThrowAnyException();
        assertThatCode(() -> authorizationService.requireUserAdmin(admin)).doesNotThrowAnyException();
    }

    @Test
    void orgAdminBypassesPermissionCheck() {
        AuthenticatedUser orgAdmin = new AuthenticatedUser(
                UUID.randomUUID(),
                IdentityTestFixture.ORG_ID,
                "org-admin@nova.local",
                "Org Admin",
                List.of("ORG_ADMIN"),
                List.of(),
                true);

        assertThatCode(() -> authorizationService.requireAdmin(orgAdmin)).doesNotThrowAnyException();
        assertThat(orgAdmin.getRoles()).contains("ORG_ADMIN");
    }
}
