package ai.nova.platform.repair.service;

public interface RepairStrategy {

    String strategyId();

    RepairProposal propose(RepairContext context);
}
