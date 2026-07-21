package ai.nova.platform.identity.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.dto.IdentityDtos.ApiTokenCreateResponse;
import ai.nova.platform.identity.dto.IdentityDtos.ApiTokenView;
import ai.nova.platform.identity.entity.IdentityApiTokenEntity;
import ai.nova.platform.identity.error.IdentityErrorCodes;
import ai.nova.platform.identity.mapper.IdentityEntityMapper;
import ai.nova.platform.identity.repository.IdentityApiTokenRepository;
import ai.nova.platform.identity.repository.IdentityUserRepository;
import ai.nova.platform.permission.Permission;
import ai.nova.platform.role.Role;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.user.UserAccount;
import ai.nova.platform.user.UserAccountRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class ApiTokenService {

    public static final String TOKEN_PREFIX = "nova_pat_";

    private final IdentityApiTokenRepository apiTokenRepository;
    private final IdentityUserRepository identityUserRepository;
    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenService refreshTokenService;

    public ApiTokenService(
            IdentityApiTokenRepository apiTokenRepository,
            IdentityUserRepository identityUserRepository,
            UserAccountRepository userAccountRepository,
            RefreshTokenService refreshTokenService) {
        this.apiTokenRepository = apiTokenRepository;
        this.identityUserRepository = identityUserRepository;
        this.userAccountRepository = userAccountRepository;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional(readOnly = true)
    public List<ApiTokenView> listTokens(UUID organizationId) {
        return apiTokenRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(IdentityEntityMapper::toApiTokenView)
                .toList();
    }

    @Transactional
    public ApiTokenCreateResponse createToken(UUID organizationId, UUID identityUserId, String name) {
        String rawToken = TOKEN_PREFIX + refreshTokenService.generateRefreshToken();
        String hash = refreshTokenService.hashToken(rawToken);
        IdentityApiTokenEntity entity = new IdentityApiTokenEntity(
                UUID.randomUUID(),
                organizationId,
                identityUserId,
                name,
                rawToken.substring(0, Math.min(16, rawToken.length())),
                hash,
                "[]",
                null,
                Instant.now());
        apiTokenRepository.save(entity);
        return new ApiTokenCreateResponse(entity.getId(), rawToken, entity.getTokenPrefix());
    }

    @Transactional
    public void deleteToken(UUID organizationId, UUID tokenId) {
        apiTokenRepository.delete(requireOrgToken(organizationId, tokenId));
    }

    @Transactional
    public void revokeToken(UUID organizationId, UUID tokenId) {
        IdentityApiTokenEntity token = requireOrgToken(organizationId, tokenId);
        token.revoke(Instant.now());
        apiTokenRepository.save(token);
    }

    @Transactional
    public AuthenticatedUser authenticateApiToken(String rawToken) {
        if (rawToken == null || !rawToken.startsWith(TOKEN_PREFIX)) {
            return null;
        }
        IdentityApiTokenEntity token = apiTokenRepository.findByTokenHash(refreshTokenService.hashToken(rawToken))
                .filter(t -> t.isActive(Instant.now()))
                .orElse(null);
        if (token == null || token.getIdentityUserId() == null) {
            return null;
        }
        token.markUsed(Instant.now());
        apiTokenRepository.save(token);
        return identityUserRepository.findById(token.getIdentityUserId())
                .flatMap(identityUser -> userAccountRepository.findById(identityUser.getPlatformUserId()))
                .map(this::toPrincipal)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public IdentityApiTokenEntity requireToken(UUID tokenId) {
        return apiTokenRepository
                .findById(tokenId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, IdentityErrorCodes.API_TOKEN_NOT_FOUND, "API token not found"));
    }

    private IdentityApiTokenEntity requireOrgToken(UUID organizationId, UUID tokenId) {
        return apiTokenRepository
                .findById(tokenId)
                .filter(t -> t.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, IdentityErrorCodes.API_TOKEN_NOT_FOUND, "API token not found"));
    }

    private AuthenticatedUser toPrincipal(UserAccount user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getOrganization().getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRoles().stream().map(Role::getCode).sorted().toList(),
                user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(Permission::getCode)
                        .distinct()
                        .sorted()
                        .toList(),
                user.isEnabled());
    }
}
