package ai.nova.platform.knowledge.embedding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.StringJoiner;

public final class EmbeddingCodec {

    private EmbeddingCodec() {
    }

    public static String encode(float[] vector) {
        StringJoiner joiner = new StringJoiner(",");
        for (float value : vector) {
            joiner.add(String.format(Locale.ROOT, "%.8f", value));
        }
        return joiner.toString();
    }

    public static float[] decode(String encoded) {
        String[] parts = encoded.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i].trim());
        }
        return vector;
    }

    public static String hash(float[] vector) {
        return sha256(encode(vector));
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
