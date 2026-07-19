package ai.nova.platform.repair.service;

import java.util.List;

import ai.nova.platform.repair.dto.RepairDtos.RepairAction;

public record RepairProposal(
        String summary,
        double confidence,
        String reason,
        List<String> repairedFiles,
        String unifiedDiffPatch,
        List<RepairAction> actions) {
}
