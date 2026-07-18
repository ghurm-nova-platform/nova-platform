package ai.nova.platform.testing.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.testing.dto.TestingDtos.ArtifactReference;
import ai.nova.platform.testing.dto.TestingDtos.GeneratedTest;
import ai.nova.platform.testing.dto.TestingDtos.GeneratedTestDraft;
import ai.nova.platform.testing.dto.TestingDtos.ParsedTestingOutput;
import ai.nova.platform.testing.dto.TestingDtos.TestCase;
import ai.nova.platform.testing.dto.TestingDtos.TestCaseDraft;
import ai.nova.platform.testing.dto.TestingDtos.TestingResult;
import ai.nova.platform.testing.entity.GeneratedTestCaseEntity;
import ai.nova.platform.testing.entity.GeneratedTestEntity;
import ai.nova.platform.testing.entity.TestPriority;
import ai.nova.platform.testing.entity.TestType;
import ai.nova.platform.testing.entity.TestingResultEntity;
import ai.nova.platform.testing.entity.TestingReviewedArtifactEntity;
import ai.nova.platform.testing.repository.GeneratedTestCaseRepository;
import ai.nova.platform.testing.repository.GeneratedTestRepository;
import ai.nova.platform.testing.repository.TestingResultRepository;
import ai.nova.platform.testing.repository.TestingReviewedArtifactRepository;

/**
 * Persists testing coverage, suites, cases, and artifact references.
 * Does not execute tests or modify repositories.
 */
@Service
public class TestingStorageService {

    private final TestingResultRepository resultRepository;
    private final GeneratedTestRepository testRepository;
    private final GeneratedTestCaseRepository caseRepository;
    private final TestingReviewedArtifactRepository reviewedArtifactRepository;

    public TestingStorageService(
            TestingResultRepository resultRepository,
            GeneratedTestRepository testRepository,
            GeneratedTestCaseRepository caseRepository,
            TestingReviewedArtifactRepository reviewedArtifactRepository) {
        this.resultRepository = resultRepository;
        this.testRepository = testRepository;
        this.caseRepository = caseRepository;
        this.reviewedArtifactRepository = reviewedArtifactRepository;
    }

    @Transactional
    public TestingResult replaceResult(
            AgentOrchestrationTask task,
            List<GeneratedArtifactResponse> artifacts,
            ParsedTestingOutput parsed,
            Long tokensUsed,
            String model,
            String provider,
            Long generationTimeMs) {
        resultRepository.deleteByTaskIdAndOrganizationId(task.getId(), task.getOrganizationId());

        Instant now = Instant.now();
        UUID resultId = UUID.randomUUID();
        TestingResultEntity result = new TestingResultEntity(
                resultId,
                task.getOrganizationId(),
                task.getProjectId(),
                task.getRunId(),
                task.getId(),
                parsed.summary(),
                parsed.coverageEstimate(),
                tokensUsed,
                model,
                provider,
                generationTimeMs,
                now);
        resultRepository.save(result);

        Map<String, GeneratedArtifactResponse> byPath = new LinkedHashMap<>();
        for (GeneratedArtifactResponse artifact : artifacts) {
            byPath.put(artifact.path(), artifact);
            reviewedArtifactRepository.save(new TestingReviewedArtifactEntity(
                    UUID.randomUUID(),
                    resultId,
                    task.getOrganizationId(),
                    artifact.id(),
                    artifact.path(),
                    artifact.filename(),
                    artifact.language().name(),
                    artifact.sha256(),
                    now));
        }

        List<GeneratedTest> tests = new ArrayList<>();
        List<TestCase> allCases = new ArrayList<>();
        for (GeneratedTestDraft draft : parsed.generatedTests()) {
            GeneratedArtifactResponse matched =
                    draft.artifactPath() == null ? null : byPath.get(draft.artifactPath());
            UUID testId = UUID.randomUUID();
            GeneratedTestEntity testEntity = new GeneratedTestEntity(
                    testId,
                    resultId,
                    task.getOrganizationId(),
                    draft.type(),
                    draft.priority(),
                    draft.title().trim(),
                    draft.description().trim(),
                    matched != null ? matched.id() : null,
                    draft.artifactPath(),
                    now);
            testRepository.save(testEntity);

            List<TestCaseDraft> caseDrafts = draft.cases() == null || draft.cases().isEmpty()
                    ? List.of(new TestCaseDraft(
                            draft.title().trim(),
                            draft.description().trim(),
                            "Expected behavior described by the test",
                            draft.priority()))
                    : draft.cases();

            List<TestCase> cases = new ArrayList<>();
            for (TestCaseDraft caseDraft : caseDrafts) {
                GeneratedTestCaseEntity caseEntity = new GeneratedTestCaseEntity(
                        UUID.randomUUID(),
                        resultId,
                        testId,
                        task.getOrganizationId(),
                        caseDraft.name().trim(),
                        caseDraft.steps(),
                        caseDraft.expectedResult(),
                        caseDraft.priority(),
                        now);
                caseRepository.save(caseEntity);
                TestCase mapped = toCase(caseEntity);
                cases.add(mapped);
                allCases.add(mapped);
            }
            tests.add(new GeneratedTest(
                    testId,
                    draft.type(),
                    draft.priority(),
                    draft.title().trim(),
                    draft.description().trim(),
                    matched != null ? matched.id() : null,
                    draft.artifactPath(),
                    List.copyOf(cases)));
        }

        List<ArtifactReference> reviewed = reviewedArtifactRepository
                .findByTestingResultIdOrderByPathAsc(resultId)
                .stream()
                .map(TestingStorageService::toArtifact)
                .toList();

        return toResult(result, tests, allCases, reviewed);
    }

