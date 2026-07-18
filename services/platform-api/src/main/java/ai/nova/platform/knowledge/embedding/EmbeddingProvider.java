package ai.nova.platform.knowledge.embedding;

import java.util.List;

public interface EmbeddingProvider {

    String providerKey();

    String model();

    int dimensions();

    float[] embed(String text);

    default List<float[]> embedBatch(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }
}
