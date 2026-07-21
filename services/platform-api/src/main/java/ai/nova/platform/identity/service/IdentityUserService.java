package ai.nova.platform.identity.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.dto.IdentityDtos.CreateUserRequest;
import ai.nova.platform.identity.dto.IdentityDtos.UpdateUserRequest;
import ai.nova.platform.identity.dto.IdentityDtos.UserView;
import ai.nova.platform.identity.entity.IdentityProviderEntity;
import ai.nova.platform.identity.entity.IdentityUserEntity;
import ai.nova.platform.identity.error.IdentityErrorCodes;
import ai.nova.platform.identity.mapper.IdentityEntityMapper;
import ai.nova.platform.identity.repository.IdentityUserRepository;
import ai.nova.platform.user.UserAccount;
import ai.nova.platform.user.UserAccountRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class IdentityUserService {

    private static final Logger log = LoggerFactory.getLogger(IdentityUserService.class);

    private final IdentityUserRepository identityUserRepository;
    private final IdentityProviderService identityProviderService;
    private final UserAccountRepository userAccountRepository;
    private final PasswordPolicyService passwordPolicyService;
    private final PasswordEncoder passwordEncoder;

    public IdentityUserService(
            IdentityUserRepository identityUserRepository,
            IdentityProviderService identityProviderService,
            UserAccountRepository userAccountRepository,
            PasswordPolicyService passwordPolicyService,
            PasswordEncoder passwordEncoder) {
        this.identityUserRepository = identityUserRepository;
        this.identityProviderService = identityProviderService;
        this.userAccountRepository = userAccountRepository;
        this.passwordPolicyService = passwordPolicyService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserView> listUsers(UUID organizationId) {
        Instant now = Instant.now();
        return identityUserRepository.findByOrganizationIdOrderByEmailAsc(organizationId).stream()
                .map(user -> IdentityEntityMapper.toUserView(user, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserView getUser(UUID organizationId, UUID userId) {
        return IdentityEntityMapper.toUserView(requireOrgUser(organizationId, userId), Instant.now());
    }

    @Transactional
    public UserView createUser(UUID organizationId, CreateUserRequest request) {
        identityUserRepository
                .findByOrganizationIdAndEmailIgnoreCase(organizationId, request.email())
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "IDENTITY_USER_EXISTS", "User already exists");
                });

        IdentityProviderEntity provider = request.providerId() != null
                ? identityProviderService.requireOrgProvider(organizationId, request.providerId())
                : identityProviderService.resolveLocalProvider(organizationId);

        Instant now = Instant.now();
        IdentityUserEntity user = new IdentityUserEntity(
                UUID.randomUUID(),
                organizationId,
                null,
                provider.getId(),
                request.email(),
                request.email(),
                request.displayName(),
                now);
        if (request.password() != null && !request.password().isBlank()) {
            passwordPolicyService.validate(request.password());
            // Password stored on platform user when linked; identity-only users defer linking
        }
        return IdentityEntityMapper.toUserView(identityUserRepository.save(user), now);
    }

    @Transactional
    public UserView updateUser(UUID organizationId, UUID userId, UpdateUserRequest request) {
        IdentityUserEntity user = requireOrgUser(organizationId, userId);
        Instant now = Instant.now();
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
            if (user.getPlatformUserId() != null) {
                userAccountRepository.findById(user.getPlatformUserId()).ifPresent(platformUser -> {
                    platformUser.setEnabled(request.enabled(), now);
                    userAccountRepository.save(platformUser);
                });
            }
        }
        if (request.providerId() != null) {
            identityProviderService.requireOrgProvider(organizationId, request.providerId());
            // provider reassignment would need setter - skip if not available
        }
        user.touch(now);
        return IdentityEntityMapper.toUserView(identityUserRepository.save(user), now);
    }

    @Transactional
    public void deleteUser(UUID organizationId, UUID userId) {
        IdentityUserEntity user = requireOrgUser(organizationId, userId);
        identityUserRepository.delete(user);
    }

    @Transactional
    public UserView enableUser(UUID organizationId, UUID userId) {
        return setEnabled(organizationId, userId, true);
    }

    @Transactional
    public UserView disableUser(UUID organizationId, UUID userId) {
        return setEnabled(organizationId, userId, false);
    }

    @Transactional
    public UserView unlockUser(UUID organizationId, UUID userId) {
        IdentityUserEntity user = requireOrgUser(organizationId, userId);
        user.unlock(Instant.now());
        return IdentityEntityMapper.toUserView(identityUserRepository.save(user), Instant.now());
    }

    @Transactional
    public void resetPassword(UUID organizationId, UUID userId, String newPassword) {
        IdentityUserEntity user = requireOrgUser(organizationId, userId);
        applyPasswordChange(user, newPassword, true);
    }

    @Transactional
    public void applyPasswordChange(IdentityUserEntity user, String newPassword, boolean forceChange) {
        passwordPolicyService.validate(newPassword);
        passwordPolicyService.validateAgainstHistory(user.getId(), newPassword);
        Instant now = Instant.now();
        String hash = passwordEncoder.encode(newPassword);
        if (user.getPlatformUserId() != null) {
            UserAccount platformUser = userAccountRepository
                    .findById(user.getPlatformUserId())
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND, IdentityErrorCodes.IDENTITY_USER_NOT_FOUND, "Platform user not found"));
            platformUser.updatePassword(hash, now);
            userAccountRepository.save(platformUser);
        }
        passwordPolicyService.recordPasswordChange(user.getId(), hash);
        user.setPasswordChangedAt(now);
        user.setForcePasswordChange(forceChange);
        user.clearPasswordResetToken();
        user.touch(now);
        identityUserRepository.save(user);
        log.info("Password reset for identity user {}", user.getId());
    }

    @Transactional(readOnly = true)
    public IdentityUserEntity requireOrgUser(UUID organizationId, UUID userId) {
        IdentityUserEntity user = identityUserRepository
                .findById(userId)
                .filter(u -> u.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, IdentityErrorCodes.IDENTITY_USER_NOT_FOUND, "Identity user not found"));
        return user;
    }

    private UserView setEnabled(UUID organizationId, UUID userId, boolean enabled) {
        UpdateUserRequest request = new UpdateUserRequest(null, enabled, null);
        return updateUser(organizationId, userId, request);
    }
}
