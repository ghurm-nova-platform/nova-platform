package ai.nova.platform.modelgateway.routing;

import java.util.UUID;

import ai.nova.platform.modelgateway.entity.AgentModelAssignment;
import ai.nova.platform.modelgateway.entity.AiModel;
import ai.nova.platform.modelgateway.entity.AiProvider;
import ai.nova.platform.modelgateway.entity.ProjectModel;

public record RoutedModelCandidate(
        AgentModelAssignment assignment,
        AiModel model,
        AiProvider provider,
        ProjectModel projectModel,
        boolean fallback) {
}
