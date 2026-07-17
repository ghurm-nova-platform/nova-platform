package ai.nova.platform.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.tool.entity.ToolStatus;
import ai.nova.platform.tool.repository.ToolDefinitionRepository;

@SpringBootTest
class ToolRepositoryTest {

    @Autowired
    private ToolDefinitionRepository toolRepository;

    @Test
    void findsSeededDemoToolsByProjectAndSearch() {
        UUID organizationId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID projectId = UUID.fromString("55555555-5555-5555-5555-555555555501");

        var page = toolRepository.search(
                organizationId, projectId, "CALCULATOR", ToolStatus.ACTIVE, null, org.springframework.data.domain.Pageable.unpaged());

        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent().get(0).getToolKey()).isEqualTo("CALCULATOR");
    }
}
