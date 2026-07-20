package ai.nova.platform.environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import ai.nova.platform.environment.entity.EnvironmentStatus;
import ai.nova.platform.environment.service.EnvironmentValidationService;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest(properties = "nova.environment.enabled=true")
class EnvironmentValidationTest {

    @Autowired
    private EnvironmentValidationService validationService;

    @Test
    void testAliasNormalizesToTesting() {
        assert validationService.normalizeType("TEST") == ai.nova.platform.deployment.entity.EnvironmentType.TESTING;
    }

    @Test
    void archivedCannotTransition() {
        assertThatThrownBy(() -> validationService.validateStatusTransition(EnvironmentStatus.ARCHIVED, EnvironmentStatus.ACTIVE))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus(), ex -> ((ApiException) ex).getCode())
                .containsExactly(HttpStatus.CONFLICT, "ENVIRONMENT_INVALID_STATUS");
    }

    @Test
    void invalidTransitionRejected() {
        assertThatThrownBy(() -> validationService.validateStatusTransition(EnvironmentStatus.DISABLED, EnvironmentStatus.MAINTENANCE))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("ENVIRONMENT_INVALID_STATUS"));
    }

    @Test
    void duplicateLabelKeysRejected() {
        assertThatThrownBy(() -> validationService.validateMetadata(
                        java.util.List.of(
                                new ai.nova.platform.environment.dto.EnvironmentDtos.LabelItem("a", "1"),
                                new ai.nova.platform.environment.dto.EnvironmentDtos.LabelItem("A", "2")),
                        null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("ENVIRONMENT_METADATA_INVALID"));
    }
}
