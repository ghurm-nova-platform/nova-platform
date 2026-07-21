package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.dto.IdentityDtos.MfaEnrollResponse;
import ai.nova.platform.identity.mfa.TotpUtils;
import ai.nova.platform.identity.service.MfaService;
import ai.nova.platform.identity.support.IdentityTestFixture;

@SpringBootTest
class MfaServiceTest {

    @Autowired
    private MfaService mfaService;

    @Test
    @Transactional
    void enrollVerifyAndRecoveryCode() {
        MfaEnrollResponse enrollment = mfaService.enroll(IdentityTestFixture.IDENTITY_USER_ID);
        assertThat(enrollment.secret()).isNotBlank();
        assertThat(enrollment.otpAuthUri()).contains("otpauth://totp/");
        assertThat(enrollment.recoveryCodes()).hasSize(8);

        String validCode = generateCurrentTotp(enrollment.secret());
        mfaService.confirmEnrollment(IdentityTestFixture.IDENTITY_USER_ID, validCode);
        assertThat(mfaService.verify(IdentityTestFixture.IDENTITY_USER_ID, validCode)).isTrue();
        assertThat(mfaService.verify(IdentityTestFixture.IDENTITY_USER_ID, enrollment.recoveryCodes().get(0)))
                .isTrue();
    }

    private String generateCurrentTotp(String secret) {
        for (int offset = -1; offset <= 1; offset++) {
            long counter = (java.time.Instant.now().getEpochSecond() / 30) + offset;
            try {
                javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
                mac.init(new javax.crypto.spec.SecretKeySpec(java.util.Base64.getDecoder().decode(secret), "HmacSHA1"));
                byte[] hash = mac.doFinal(java.nio.ByteBuffer.allocate(8).putLong(counter).array());
                int otpOffset = hash[hash.length - 1] & 0x0F;
                int binary = ((hash[otpOffset] & 0x7F) << 24)
                        | ((hash[otpOffset + 1] & 0xFF) << 16)
                        | ((hash[otpOffset + 2] & 0xFF) << 8)
                        | (hash[otpOffset + 3] & 0xFF);
                int otp = binary % 1_000_000;
                String candidate = String.format("%06d", otp);
                if (TotpUtils.verify(secret, candidate, 0)) {
                    return candidate;
                }
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
        throw new IllegalStateException("Unable to generate TOTP for test");
    }
}
