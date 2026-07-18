package ai.nova.platform.testing.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "generated_tests")
public class GeneratedTestEntity {

    @Id
    private UUID id;

    @Column(name = "testing_result_id", nullable = false)
    private UUID testingResultId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type", nullable = false, length = 50)
    private TestType testType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestPriority priority;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(name = "artifact_id")
    private UUID artifactId;

    @Column(name = "artifact_path", length = 1000)
    private String artifactPath;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GeneratedTestEntity() {
    }

    public GeneratedTestEntity(
            UUID id,
            UUID testingResultId,
            UUID organizationId,
            TestType testType,
            TestPriority priority,
            String title,
            String description,
            UUID artifactId,
            String artifactPath,
            Instant createdAt) {
        this.id = id;
        this.testingResultId = testingResultId;
        this.organizationId = organizationId;
        this.testType = testType;
        this.priority = priority;
        this.title = title;
        this.description = description;
        this.artifactId = artifactId;
        this.artifactPath = artifactPath;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTestingResultId() {
        return testingResultId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public TestType getTestType() {
        return testType;
    }

    public TestPriority getPriority() {
        return priority;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public UUID getArtifactId() {
        return artifactId;
    }

    public String getArtifactPath() {
        return artifactPath;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
