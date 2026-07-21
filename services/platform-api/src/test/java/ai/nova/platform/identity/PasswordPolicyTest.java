package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.identity.service.PasswordPolicyService;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest
class PasswordPolicyTest {

    @Autowired
    private PasswordPolicyService passwordPolicyService;

    @Test
    void rejectsShortPassword() {
        assertThatThrownBy(() -> passwordPolicyService.validate("Short1!"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("at least");
    }

    @Test
    void acceptsStrongPassword() {
        passwordPolicyService.validate("StrongPass123!");
    }
}
