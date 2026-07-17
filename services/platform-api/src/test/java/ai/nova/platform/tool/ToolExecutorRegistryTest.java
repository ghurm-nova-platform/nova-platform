package ai.nova.platform.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import ai.nova.platform.tool.executor.CalculatorToolExecutor;
import ai.nova.platform.tool.executor.CurrentDateTimeToolExecutor;
import ai.nova.platform.tool.executor.TextStatisticsToolExecutor;
import ai.nova.platform.tool.executor.ToolExecutor;
import ai.nova.platform.tool.executor.ToolExecutorRegistry;
import ai.nova.platform.tool.validation.ToolInputValidator;

class ToolExecutorRegistryTest {

    @Test
    void registersBuiltInExecutors() {
        ToolExecutorRegistry registry = new ToolExecutorRegistry(List.of(
                new CurrentDateTimeToolExecutor(new com.fasterxml.jackson.databind.ObjectMapper(), new ToolInputValidator()),
                new CalculatorToolExecutor(new com.fasterxml.jackson.databind.ObjectMapper(), new ToolInputValidator()),
                new TextStatisticsToolExecutor(new com.fasterxml.jackson.databind.ObjectMapper(), new ToolInputValidator())));

        assertThat(registry.allowedKeys())
                .containsExactlyInAnyOrder("CURRENT_DATETIME", "CALCULATOR", "TEXT_STATISTICS");
        assertThat(registry.find("CALCULATOR")).isPresent();
    }

    @Test
    void failsOnDuplicateExecutorKeys() {
        ToolExecutor duplicate = new ToolExecutor() {
            @Override
            public String executorKey() {
                return "CALCULATOR";
            }

            @Override
            public ai.nova.platform.tool.executor.ToolExecutionResult execute(
                    ai.nova.platform.tool.executor.ToolExecutionContext context,
                    com.fasterxml.jackson.databind.JsonNode input) {
                return null;
            }
        };

        assertThatThrownBy(() -> new ToolExecutorRegistry(List.of(duplicate, duplicate)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate tool executor key");
    }
}
