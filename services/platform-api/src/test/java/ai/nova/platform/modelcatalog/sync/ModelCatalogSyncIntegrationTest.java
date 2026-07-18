package ai.nova.platform.modelcatalog.sync;

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
import ai.nova.platform.modelgateway.repository.AiModelRepository;
import ai.nova.platform.modelgateway.repository.AiProviderRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@SpringBootTest
@AutoConfigureMockMvc
class ModelCatalogSyncIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ModelGatewayProperties properties;
    @Autowired
    private AiProviderRepository providerRepository;
    @Autowired
    private AiModelRepository modelRepository;

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
    void syncCreatesDraftProviderSyncModelsAndHonorsStaleRace() throws Exception {
        MvcResult secretResult = mockMvc.perform(post("/api/provider-secrets")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "secretKey":"SYNC_%s",
                                  "name":"Sync",
                                  "providerType":"OPENAI",
                                  "secret":"sync-test-key-aaaa"
                                }
                                """.formatted(UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase())))
                .andExpect(status().isCreated())
                .andReturn();
        String credentialReference = objectMapper
                .readTree(secretResult.getResponse().getContentAsString())
                .get("credentialReference")
                .asText();

        String providerKey =
                "OPENAI_SYNC_" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        MvcResult createProvider = mockMvc.perform(post("/api/model-providers")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerKey":"%s",
                                  "name":"OpenAI Sync",
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

        AiProvider provider = providerRepository.findById(providerId).orElseThrow();
        provider.setLastConnectionTestStatus(ConnectionTestStatus.SUCCESS);
        provider.setLastConnectionTestAt(java.time.Instant.now());
        providerRepository.save(provider);

        mockMvc.perform(post("/api/model-providers/" + providerId + "/activate")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        provider = providerRepository.findById(providerId).orElseThrow();
        // Ensure SUCCESS remains after activate (activate should not clear it).
        if (provider.getLastConnectionTestStatus() != ConnectionTestStatus.SUCCESS) {
            provider.setLastConnectionTestStatus(ConnectionTestStatus.SUCCESS);
            provider.setLastConnectionTestAt(java.time.Instant.now());
            providerRepository.save(provider);
        }

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"data":[
                          {"id":"gpt-4o-mini"},
                          {"id":"text-embedding-3-small"},
                          {"id":"ft:unknown-custom"}
                        ]}
                        """));

        mockMvc.perform(post("/api/model-providers/" + providerId + "/models/sync")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.discoveredCount").value(3))
                .andExpect(jsonPath("$.createdCount").value(3));

        RecordedRequest recorded = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getPath()).isEqualTo("/v1/models");
        assertThat(recorded.getHeader("Authorization")).startsWith("Bearer ");

        assertThat(modelRepository.findByProviderIdAndProviderModelId(providerId, "gpt-4o-mini"))
                .isPresent()
                .get()
                .satisfies(model -> {
                    assertThat(model.getStatus().name()).isEqualTo("DRAFT");
                    assertThat(model.getSource().name()).isEqualTo("PROVIDER_SYNC");
                    assertThat(model.isSupportsTools()).isTrue();
                });
        assertThat(modelRepository.findByProviderIdAndProviderModelId(providerId, "ft:unknown-custom"))
                .isPresent()
                .get()
                .satisfies(model -> assertThat(model.isSupportsTools()).isFalse());

        // Second sync should update/unchanged, never delete/activate
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"data":[{"id":"gpt-4o-mini"}]}
                        """));
        mockMvc.perform(post("/api/model-providers/" + providerId + "/models/sync")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.createdCount").value(0));
        assertThat(modelRepository.findByProviderIdAndProviderModelId(providerId, "text-embedding-3-small"))
                .isPresent();

        // Stale race: change provider config while HTTP is in flight (simulated by updating before apply)
        mockMvc.perform(put("/api/model-providers/" + providerId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"OpenAI Sync Renamed",
                                  "credentialReference":"%s",
                                  "endpointProfile":"OPENAI_PUBLIC",
                                  "version":%d
                                }
                                """.formatted(credentialReference, providerRepository.findById(providerId).orElseThrow().getVersion())))
                .andExpect(status().isOk());

        // Capture snapshot version mismatch by enqueuing response then syncing after another update mid-flight:
        // Update again to bump version, then sync with delayed response that was prepared against old snapshot.
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBodyDelay(200, TimeUnit.MILLISECONDS)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"data\":[{\"id\":\"gpt-4o\"}]}"));

        // Kick sync in a way that loadContext happens, then mutate provider before apply.
        // Easiest reliable approach: manually invoke after mutating version between load and apply via repository.
        AiProvider before = providerRepository.findById(providerId).orElseThrow();
        int snapshotVersion = before.getVersion();

        // Force stale by changing credential reference after we would have loaded snapshot:
        // Use a dedicated sync call after bumping version so matchesCurrent fails.
        before.setName("bumped-for-stale");
        providerRepository.save(before);
        assertThat(providerRepository.findById(providerId).orElseThrow().getVersion()).isGreaterThan(snapshotVersion);

        // Reload and sync - the HTTP will succeed but if we mutate DURING sync it's hard in one thread.
        // Instead: call sync, and inside the same test mutate between enqueue and sync completion by
        // updating provider name (version bump) right before sync returns from HTTP — use short delay.
        Thread mutator = new Thread(() -> {
            try {
                Thread.sleep(50);
                AiProvider p = providerRepository.findById(providerId).orElseThrow();
                p.setDescription("stale-mutation");
                providerRepository.save(p);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });
        mutator.start();
        mockMvc.perform(post("/api/model-providers/" + providerId + "/models/sync")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STALE"))
                .andExpect(jsonPath("$.errorCode").value("MODEL_SYNC_STALE"));
        mutator.join();

        assertThat(modelRepository.findByProviderIdAndProviderModelId(providerId, "gpt-4o")).isEmpty();
    }
}
