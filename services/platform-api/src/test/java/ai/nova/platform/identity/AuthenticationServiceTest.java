package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.auth.AuthDtos.TokenResponse;
import ai.nova.platform.identity.error.IdentityErrorCodes;
import ai.nova.platform.identity.service.AuthenticationService;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest
class AuthenticationServiceTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Test
    @Transactional
    void loginSuccess() {
        TokenResponse tokens = authenticationService.login(
                "admin@nova.local", "ChangeMe123!", null, "127.0.0.1", "AuthenticationServiceTest");

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(tokens.tokenType()).isEqualTo("Bearer");
        assertThat(tokens.expiresInSeconds()).isPositive();
    }

    @Test
    @Transactional
    void loginInvalidCredentialsThrows() {
        assertThatThrownBy(() -> authenticationService.login(
                        "admin@nova.local", "wrong-password", null, "127.0.0.1", "AuthenticationServiceTest"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getCode()).isEqualTo(IdentityErrorCodes.INVALID_CREDENTIALS);
                });
    }
}
