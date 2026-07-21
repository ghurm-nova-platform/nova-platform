package ai.nova.platform.llm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.llm.dto.LlmDtos.ModelView;
import ai.nova.platform.llm.dto.LlmDtos.RegisterModelRequest;
import ai.nova.platform.llm.entity.LlmModelFamily;
import ai.nova.platform.llm.entity.LlmModelStatus;
import ai.nova.platform.llm.entity.LlmProviderType;
import ai.nova.platform.llm.service.ModelRegistryService;
import ai.nova.platform.llm.support.LlmTestFixture;

@SpringBootTest
class ModelRegistryServiceTest {

    @Autowired
    private ModelRegistryService modelRegistryService;

    @Test
    void listsSeedModelAndRegistersNew() {
        assertThat(modelRegistryService.list(LlmTestFixture.ORG_ID))
                .anyMatch(m -> m.id().equals(LlmTestFixture.SEED_MODEL_ID));

        String code = "test-model-" + System.nanoTime();
        ModelView created = modelRegistryService.register(
                new RegisterModelRequest(
                        code,
                        "Test Model",
                        LlmModelFamily.CUSTOM,
                        LlmProviderType.DETERMINISTIC,
                        2048,
                        null,
                        "tester",
                        "[\"CHAT\"]",
                        "[\"test\"]"),
                LlmTestFixture.llmAdminUser());

        assertThat(created.code()).isEqualTo(code);
        assertThat(created.status()).isEqualTo(LlmModelStatus.REGISTERED);
        assertThat(modelRegistryService.get(LlmTestFixture.ORG_ID, created.id()).displayName())
                .isEqualTo("Test Model");
    }
}
