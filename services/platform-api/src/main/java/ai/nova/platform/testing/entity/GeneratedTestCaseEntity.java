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
@Table(name = "generated_test_cases")
public class GeneratedTestCaseEntity {

    @Id
    private UUID id;

    @Column(name = "testing_result_id", nullable = false)
    private UUID testingResultId;

    @Column(name = "generated_test_id", nullable = false)
    private UUID generatedTestId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String name;

    @Column(length = 4000)
    private String steps;

    @Column(name = "expected_result", length = 4000)
    private String expectedResult;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestPriority priority;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GeneratedTestCaseEntity() {
    }

    public GeneratedTestCaseEntity(
            UUID id,
            UUID testingResultId,
            UUID generatedTestId,
            UUID organizationId,
            String name,
            String steps,
            String expectedResult,
            TestPriority priority,
            Instant createdAt) {
        this.id = id;
        this.testingResultId = testingResultId;
        this.generatedTestId = generatedTestId;
        this.organizationId = organizationId;
        this.name = name;
        this.steps = steps;
        this.expectedResult = expectedResult;
        this.priority = priority;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTestingResultId() {
        return testingResultId;
    }

    public UUID getGeneratedTestId() {
        return generatedTestId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getName() {
        return name;
    }

    public String getSteps() {
        return steps;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public TestPriority getPriority() {
        return priority;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
