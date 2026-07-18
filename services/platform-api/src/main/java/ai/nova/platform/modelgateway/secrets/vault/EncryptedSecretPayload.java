package ai.nova.platform.modelgateway.secrets.vault;

public record EncryptedSecretPayload(
        byte[] ciphertext,
        byte[] nonce,
        int keyVersion,
        String algorithm) {
}
