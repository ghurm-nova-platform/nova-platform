package ai.nova.platform.knowledge.embedding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Component;

@Component
public class DeterministicLocalEmbeddingProvider implements EmbeddingProvider {

    public static final String KEY = "DETERMINISTIC_LOCAL";
    public static final String MODEL = "deterministic-v1";
    public static final int DIMENSIONS = 64;

    @Override
    public String providerKey() {
        return KEY;
    }

    @Override
    public String model() {
        return MODEL;
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    @Override
    public float[] embed(String text) {
        String input = text == null ? "" : text;
        byte[] seed = sha256(input.getBytes(StandardCharsets.UTF_8));
        float[] vector = new float[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) {
            int b0 = seed[i % seed.length] & 0xff;
            int b1 = seed[(i * 3 + 7) % seed.length] & 0xff;
            int b2 = seed[(i * 5 + 11) % seed.length] & 0xff;
            int raw = (b0 << 16) | (b1 << 8) | b2;
            double value = ((raw % 20001) / 10000.0) - 1.0;
            vector[i] = (float) value;
        }
        normalize(vector);
        return vector;
    }

    private static void normalize(float[] vector) {
        double sum = 0.0;
        for (float v : vector) {
            if (!Float.isFinite(v)) {
                throw new IllegalStateException("Non-finite embedding value");
            }
            sum += (double) v * v;
        }
        double norm = Math.sqrt(sum);
        if (norm == 0.0) {
            vector[0] = 1.0f;
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / norm);
        }
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
