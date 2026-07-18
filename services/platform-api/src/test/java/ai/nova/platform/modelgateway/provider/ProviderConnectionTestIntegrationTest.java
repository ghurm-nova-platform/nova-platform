package ai.nova.platform.modelgateway.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.modelgateway.config.ModelGatewayProperties;
import ai.nova.platform.modelgateway.entity.AiProvider;
import ai.nova.platform.modelgateway.entity.ConnectionTestStatus;
import ai.nova.platform.modelgateway.repository.AiProviderRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@SpringBootTest
@AutoConfigureMockMvc
class ProviderConnectionTestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ModelGatewayProperties properties;
    @Autowired
    private AiProviderRepository providerRepository;

    private MockWebServer server;
    private String accessToken;
    private String previousBaseUrl;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        previousBaseUrl = properties.getProviders().getOpenai().getBaseUrl();
        properties.getProviders().getOpenai().setBaseUrl(server.url("/").toString().replaceAll("/$", ""));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@nova.local","password":"ChangeMe123!"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }

    @AfterEach
    void tearDown() throws Exception {
        properties.getProviders().getOpenai().setBaseUrl(previousBaseUrl == null ? "" : previousBaseUrl);
        server.shutdown();
    }

    @Test
    void connectionTestSuccessAndFailureUpdateMetadata() throws Exception {
        MvcResult secretResult = mockMvc.perform(post("/api/provider-secrets")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "secretKey":"CONN_%s",
                                  "name":"Conn",
                                  "providerType":"OPENAI",
                                  "secret":"connection-test-key1"
                                }
                                """.formatted(UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase())))
                .andExpect(status().isCreated())
                .andReturn();
        String credentialReference = objectMapper
                .readTree(secretResult.getResponse().getContentAsString())
                .get("credentialReference")
                .asText();

        String providerKey =
                "OPENAI_CONN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        MvcResult createProvider = mockMvc.perform(post("/api/model-providers")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerKey":"%s",
                                  "name":"OpenAI Conn",
                                  "providerType":"OPENAI",
                                  "adapterKey":"OPENAI",
                                  "credentialReference":"%s",
                                  "endpointProfile":"OPENAI_PUBLIC"
                                }
                                """.formatted(providerKey, credentialReference)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode providerJson = objectMapper.readTree(createProvider.getResponse().getContentAsString());
        UUID providerId = UUID.fromString(providerJson.get("id").asText());

        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"data\":[]}"));
        mockMvc.perform(post("/api/model-providers/" + providerId + "/connection-test")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.testedAt").isNotEmpty());

        AiProvider afterSuccess = providerRepository.findById(providerId).orElseThrow();
        assertThat(afterSuccess.getLastConnectionTestStatus()).isEqualTo(ConnectionTestStatus.SUCCESS);
        assertThat(afterSuccess.getLastConnectionTestErrorCode()).isNull();
        assertThat(server.takeRequest(2, TimeUnit.SECONDS).getPath()).isEqualTo("/v1/models");

        server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"error\":\"nope\"}"));
        mockMvc.perform(post("/api/model-providers/" + providerId + "/connection-test")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorCode").value("PROVIDER_AUTHENTICATION_FAILED"));

        AiProvider afterFailure = providerRepository.findById(providerId).orElseThrow();
        assertThat(afterFailure.getLastConnectionTestStatus()).isEqualTo(ConnectionTestStatus.FAILED);
        assertThat(afterFailure.getLastConnectionTestErrorCode()).isEqualTo("PROVIDER_AUTHENTICATION_FAILED");
    }

    @Test
    void staleConnectionTestDoesNotMarkSuccessAfterConfigChange() throws Exception {
        MvcResult secretResult = mockMvc.perform(post("/api/provider-secrets")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "secretKey":"STALE_%s",
                                  "name":"Stale",
                                  "providerType":"OPENAI",
                                  "secret":"stale-test-key-aaaa"
                                }
                                """.formatted(UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase())))
                .andExpect(status().isCreated())
                .andReturn();
        String credentialReference = objectMapper
                .readTree(secretResult.getResponse().getContentAsString())
                .get("credentialReference")
                .asText();

        MvcResult secret2 = mockMvc.perform(post("/api/provider-secrets")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "secretKey":"STALE2_%s",
                                  "name":"Stale2",
                                  "providerType":"OPENAI",
                                  "secret":"stale-test-key-bbbb"
                                }
                                """.formatted(UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase())))
                .andExpect(status().isCreated())
                .andReturn();
        String newCredentialReference = objectMapper
                .readTree(secret2.getResponse().getContentAsString())
                .get("credentialReference")
                .asText();

        String providerKey =
                "OPENAI_STALE_" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        MvcResult createProvider = mockMvc.perform(post("/api/model-providers")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerKey":"%s",
                                  "name":"OpenAI Stale",
                                  "providerType":"OPENAI",
                                  "adapterKey":"OPENAI",
                                  "credentialReference":"%s",
                                  "endpointProfile":"OPENAI_PUBLIC"
                                }
                                """.formatted(providerKey, credentialReference)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode providerJson = objectMapper.readTree(createProvider.getResponse().getContentAsString());
        UUID providerId = UUID.fromString(providerJson.get("id").asText());
        int version = providerJson.get("version").asInt();

        server.enqueue(new MockResponse()
                .setBodyDelay(1500, TimeUnit.MILLISECONDS)
                .setResponseCode(200)
                .setBody("{\"data\":[]}"));

        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            java.util.concurrent.Future<MvcResult> testFuture = pool.submit(() -> mockMvc.perform(
                            post("/api/model-providers/" + providerId + "/connection-test")
                                    .header("Authorization", "Bearer " + accessToken))
                    .andReturn());

            // Wait until the out-of-TX HTTP probe has started against the old snapshot.
            assertThat(server.takeRequest(5, TimeUnit.SECONDS)).isNotNull();

            mockMvc.perform(put("/api/model-providers/" + providerId)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name":"OpenAI Stale",
                                      "credentialReference":"%s",
                                      "endpointProfile":"OPENAI_PUBLIC",
                                      "version":%d
                                    }
                                    """.formatted(newCredentialReference, version)))
                    .andExpect(status().isOk());

            AiProvider midUpdate = providerRepository.findById(providerId).orElseThrow();
            assertThat(midUpdate.getLastConnectionTestStatus()).isEqualTo(ConnectionTestStatus.NEVER);

            MvcResult testResult = testFuture.get(10, TimeUnit.SECONDS);
            assertThat(testResult.getResponse().getStatus()).isEqualTo(200);
            JsonNode body = objectMapper.readTree(testResult.getResponse().getContentAsString());
            assertThat(body.get("status").asText()).isEqualTo("NEVER");
            assertThat(body.get("errorCode").asText()).isEqualTo("CONNECTION_TEST_STALE");

            AiProvider after = providerRepository.findById(providerId).orElseThrow();
            assertThat(after.getLastConnectionTestStatus()).isEqualTo(ConnectionTestStatus.NEVER);
            assertThat(after.getLastConnectionTestErrorCode()).isEqualTo("CONNECTION_TEST_STALE");
            assertThat(after.getCredentialReference()).isEqualTo(newCredentialReference);
        } finally {
            pool.shutdownNow();
        }
    }
}
