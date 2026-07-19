package ai.nova.platform.ci.provider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.pullrequest.provider.RepositoryRef;
import ai.nova.platform.web.error.ApiException;

/**
 * In-memory CI provider for tests and local development. Never stores or logs credentials.
 */
@Component
@ConditionalOnProperty(name = "nova.ci-observation.provider", havingValue = "LOCAL")
public class InMemoryCiProvider implements CiProvider {

    private static final String PROVIDER_ID = "LOCAL";

    private final Map<String, List<ProviderWorkflowRun>> runsByRepo = new ConcurrentHashMap<>();
    private final Map<String, List<ProviderJob>> jobsByRunId = new ConcurrentHashMap<>();

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderWorkflowRun> listWorkflowRuns(RepositoryRef ref, WorkflowRunQuery query, String token) {
        String key = repoKey(ref);
        List<ProviderWorkflowRun> all = runsByRepo.getOrDefault(key, List.of());
        List<ProviderWorkflowRun> filtered = new ArrayList<>();
        for (ProviderWorkflowRun run : all) {
            if (query.branch() != null
                    && !query.branch().isBlank()
                    && run.branch() != null
                    && !run.branch().equals(query.branch())) {
                continue;
            }
            if (query.commitHash() != null
                    && !query.commitHash().isBlank()
                    && run.commitHash() != null
                    && !run.commitHash().equalsIgnoreCase(query.commitHash())) {
                continue;
            }
            if (query.pullRequestNumber() != null
                    && run.pullRequestNumber() != null
                    && !query.pullRequestNumber().equals(run.pullRequestNumber())) {
                continue;
            }
            filtered.add(run);
            if (filtered.size() >= query.maxRuns()) {
                break;
            }
        }
        return List.copyOf(filtered);
    }

    @Override
    public List<ProviderJob> listJobs(RepositoryRef ref, String externalRunId, String token) {
        String key = jobsKey(ref, externalRunId);
        return List.copyOf(jobsByRunId.getOrDefault(key, List.of()));
    }

    public void seedWorkflowRun(RepositoryRef ref, ProviderWorkflowRun run) {
        String key = repoKey(ref);
        runsByRepo.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(run);
    }

    public void seedJobs(RepositoryRef ref, String externalRunId, List<ProviderJob> jobs) {
        String key = jobsKey(ref, externalRunId);
        jobsByRunId.put(key, new CopyOnWriteArrayList<>(jobs));
    }

    public void clear() {
        runsByRepo.clear();
        jobsByRunId.clear();
    }

    public void requireRepository(RepositoryRef ref) {
        if (ref.owner() == null || ref.owner().isBlank() || ref.name() == null || ref.name().isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "CI_REPOSITORY_MISMATCH", "Repository owner/name required");
        }
    }

    private static String repoKey(RepositoryRef ref) {
        return ref.owner() + "/" + ref.name();
    }

    private static String jobsKey(RepositoryRef ref, String externalRunId) {
        return repoKey(ref) + "#run=" + externalRunId;
    }

    public static ProviderWorkflowRun sampleRun(
            String externalRunId,
            String workflowName,
            String branch,
            String commitHash,
            long pullRequestNumber,
            String status,
            String conclusion) {
        Instant now = Instant.now();
        return new ProviderWorkflowRun(
                "wf-1",
                workflowName,
                externalRunId,
                "memory://ci/" + externalRunId,
                status,
                conclusion,
                1000L,
                "pull_request",
                commitHash,
                branch,
                pullRequestNumber,
                "failure".equalsIgnoreCase(conclusion) ? "Workflow failed" : null,
                now.minusSeconds(60),
                now);
    }

    public static ProviderJob sampleJob(String externalJobId, String jobName, String conclusion, List<ProviderStep> steps) {
        Instant now = Instant.now();
        return new ProviderJob(
                externalJobId,
                jobName,
                "completed",
                conclusion,
                500L,
                "failure".equalsIgnoreCase(conclusion) ? "Job failed" : null,
                steps == null ? List.of() : steps,
                now.minusSeconds(30),
                now);
    }

    public static ProviderStep sampleStep(int number, String name, String conclusion) {
        Instant now = Instant.now();
        return new ProviderStep(
                number,
                name,
                "completed",
                conclusion,
                100L,
                "failure".equalsIgnoreCase(conclusion) ? "Step failed" : null,
                now.minusSeconds(10),
                now);
    }
}
