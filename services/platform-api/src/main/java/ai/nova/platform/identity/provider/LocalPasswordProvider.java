package ai.nova.platform.identity.provider;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import ai.nova.platform.identity.entity.IdentityUserEntity;
import ai.nova.platform.identity.entity.ProviderType;
import ai.nova.platform.identity.repository.IdentityUserRepository;
import ai.nova.platform.user.UserAccount;
import ai.nova.platform.user.UserAccountRepository;

@Component
public class LocalPasswordProvider implements IdentityProviderConnector {

    private final UserAccountRepository userAccountRepository;
    private final IdentityUserRepository identityUserRepository;
    private final PasswordEncoder passwordEncoder;

    public LocalPasswordProvider(
            UserAccountRepository userAccountRepository,
            IdentityUserRepository identityUserRepository,
            PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.identityUserRepository = identityUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean supports(ProviderType type) {
        return type == ProviderType.LOCAL;
    }

    @Override
    public AuthenticationResult authenticate(AuthenticationCredentials credentials) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(credentials.email().trim())
                .orElse(null);
        if (user == null || !user.isEnabled()) {
            return AuthenticationResult.failure("Invalid email or password");
        }
        if (!passwordEncoder.matches(credentials.password(), user.getPasswordHash())) {
            return AuthenticationResult.failure("Invalid email or password");
        }
        UUID identityUserId = identityUserRepository.findByPlatformUserId(user.getId())
                .map(IdentityUserEntity::getId)
                .orElseGet(() -> identityUserRepository.findByOrganizationIdAndEmailIgnoreCase(
                                user.getOrganization().getId(), user.getEmail())
                        .map(IdentityUserEntity::getId)
                        .orElse(null));
        return AuthenticationResult.success(user, identityUserId);
    }
}
