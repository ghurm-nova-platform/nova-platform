package ai.nova.platform.repair.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import ai.nova.platform.repair.entity.RepairInputSource;

public final class RepairFingerprint {

    private RepairFingerprint() {
    }

    public record InputLine(RepairInputSource sourceType, String detail) {
    }

    public static String compute(List<InputLine> inputs, UUID priorPatchResultId) {
        List<InputLine> sorted = new ArrayList<>(inputs);
        sorted.sort(Comparator.comparingInt((InputLine line) -> line.sourceType().priority())
                .thenComparing(line -> line.sourceType().name())
                .thenComparing(InputLine::detail));

        StringBuilder payload = new StringBuilder();
        for (InputLine line : sorted) {
            payload.append(line.sourceType().name()).append('|').append(line.detail()).append('\n');
        }
        // priorPatchResultId is intentionally omitted from the hash so a successful repair
        // remains idempotent after appendResult makes the new patch the latest result.

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
