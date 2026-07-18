package ai.nova.platform.planner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.orchestration.entity.ExecutionMode;
import ai.nova.platform.orchestration.entity.TaskType;
import ai.nova.platform.planner.service.PlannerJsonParser;
import ai.nova.platform.web.error.ApiException;

class PlannerJsonParserTest {

    private final PlannerJsonParser parser = new PlannerJsonParser(new ObjectMapper());

    @Test
    void parsesFencedJson() {
        String raw = """
                ```json
                {
                  "objective":"Build authentication",
                  "executionMode":"DEPENDENCY_GRAPH",
                  "failurePolicy":"FAIL_FAST",
                  "tasks":[
                    {"taskKey":"analysis","displayName":"Analyze","taskType":"AGENT_TURN","agentRole":"research","priority":1},
                    {"taskKey":"implementation","displayName":"Implement","taskType":"AGENT_TURN","agentRole":"coding","priority":2}
                  ],
                  "dependencies":[{"from":"analysis","to":"implementation","type":"SUCCESS"}]
                }
                ```
                """;
        var plan = parser.parse(raw, "fallback");
        assertThat(plan.objective()).isEqualTo("Build authentication");
        assertThat(plan.executionMode()).isEqualTo(ExecutionMode.DEPENDENCY_GRAPH);
        assertThat(plan.tasks()).hasSize(2);
        assertThat(plan.tasks().get(0).taskType()).isEqualTo(TaskType.AGENT_TURN);
        assertThat(plan.dependencies()).hasSize(1);
    }

    @Test
    void rejectsNonJson() {
        assertThatThrownBy(() -> parser.parse("not json", "obj"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PLANNER_INVALID_OUTPUT");
    }
}
