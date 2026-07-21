package ai.nova.platform.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.configuration.IdentityProperties;
import ai.nova.platform.identity.entity.IdentityPasswordHistoryEntity;
import ai.nova.platform.identity.entity.IdentityUserEntity;
import ai.nova.platform.identity.repository.IdentityPasswordHistoryRepository;
import ai.nova.platform.identity.repository.IdentityUserRepository;
import ai.nova.platform.user.UserAccount;
import ai.nova.platform.user.UserAccountRepository;
import ai.nova.platform.web.error.ApiException;
import org.springframework.http.HttpStatus;

@Service
public class PasswordPolicyService {

    private final IdentityProperties properties;
    private final IdentityPasswordHistoryRepository passwordHistoryRepository;
    private final IdentityUserRepository identityUserRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordPolicyService(
            IdentityProperties properties,
            IdentityPasswordHistoryRepository passwordHistoryRepository,
            IdentityUserRepository identityUserRepository,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.identityUserRepository = identityUserRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void validate(String password) {
        IdentityProperties.Password policy = properties.getPassword();
        if (password == null || password.length() < policy.getMinLength()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_POLICY_VIOLATION",
                    "Password must be at least " + policy.getMinLength() + " characters");
        }
        if (policy.isRequireUppercase() && password.chars().noneMatch(Character::isUpperCase)) {
            throw policyViolation("Password must contain an uppercase letter");
        }
        if (policy.isRequireLowercase() && password.chars().noneMatch(Character::isLowerCase)) {
            throw policyViolation("Password must contain a lowercase letter");
        }
        if (policy.isRequireDigit() && password.chars().noneMatch(Character::isDigit)) {
            throw policyViolation("Password must contain a digit");
        }
        if (policy.isRequireSpecial() && password.chars().allMatch(Character::isLetterOrDigit)) {
            throw policyViolation("Password must contain a special character");
        }
    }

    @Transactional(readOnly = true)
    public void validateAgainstHistory(UUID identityUserId, String newPassword) {
        int historyCount = properties.getPassword().getHistoryCount();
        if (historyCount <= 0) {
            return;
        }
        List<IdentityPasswordHistoryEntity> history =
                passwordHistoryRepository.findTop10ByIdentityUserIdOrderByCreatedAtDesc(identityUserId);
        for (IdentityPasswordHistoryEntity entry : history.stream().limit(historyCount).toList()) {
            if (passwordEncoder.matches(newPassword, entry.getPasswordHash())) {
                throw policyViolation("Password was used recently and cannot be reused");
            }
        }
        IdentityUserEntity identityUser = identityUserRepository.findById(identityUserId).orElse(null);
        if (identityUser != null && identityUser.getPlatformUserId() != null) {
            UserAccount platformUser = userAccountRepository.findById(identityUser.getPlatformUserId()).orElse(null);
            if (platformUser != null && passwordEncoder.matches(newPassword, platformUser.getPasswordHash())) {
                throw policyViolation("Password matches current password");
            }
        }
    }

    @Transactional
    public void recordPasswordChange(UUID identityUserId, String passwordHash) {
        passwordHistoryRepository.save(new IdentityPasswordHistoryEntity(
                UUID.randomUUID(), identityUserId, passwordHash, java.time.Instant.now()));
    }

    public List<String> generateRecoveryCodes(int count) {
        SecureRandom random = new SecureRandom();
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> {
                    byte[] bytes = new byte[5];
                    random.nextBytes(bytes);
                    return HexFormat.of().formatHex(bytes);
                })
                .collect(Collectors.toList());
    }

    public String hashValue(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private ApiException policyViolation(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_POLICY_VIOLATION", message);
    }
}
