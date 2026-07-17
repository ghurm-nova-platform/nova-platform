package ai.nova.platform.project;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

@SpringBootTest
class ProjectRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void findsSeededProjectByOrganizationAndSearch() {
        UUID organizationId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        assertThat(projectRepository.existsByOrganizationIdAndNameIgnoreCase(organizationId, "Demo Project"))
                .isTrue();
        assertThat(projectRepository.searchByOrganization(organizationId, "Demo", PageRequest.of(0, 10)).getContent())
                .isNotEmpty();
    }
}
