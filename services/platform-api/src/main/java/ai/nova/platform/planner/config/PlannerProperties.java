package ai.nova.platform.planner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.planner")
public class PlannerProperties {

    private boolean enabled = true;
    private int maxObjectiveLength = 4000;
    private int maxTasks = 50;
    private int maxDependencies = 200;
    private double costPerThousandTokensUsd = 0.002;
    private String defaultProvider = "LOCAL";
    private String defaultModel = "planner-local";
    private String defaultSystemPrompt =
            """
            You are the Nova Planner Agent. Produce a complete multi-agent execution plan as JSON only.
            Do not execute tools, shell, git, browser automation, or MCP. Do not run workflows.
            Return only a single JSON object matching the documented schema with tasks and dependencies.
            Use agentRole values from the allowed vocabulary. Prefer DEPENDENCY_GRAPH for multi-step work.
            """;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxObjectiveLength() {
        return maxObjectiveLength;
    }

    public void setMaxObjectiveLength(int maxObjectiveLength) {
        this.maxObjectiveLength = maxObjectiveLength;
    }

    public int getMaxTasks() {
        return maxTasks;
    }

    public void setMaxTasks(int maxTasks) {
        this.maxTasks = maxTasks;
    }

    public int getMaxDependencies() {
        return maxDependencies;
    }

    public void setMaxDependencies(int maxDependencies) {
        this.maxDependencies = maxDependencies;
    }

    public double getCostPerThousandTokensUsd() {
        return costPerThousandTokensUsd;
    }

    public void setCostPerThousandTokensUsd(double costPerThousandTokensUsd) {
        this.costPerThousandTokensUsd = costPerThousandTokensUsd;
    }

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public String getDefaultSystemPrompt() {
        return defaultSystemPrompt;
    }

    public void setDefaultSystemPrompt(String defaultSystemPrompt) {
        this.defaultSystemPrompt = defaultSystemPrompt;
    }
}