    @Transactional(readOnly = true)
    public TestingResult findLatest(UUID taskId, UUID organizationId) {
        return resultRepository
                .findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(taskId, organizationId)
                .map(result -> {
                    List<GeneratedTestEntity> testEntities =
                            testRepository.findByTestingResultIdOrderByPriorityDescTitleAsc(result.getId());
                    List<GeneratedTestCaseEntity> caseEntities =
                            caseRepository.findByTestingResultIdOrderByNameAsc(result.getId());
                    Map<UUID, List<TestCase>> casesByTest = new LinkedHashMap<>();
                    List<TestCase> allCases = new ArrayList<>();
                    for (GeneratedTestCaseEntity caseEntity : caseEntities) {
                        TestCase mapped = toCase(caseEntity);
                        allCases.add(mapped);
                        casesByTest
                                .computeIfAbsent(caseEntity.getGeneratedTestId(), ignored -> new ArrayList<>())
                                .add(mapped);
                    }
                    List<GeneratedTest> tests = testEntities.stream()
                            .map(test -> new GeneratedTest(
                                    test.getId(),
                                    test.getTestType(),
                                    test.getPriority(),
                                    test.getTitle(),
                                    test.getDescription(),
                                    test.getArtifactId(),
                                    test.getArtifactPath(),
                                    List.copyOf(casesByTest.getOrDefault(test.getId(), List.of()))))
                            .toList();
                    List<ArtifactReference> reviewed = reviewedArtifactRepository
                            .findByTestingResultIdOrderByPathAsc(result.getId())
                            .stream()
                            .map(TestingStorageService::toArtifact)
                            .toList();
                    return toResult(result, tests, allCases, reviewed);
                })
                .orElse(null);
    }

    private static TestingResult toResult(
            TestingResultEntity result,
            List<GeneratedTest> tests,
            List<TestCase> cases,
            List<ArtifactReference> reviewed) {
        return new TestingResult(
                result.getId(),
                result.getTaskId(),
                result.getRunId(),
                result.getProjectId(),
                result.getSummary(),
                result.getCoverageEstimate(),
                tests,
                cases,
                reviewed,
                typeCounts(tests),
                priorityCounts(tests),
                result.getTokensUsed(),
                result.getModel(),
                result.getProvider(),
                result.getGenerationTimeMs(),
                result.getCreatedAt(),
                true);
    }

    private static Map<String, Long> typeCounts(List<GeneratedTest> tests) {
        Map<TestType, Long> counts = new EnumMap<>(TestType.class);
        for (TestType type : TestType.values()) {
            counts.put(type, 0L);
        }
        for (GeneratedTest test : tests) {
            counts.merge(test.type(), 1L, Long::sum);
        }
        Map<String, Long> out = new LinkedHashMap<>();
        counts.forEach((k, v) -> out.put(k.name(), v));
        return out;
    }

    private static Map<String, Long> priorityCounts(List<GeneratedTest> tests) {
        Map<TestPriority, Long> counts = new EnumMap<>(TestPriority.class);
        for (TestPriority priority : TestPriority.values()) {
            counts.put(priority, 0L);
        }
        for (GeneratedTest test : tests) {
            counts.merge(test.priority(), 1L, Long::sum);
        }
        Map<String, Long> out = new LinkedHashMap<>();
        counts.forEach((k, v) -> out.put(k.name(), v));
        return out;
    }

    private static TestCase toCase(GeneratedTestCaseEntity entity) {
        return new TestCase(
                entity.getId(),
                entity.getGeneratedTestId(),
                entity.getName(),
                entity.getSteps(),
                entity.getExpectedResult(),
                entity.getPriority());
    }

    private static ArtifactReference toArtifact(TestingReviewedArtifactEntity entity) {
        return new ArtifactReference(
                entity.getArtifactId(),
                entity.getPath(),
                entity.getFilename(),
                entity.getLanguage(),
                entity.getSha256());
    }
}
