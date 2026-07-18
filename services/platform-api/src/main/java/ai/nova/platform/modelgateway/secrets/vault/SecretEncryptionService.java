package ai.nova.platform.modelgateway.secrets.vault;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.web.error.ApiException;

@Service
public class SecretEncryptionService {

    public static final String ALGORITHM = "AES-256-GCM";
    public static final int KEY_VERSION = 1;
    private static final int GCM_TAG_BITS = 128;
    private static final int NONCE_BYTES = 12;
    private static final int KEY_BYTES = 32;

    private final SecretsProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public SecretEncryptionService(SecretsProperties properties) {
        this.properties = properties;
    }

    public EncryptedSecretPayload encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SECRET_EMPTY", "Secret value is required");
        }
        SecretKey key = requireMasterKey();
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            secureRandom.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return new EncryptedSecretPayload(ciphertext, nonce, KEY_VERSION, ALGORITHM);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SECRET_ENCRYPT_FAILED", "Failed to encrypt secret");
        }
    }

    public String decrypt(byte[] ciphertext, byte[] nonce, int keyVersion) {
        if (ciphertext == null || nonce == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SECRET_DECRYPT_FAILED", "Invalid ciphertext");
        }
        if (keyVersion != KEY_VERSION) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SECRET_KEY_VERSION", "Unsupported key version");
        }
        SecretKey key = requireMasterKey();
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] plain = cipher.doFinal(ciphertext);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SECRET_DECRYPT_FAILED", "Failed to decrypt secret");
        }
    }

    public String fingerprintSha256(String plaintext) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plaintext.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SECRET_FINGERPRINT_FAILED", "Failed to fingerprint secret");
        }
    }

    public String last4(String plaintext) {
        if (plaintext == null || plaintext.length() < 4) {
            return null;
        }
        String suffix = plaintext.substring(plaintext.length() - 4);
        for (int i = 0; i < suffix.length(); i++) {
            if (!Character.isLetterOrDigit(suffix.charAt(i))) {
                return null;
            }
        }
        return suffix;
    }

    public boolean isMasterKeyConfigured() {
        String encoded = properties.getMasterKey();
        if (encoded == null || encoded.isBlank()) {
            return false;
        }
        try {
            return Base64.getDecoder().decode(encoded.trim()).length == KEY_BYTES;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private SecretKey requireMasterKey() {
        String encoded = properties.getMasterKey();
        if (encoded == null || encoded.isBlank()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "SECRET_MASTER_KEY_MISSING",
                    "NOVA_SECRET_MASTER_KEY is not configured");
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(encoded.trim());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "SECRET_MASTER_KEY_INVALID",
                    "NOVA_SECRET_MASTER_KEY must be Base64-encoded 32 bytes");
        }
        if (raw.length != KEY_BYTES) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "SECRET_MASTER_KEY_INVALID",
                    "NOVA_SECRET_MASTER_KEY must be Base64-encoded 32 bytes");
        }
        return new SecretKeySpec(raw, "AES");
    }
}
