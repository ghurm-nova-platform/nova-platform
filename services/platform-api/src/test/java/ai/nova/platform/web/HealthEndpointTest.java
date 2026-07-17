package ai.nova.platform.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import ai.nova.platform.web.correlation.CorrelationIdFilter;

@SpringBootTest
@AutoConfigureMockMvc
class HealthEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiHealthReturnsUp() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("platform-api"))
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }

    @Test
    void actuatorHealthReturnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void actuatorDoesNotExposeEnvEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void incomingCorrelationIdIsPropagated() throws Exception {
        String correlationId = "test-correlation-id-123";

        mockMvc.perform(get("/api/v1/health")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId));
    }
}
