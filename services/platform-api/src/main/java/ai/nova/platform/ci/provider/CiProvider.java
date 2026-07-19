package ai.nova.platform.ci.provider;

import java.time.Instant;
import java.util.List;

import ai.nova.platform.pullrequest.provider.RepositoryRef;

public interface CiProvider {

    String providerId();

    List<ProviderWorkflowRun> listWorkflowRuns(RepositoryRef ref, WorkflowRunQuery query, String token);

    List<ProviderJob> listJobs(RepositoryRef ref, String externalRunId, String token);
}
