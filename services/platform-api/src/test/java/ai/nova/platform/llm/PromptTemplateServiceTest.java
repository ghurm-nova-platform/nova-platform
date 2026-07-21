package ai.nova.platform.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.llm.dto.LlmDtos.RenderPromptResponse;
import ai.nova.platform.llm.service.PromptTemplateService;
import ai.nova.platform.llm.support.LlmTestFixture;

@SpringBootTest
class PromptTemplateServiceTest {

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Test
    void rendersSeedChatTemplateVariables() {
        UUID promptId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01");
        RenderPromptResponse rendered = promptTemplateService.render(
                promptId, Map.of("userMessage", "hello templates"), LlmTestFixture.llmAdminUser());

        assertThat(rendered.systemPrompt()).contains("Nova");
        assertThat(rendered.userPrompt()).isEqualTo("hello templates");
    }
}
