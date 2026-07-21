package ai.nova.platform.llm.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import ai.nova.platform.llm.configuration.LlmProperties;
import ai.nova.platform.llm.dto.LlmDtos.ConfigResponse;

@Service
public class LlmRuntimeInfoService {

    private final LlmProperties properties;
    private final UsageMetricsService usageMetricsService;

    public LlmRuntimeInfoService(LlmProperties properties, UsageMetricsService usageMetricsService) {
        this.properties = properties;
        this.usageMetricsService = usageMetricsService;
    }

    public ConfigResponse config() {
        return new ConfigResponse(
                properties.isEnabled(),
                properties.getDefaultProvider(),
                properties.isFallbackToDeterministic(),
                properties.getTimeout().getSeconds(),
                properties.getOllama().isEnabled(),
                properties.getLlamacpp().isEnabled(),
                properties.getVllm().isEnabled());
    }

    public Map<String, Object> metricsSummary() {
        return usageMetricsService.summary();
    }
}
