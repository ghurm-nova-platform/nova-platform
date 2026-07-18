package ai.nova.platform.modelgateway.routing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.modelgateway.gateway.ModelGatewayRequest;
import ai.nova.platform.modelgateway.routing.ModelRoutingService.ResolvedRouting;

@SpringBootTest
class ModelRoutingServiceTest {

    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PROJECT_ID = UUID.fromString("55555555-5555-5555-5555-555555555501");
    private static final UUID AGENT_ID = UUID.fromString("66666666-6666-6666-6666-666666666601");
    private static final UUID EXECUTION_ID = UUID.randomUUID();

    @Autowired
    private ModelRoutingService routingService;

    @Test
    void resolvesDemoAgentPrimaryModel() {
        ModelGatewayRequest request = new ModelGatewayRequest(
                ORG_ID,
                PROJECT_ID,
                AGENT_ID,
                EXECUTION_ID,
                null,
                UUID.fromString("44444444-4444-4444-4444-444444444401"),
                "system",
                List.of(),
                List.of(),
                List.of(),
                null,
                false,
                false);

        ResolvedRouting routing = routingService.resolve(request);
        assertThat(routing.candidates()).isNotEmpty();
        assertThat(routing.candidates().getFirst().model().getModelKey()).isEqualTo("DETERMINISTIC_CHAT_V1");
        assertThat(routing.policy()).isNotNull();
        assertThat(routing.policy().getStrategy().name()).isEqualTo("PRIORITY_FALLBACK");
    }
}
