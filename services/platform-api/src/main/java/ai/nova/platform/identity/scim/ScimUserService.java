package ai.nova.platform.identity.scim;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.dto.IdentityDtos.ScimUserRequest;
import ai.nova.platform.identity.dto.IdentityDtos.ScimUserResponse;
import ai.nova.platform.identity.entity.IdentityProviderEntity;
import ai.nova.platform.identity.entity.IdentityUserEntity;
import ai.nova.platform.identity.repository.IdentityUserRepository;
import ai.nova.platform.identity.service.IdentityProviderService;

@Service
public class ScimUserService {

    private final IdentityUserRepository identityUserRepository;
    private final IdentityProviderService identityProviderService;

    public ScimUserService(
            IdentityUserRepository identityUserRepository, IdentityProviderService identityProviderService) {
        this.identityUserRepository = identityUserRepository;
        this.identityProviderService = identityProviderService;
    }

    @Transactional
    public ScimUserResponse createUser(UUID organizationId, ScimUserRequest request) {
        IdentityProviderEntity provider = identityProviderService.resolveLocalProvider(organizationId);
        Instant now = Instant.now();
        IdentityUserEntity user = new IdentityUserEntity(
                UUID.randomUUID(),
                organizationId,
                null,
                provider.getId(),
                request.userName(),
                request.userName(),
                request.displayName() != null ? request.displayName() : request.userName(),
                now);
        if (!request.active()) {
            // enabled defaults true; SCIM inactive users remain stored but disabled via future flag
        }
        identityUserRepository.save(user);
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<ScimUserResponse> listUsers(UUID organizationId) {
        return identityUserRepository.findByOrganizationIdOrderByEmailAsc(organizationId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ScimUserResponse toResponse(IdentityUserEntity user) {
        return new ScimUserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.isEnabled());
    }
}
