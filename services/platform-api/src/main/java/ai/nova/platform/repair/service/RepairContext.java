package ai.nova.platform.repair.service;

import java.util.List;
import java.util.UUID;

import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.repair.service.RepairInputCollector.CollectedInput;

public record RepairContext(
        AgentOrchestrationTask task,
        PatchResult priorPatch,
        List<GeneratedArtifactResponse> artifacts,
        List<CollectedInput> failureInputs,
        String provider,
        String model,
        UUID organizationId,
        UUID projectId) {
}
