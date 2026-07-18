package ai.nova.platform.modelcatalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import ai.nova.platform.modelcatalog.entity.AiModelAlias;
import ai.nova.platform.modelcatalog.repository.AiModelAliasRepository;
import ai.nova.platform.modelcatalog.service.ModelReferenceResolver.ResolvedModel;
import ai.nova.platform.modelgateway.entity.AiModel;
import ai.nova.platform.modelgateway.entity.AiModelStatus;
import ai.nova.platform.modelgateway.repository.AiModelRepository;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest
class ModelReferenceResolverTest {

    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DEMO_MODEL_ID = UUID.fromString("99999999-9999-9999-9999-999999999911");

    @Autowired
    private ModelReferenceResolver resolver;
    @Autowired
    private AiModelRepository modelRepository;
    @Autowired
    private AiModelAliasRepository aliasRepository;

    @Test
    void resolvesByCanonicalModelKey() {
        ResolvedModel resolved = resolver.resolve(ORG_ID, "deterministic-chat-v1");
        assertThat(resolved.model().getId()).isEqualTo(DEMO_MODEL_ID);
        assertThat(resolved.canonicalKey()).isEqualTo("deterministic-chat-v1");
        assertThat(resolved.provider()).isNotNull();
    }

    @Test
    void resolvesByAlias() {
        AiModelAlias alias = new AiModelAlias();
        alias.setId(UUID.randomUUID());
        alias.setOrganizationId(ORG_ID);
        alias.setModelId(DEMO_MODEL_ID);
        alias.setAlias("Fast-Local");
        alias.setNormalizedAlias("fast-local");
        alias.setCreatedBy(UUID.fromString("44444444-4444-4444-4444-444444444401"));
        alias.setCreatedAt(java.time.Instant.now());
        aliasRepository.save(alias);

        ResolvedModel resolved = resolver.resolve(ORG_ID, "FAST-LOCAL");
        assertThat(resolved.model().getId()).isEqualTo(DEMO_MODEL_ID);

        aliasRepository.delete(alias);
    }

    @Test
    void rejectsAmbiguousKeyAndAlias() {
        AiModel other = modelRepository.findById(DEMO_MODEL_ID).orElseThrow();
        // Create a second active model then alias its key to the first — skip if not practical;
        // instead create alias equal to a different active model's key scenario via fake alias
        // pointing at demo while another active model uses that normalized name as key.
        // Simpler path: alias "deterministic-chat-v1" cannot be created for another model in service,
        // so simulate by inserting alias row for a non-demo model with normalized = demo key after
        // temporarily renaming is not needed — insert alias for demo with same normalized as key
        // and ensure another ACTIVE model exists... Seed only has one ACTIVE model.
        // Create alias on demo with normalized that equals another model's key after creating one.
        UUID providerId = other.getProviderId();
        AiModel second = new AiModel();
        second.setId(UUID.randomUUID());
        second.setOrganizationId(ORG_ID);
        second.setProviderId(providerId);
        second.setModelKey("alt-chat-v1");
        second.setProviderModelId("alt-chat-v1-" + UUID.randomUUID());
        second.setDisplayName("Alt");
        second.setModelType(other.getModelType());
        second.setStatus(AiModelStatus.ACTIVE);
        second.setSource(ai.nova.platform.modelgateway.entity.AiModelSource.MANUAL);
        second.setContextWindowTokens(4096);
        second.setContextWindow(4096);
        second.setMaxOutputTokens(1024);
        second.setSupportsTools(false);
        second.setSupportsKnowledgeContext(true);
        second.setSupportsJsonOutput(false);
        second.setSupportsStreaming(false);
        second.setSupportsSystemMessages(true);
        second.setCreatedBy(other.getCreatedBy());
        second.setUpdatedBy(other.getUpdatedBy());
        second.setCreatedAt(java.time.Instant.now());
        second.setUpdatedAt(java.time.Instant.now());
        modelRepository.saveAndFlush(second);

        AiModelAlias alias = new AiModelAlias();
        alias.setId(UUID.randomUUID());
        alias.setOrganizationId(ORG_ID);
        alias.setModelId(second.getId());
        alias.setAlias("deterministic-chat-v1");
        alias.setNormalizedAlias("deterministic-chat-v1");
        alias.setCreatedBy(other.getCreatedBy());
        alias.setCreatedAt(java.time.Instant.now());
        aliasRepository.save(alias);

        assertThatThrownBy(() -> resolver.resolve(ORG_ID, "deterministic-chat-v1"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getCode()).isEqualTo("MODEL_REFERENCE_AMBIGUOUS");
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });

        aliasRepository.delete(alias);
        modelRepository.delete(second);
    }
}
