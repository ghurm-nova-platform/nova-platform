package ai.nova.platform.modelcatalog.sync;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import ai.nova.platform.modelcatalog.entity.AiModelCapability;
import ai.nova.platform.modelgateway.entity.AiModelType;

/**
 * Explicit capability rules for known OpenAI model families only. Unknown ids get no capabilities.
 */
@Component
public class OpenAiModelCapabilityMapper {

    public Set<AiModelCapability> mapCapabilities(String providerModelId) {
        if (providerModelId == null || providerModelId.isBlank()) {
            return Set.of();
        }
        String id = providerModelId.toLowerCase(Locale.ROOT);
        if (id.startsWith("text-embedding") || id.startsWith("embedding")) {
            return EnumSet.of(AiModelCapability.EMBEDDINGS);
        }
        if (id.startsWith("whisper") || id.contains("transcribe")) {
            return EnumSet.of(AiModelCapability.TRANSCRIPTION, AiModelCapability.AUDIO_INPUT);
        }
        if (id.startsWith("tts-") || id.contains("tts")) {
            return EnumSet.of(AiModelCapability.TEXT_TO_SPEECH, AiModelCapability.AUDIO_OUTPUT);
        }
        if (id.startsWith("dall-e") || id.startsWith("gpt-image")) {
            return EnumSet.of(AiModelCapability.IMAGE_GENERATION);
        }
        if (isKnownChatFamily(id)) {
            EnumSet<AiModelCapability> caps = EnumSet.of(
                    AiModelCapability.CHAT,
                    AiModelCapability.STREAMING,
                    AiModelCapability.JSON_MODE,
                    AiModelCapability.FUNCTION_CALLING,
                    AiModelCapability.TOOL_CALLING);
            if (id.contains("gpt-4o") || id.contains("vision") || id.contains("gpt-4.1") || id.contains("o1")
                    || id.contains("o3") || id.contains("o4")) {
                caps.add(AiModelCapability.VISION);
                caps.add(AiModelCapability.IMAGE_UNDERSTANDING);
            }
            if (id.startsWith("o1") || id.startsWith("o3") || id.startsWith("o4") || id.contains("reason")) {
                caps.add(AiModelCapability.REASONING);
            }
            if (id.contains("gpt-4o") || id.contains("gpt-4.1") || id.contains("gpt-5")) {
                caps.add(AiModelCapability.STRUCTURED_OUTPUT);
                caps.add(AiModelCapability.PARALLEL_TOOL_CALLING);
            }
            return caps;
        }
        return Set.of();
    }

    public AiModelType mapType(String providerModelId) {
        Set<AiModelCapability> caps = mapCapabilities(providerModelId);
        if (caps.contains(AiModelCapability.EMBEDDINGS)) {
            return AiModelType.EMBEDDING;
        }
        if (caps.contains(AiModelCapability.VISION) || caps.contains(AiModelCapability.IMAGE_GENERATION)) {
            return AiModelType.MULTIMODAL;
        }
        if (caps.contains(AiModelCapability.REASONING)) {
            return AiModelType.REASONING;
        }
        if (caps.contains(AiModelCapability.CHAT)) {
            return AiModelType.CHAT;
        }
        return AiModelType.CHAT;
    }

    public String mapFamily(String providerModelId) {
        if (providerModelId == null) {
            return null;
        }
        String id = providerModelId.toLowerCase(Locale.ROOT);
        if (id.startsWith("gpt-4o")) {
            return "gpt-4o";
        }
        if (id.startsWith("gpt-4.1")) {
            return "gpt-4.1";
        }
        if (id.startsWith("gpt-4")) {
            return "gpt-4";
        }
        if (id.startsWith("gpt-3.5")) {
            return "gpt-3.5";
        }
        if (id.startsWith("gpt-5")) {
            return "gpt-5";
        }
        if (id.startsWith("o1")) {
            return "o1";
        }
        if (id.startsWith("o3")) {
            return "o3";
        }
        if (id.startsWith("o4")) {
            return "o4";
        }
        if (id.startsWith("text-embedding")) {
            return "text-embedding";
        }
        if (id.startsWith("whisper")) {
            return "whisper";
        }
        if (id.startsWith("dall-e")) {
            return "dall-e";
        }
        return null;
    }

    public Integer mapContextWindow(String providerModelId) {
        if (providerModelId == null) {
            return null;
        }
        String id = providerModelId.toLowerCase(Locale.ROOT);
        if (id.contains("gpt-4o") || id.contains("gpt-4.1") || id.contains("gpt-4-turbo")) {
            return 128_000;
        }
        if (id.startsWith("gpt-4")) {
            return 8_192;
        }
        if (id.startsWith("gpt-3.5")) {
            return 16_385;
        }
        if (id.startsWith("o1") || id.startsWith("o3") || id.startsWith("o4")) {
            return 200_000;
        }
        if (id.startsWith("text-embedding-3")) {
            return 8_191;
        }
        return null;
    }

    public Integer mapMaxOutputTokens(String providerModelId) {
        Integer context = mapContextWindow(providerModelId);
        if (context == null) {
            return null;
        }
        return Math.min(context, 16_384);
    }

    public boolean isKnownChatFamily(String id) {
        return id.startsWith("gpt-")
                || id.startsWith("o1")
                || id.startsWith("o3")
                || id.startsWith("o4")
                || id.startsWith("chatgpt");
    }

    public List<AiModelCapability> asList(String providerModelId) {
        return new ArrayList<>(mapCapabilities(providerModelId));
    }
}
