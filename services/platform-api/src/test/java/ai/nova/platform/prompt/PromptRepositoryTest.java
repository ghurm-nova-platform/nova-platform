package ai.nova.platform.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import ai.nova.platform.prompt.entity.PromptStatus;
import ai.nova.platform.prompt.entity.PromptType;
import ai.nova.platform.prompt.repository.PromptRepository;

@SpringBootTest
class PromptRepositoryTest {

    @Autowired
    private PromptRepository promptRepository;

    @Test
    void findsSeededDemoPromptByProjectTagAndSearch() {
        UUID organizationId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID projectId = UUID.fromString("55555555-5555-5555-5555-555555555501");

        assertThat(promptRepository.existsByProjectIdAndNameIgnoreCase(projectId, "Demo Support Reply"))
                .isTrue();
        assertThat(promptRepository.search(
                        organizationId,
                        projectId,
                        "Demo",
                        PromptStatus.DRAFT,
                        PromptType.CHAT,
                        "support",
                        PageRequest.of(0, 10))
                .getContent())
                .isNotEmpty();
    }
}
