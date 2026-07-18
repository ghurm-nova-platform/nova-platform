package ai.nova.platform.modelgateway.usage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.modelgateway.config.ModelGatewayProperties;
import ai.nova.platform.modelgateway.entity.AiModel;
import ai.nova.platform.modelgateway.entity.InvocationStatus;
import ai.nova.platform.modelgateway.entity.ModelInvocation;
import ai.nova.platform.modelgateway.entity.ModelUsageDaily;
import ai.nova.platform.modelgateway.repository.ModelUsageDailyRepository;

@Service
public class ModelUsageRecorder {

    private final ModelUsageDailyRepository usageRepository;
    private final ModelGatewayProperties properties;

    public ModelUsageRecorder(ModelUsageDailyRepository usageRepository, ModelGatewayProperties properties) {
        this.usageRepository = usageRepository;
        this.properties = properties;
    }

    @Transactional
    public void record(ModelInvocation invocation, AiModel model, boolean success) {
        if (!properties.isUsageEnabled()) {
            return;
        }
        LocalDate usageDate = invocation.getStartedAt().atZone(ZoneOffset.UTC).toLocalDate();
        ModelUsageDaily usage = usageRepository
                .findForUpdate(invocation.getProjectId(), invocation.getModelId(), usageDate)
                .orElseGet(() -> createUsage(invocation, usageDate));

        usage.setRequestCount(usage.getRequestCount() + 1);
        if (success) {
            usage.setSuccessfulRequestCount(usage.getSuccessfulRequestCount() + 1);
        } else {
            usage.setFailedRequestCount(usage.getFailedRequestCount() + 1);
        }
        long inputTokens = invocation.getEstimatedInputTokens() != null ? invocation.getEstimatedInputTokens() : 0L;
        long outputTokens = invocation.getEstimatedOutputTokens() != null ? invocation.getEstimatedOutputTokens() : 0L;
        usage.setInputTokens(usage.getInputTokens() + inputTokens);
        usage.setOutputTokens(usage.getOutputTokens() + outputTokens);

        if (properties.isCostEstimationEnabled()) {
            usage.setEstimatedCost(estimateCost(usage, model));
            usage.setCurrencyCode(model.getCurrencyCode());
        }
        usage.setUpdatedAt(Instant.now());
        usageRepository.save(usage);
    }

    private ModelUsageDaily createUsage(ModelInvocation invocation, LocalDate usageDate) {
        ModelUsageDaily usage = new ModelUsageDaily();
        usage.setId(UUID.randomUUID());
        usage.setOrganizationId(invocation.getOrganizationId());
        usage.setProjectId(invocation.getProjectId());
        usage.setProviderId(invocation.getProviderId());
        usage.setModelId(invocation.getModelId());
        usage.setUsageDate(usageDate);
        usage.setRequestCount(0L);
        usage.setSuccessfulRequestCount(0L);
        usage.setFailedRequestCount(0L);
        usage.setInputTokens(0L);
        usage.setOutputTokens(0L);
        usage.setUpdatedAt(Instant.now());
        return usage;
    }

    private BigDecimal estimateCost(ModelUsageDaily usage, AiModel model) {
        BigDecimal cost = BigDecimal.ZERO;
        if (model.getInputCostPerMillion() != null) {
            cost = cost.add(model.getInputCostPerMillion()
                    .multiply(BigDecimal.valueOf(usage.getInputTokens()))
                    .divide(BigDecimal.valueOf(1_000_000L), 8, RoundingMode.HALF_UP));
        }
        if (model.getOutputCostPerMillion() != null) {
            cost = cost.add(model.getOutputCostPerMillion()
                    .multiply(BigDecimal.valueOf(usage.getOutputTokens()))
                    .divide(BigDecimal.valueOf(1_000_000L), 8, RoundingMode.HALF_UP));
        }
        return cost;
    }

    public static boolean isSuccessful(InvocationStatus status) {
        return status == InvocationStatus.COMPLETED;
    }
}
