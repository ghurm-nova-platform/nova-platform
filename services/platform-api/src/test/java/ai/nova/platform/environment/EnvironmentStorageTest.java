package ai.nova.platform.environment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.deployment.entity.DeploymentEnvironmentEntity;
import ai.nova.platform.deployment.entity.EnvironmentType;
import ai.nova.platform.deployment.repository.DeploymentEnvironmentRepository;
import ai.nova.platform.environment.dto.EnvironmentDtos.CreateEnvironmentRequest;
import ai.nova.platform.environment.entity.EnvironmentEventType;
import ai.nova.platform.environment.entity.EnvironmentStatus;
import ai.nova.platform.environment.repository.EnvironmentEventRepository;
import ai.nova.platform.environment.repository.EnvironmentHistoryRepository;
import ai.nova.platform.environment.repository.EnvironmentLabelRepository;
import ai.nova.platform.environment.service.EnvironmentStorageService;
import ai.nova.platform.environment.support.EnvironmentTestFixture;

@SpringBootTest(properties = "nova.environment.enabled=true")
class EnvironmentStorageTest {

    @Autowired
    private EnvironmentStorageService storageService;

    @Autowired
    private DeploymentEnvironmentRepository environmentRepository;

    @Autowired
    private EnvironmentLabelRepository labelRepository;

    @Autowired
    private EnvironmentEventRepository eventRepository;

    @Autowired
    private EnvironmentHistoryRepository historyRepository;

    @Test
    void createPersistsLabelsEventsAndHistory() {
        String name = "storage-" + UUID.randomUUID();
        CreateEnvironmentRequest request = new CreateEnvironmentRequest(
                EnvironmentTestFixture.PROJECT_ID,
                name,
                "desc",
                EnvironmentType.STAGING,
                "us-west-2",
                "eks",
                "cluster-a",
                "ns-a",
                "aws",
                "k8s",
                "owner",
                "bu",
                "cc",
                java.util.Map.of("tier", "nonprod"),
                List.of(new ai.nova.platform.environment.dto.EnvironmentDtos.LabelItem("env", "stage")),
                List.of(new ai.nova.platform.environment.dto.EnvironmentDtos.VariableMetadataItem(
                        "API_URL", "Base URL", true, false, "RUNTIME")));

        DeploymentEnvironmentEntity created = storageService.createEnvironment(
                EnvironmentTestFixture.ORG_ID, request, EnvironmentType.STAGING, EnvironmentTestFixture.USER_ID, Instant.now());

        assertThat(created.getStatus()).isEqualTo(EnvironmentStatus.ACTIVE);
        assertThat(created.isProjectScoped()).isTrue();
        assertThat(labelRepository.findByEnvironmentIdOrderByLabelKeyAsc(created.getId())).hasSize(1);
        assertThat(eventRepository.findByEnvironmentIdOrderByCreatedAtAsc(created.getId()))
                .extracting(e -> e.getEventType())
                .contains(EnvironmentEventType.CREATED, EnvironmentEventType.ENABLED);
        assertThat(historyRepository.findByEnvironmentIdOrderByCreatedAtDesc(created.getId())).isNotEmpty();
        assertThat(environmentRepository.findById(created.getId())).isPresent();
    }
}
