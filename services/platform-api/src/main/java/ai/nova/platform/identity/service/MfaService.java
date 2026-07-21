package ai.nova.platform.identity.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.configuration.IdentityProperties;
import ai.nova.platform.identity.dto.IdentityDtos.MfaEnrollResponse;
import ai.nova.platform.identity.entity.IdentityMfaFactorEntity;
import ai.nova.platform.identity.entity.IdentityRecoveryCodeEntity;
import ai.nova.platform.identity.entity.IdentityUserEntity;
import ai.nova.platform.identity.entity.MfaFactorType;
import ai.nova.platform.identity.mfa.TotpUtils;
import ai.nova.platform.identity.repository.IdentityMfaFactorRepository;
import ai.nova.platform.identity.repository.IdentityRecoveryCodeRepository;
import ai.nova.platform.identity.repository.IdentityUserRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class MfaService {

    private static final int RECOVERY_CODE_COUNT = 8;

    private final IdentityProperties properties;
    private final IdentityUserRepository identityUserRepository;
    private final IdentityMfaFactorRepository mfaFactorRepository;
    private final IdentityRecoveryCodeRepository recoveryCodeRepository;
    private final PasswordPolicyService passwordPolicyService;

    public MfaService(
            IdentityProperties properties,
            IdentityUserRepository identityUserRepository,
            IdentityMfaFactorRepository mfaFactorRepository,
            IdentityRecoveryCodeRepository recoveryCodeRepository,
            PasswordPolicyService passwordPolicyService) {
        this.properties = properties;
        this.identityUserRepository = identityUserRepository;
        this.mfaFactorRepository = mfaFactorRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.passwordPolicyService = passwordPolicyService;
    }

    @Transactional
    public MfaEnrollResponse enroll(UUID identityUserId) {
        if (!properties.getMfa().isEnabled()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MFA_DISABLED", "MFA is disabled");
        }
        IdentityUserEntity user = identityUserRepository.findById(identityUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "IDENTITY_USER_NOT_FOUND", "User not found"));

        String secret = TotpUtils.generateSecret();
        Instant now = Instant.now();
        IdentityMfaFactorEntity factor = new IdentityMfaFactorEntity(
                UUID.randomUUID(), identityUserId, MfaFactorType.TOTP, secret, now);
        mfaFactorRepository.save(factor);

        List<String> recoveryCodes = passwordPolicyService.generateRecoveryCodes(RECOVERY_CODE_COUNT);
        recoveryCodeRepository.deleteAll(recoveryCodeRepository.findByIdentityUserIdAndUsedAtIsNull(identityUserId));
        for (String code : recoveryCodes) {
            recoveryCodeRepository.save(new IdentityRecoveryCodeEntity(
                    UUID.randomUUID(),
                    identityUserId,
                    passwordPolicyService.hashValue(code),
                    now));
        }

        String otpAuthUri = TotpUtils.buildOtpAuthUri(
                properties.getMfa().getIssuer(), user.getEmail(), secret);
        return new MfaEnrollResponse(secret, otpAuthUri, recoveryCodes);
    }

    @Transactional
    public void confirmEnrollment(UUID identityUserId, String totpCode) {
        IdentityMfaFactorEntity factor = mfaFactorRepository
                .findByIdentityUserId(identityUserId)
                .stream()
                .filter(f -> f.getFactorType() == MfaFactorType.TOTP && !f.isEnabled())
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MFA_FACTOR_NOT_FOUND", "No pending MFA factor"));

        if (!TotpUtils.verify(factor.getSecretEncrypted(), totpCode, 1)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MFA_VERIFICATION_FAILED", "Invalid TOTP code");
        }
        Instant now = Instant.now();
        factor.activate(now);
        mfaFactorRepository.save(factor);

        IdentityUserEntity user = identityUserRepository.findById(identityUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "IDENTITY_USER_NOT_FOUND", "User not found"));
        user.setMfaEnabled(true);
        user.touch(now);
        identityUserRepository.save(user);
    }

    @Transactional(readOnly = true)
    public boolean verify(UUID identityUserId, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        if (code.matches("\\d{6}")) {
            return mfaFactorRepository
                    .findByIdentityUserIdAndFactorTypeAndEnabledTrue(identityUserId, MfaFactorType.TOTP)
                    .map(f -> TotpUtils.verify(f.getSecretEncrypted(), code, 1))
                    .orElse(false);
        }
        return verifyRecoveryCode(identityUserId, code);
    }

    @Transactional
    public boolean verifyRecoveryCode(UUID identityUserId, String code) {
        String hash = passwordPolicyService.hashValue(code.trim());
        for (IdentityRecoveryCodeEntity recoveryCode :
                recoveryCodeRepository.findByIdentityUserIdAndUsedAtIsNull(identityUserId)) {
            if (recoveryCode.getCodeHash().equals(hash)) {
                recoveryCode.markUsed(Instant.now());
                recoveryCodeRepository.save(recoveryCode);
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public boolean isMfaRequired(UUID identityUserId) {
        return properties.getMfa().isEnabled()
                && identityUserRepository.findById(identityUserId).map(IdentityUserEntity::isMfaEnabled).orElse(false);
    }

    @Transactional
    public void disable(UUID identityUserId) {
        IdentityUserEntity user = identityUserRepository
                .findById(identityUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "IDENTITY_USER_NOT_FOUND", "User not found"));
        Instant now = Instant.now();
        user.setMfaEnabled(false);
        user.touch(now);
        identityUserRepository.save(user);
        mfaFactorRepository.findByIdentityUserId(identityUserId).forEach(factor -> {
            factor.disable(now);
            mfaFactorRepository.save(factor);
        });
    }
}
