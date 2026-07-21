package ai.nova.platform.identity.mfa;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class TotpUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SECRET_BYTES = 20;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;

    private TotpUtils() {
    }

    public static String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String buildOtpAuthUri(String issuer, String accountName, String secret) {
        return "otpauth://totp/" + urlEncode(issuer) + ":" + urlEncode(accountName)
                + "?secret=" + secret + "&issuer=" + urlEncode(issuer) + "&digits=" + CODE_DIGITS;
    }

    public static boolean verify(String secretBase64, String code, int window) {
        if (code == null || !code.matches("\\d{6}")) {
            return false;
        }
        byte[] key = Base64.getDecoder().decode(secretBase64);
        long counter = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        for (int i = -window; i <= window; i++) {
            if (generateCode(key, counter + i).equals(code)) {
                return true;
            }
        }
        return false;
    }

    private static String generateCode(byte[] key, long counter) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("TOTP generation failed", ex);
        }
    }

    private static String urlEncode(String value) {
        return value.replace(" ", "%20");
    }
}
