package ai.nova.platform.identity.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.dto.IdentityDtos.CreateServiceAccountRequest;
import ai.nova.platform.identity.dto.IdentityDtos.ServiceAccountCreateResponse;
import ai.nova.platform.identity.dto.IdentityDtos.ServiceAccountView;
import ai.nova.platform.identity.dto.IdentityDtos.UpdateServiceAccountRequest;
import ai.nova.platform.identity.entity.IdentityServiceAccountEntity;
import ai.nova.platform.identity.error.IdentityErrorCodes;
import ai.nova.platform.identity.mapper.IdentityEntityMapper;
import ai.nova.platform.identity.repository.IdentityServiceAccountRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class ServiceAccountService {

    private final IdentityServiceAccountRepository serviceAccountRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    public ServiceAccountService(
            IdentityServiceAccountRepository serviceAccountRepository,
            RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder) {
        this.serviceAccountRepository = serviceAccountRepository;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<ServiceAccountView> listServiceAccounts(UUID organizationId) {
        return serviceAccountRepository.findByOrganizationIdOrderByNameAsc(organizationId).stream()
                .map(IdentityEntityMapper::toServiceAccountView)
                .toList();
    }

    @Transactional
    public ServiceAccountCreateResponse createServiceAccount(UUID organizationId, CreateServiceAccountRequest request) {
        Instant now = Instant.now();
        String clientId = "sa_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String clientSecret = refreshTokenService.generateRefreshToken();
        IdentityServiceAccountEntity entity = new IdentityServiceAccountEntity(
                UUID.randomUUID(),
                organizationId,
                request.name(),
                clientId,
                passwordEncoder.encode(clientSecret),
                now);
        serviceAccountRepository.save(entity);
        return new ServiceAccountCreateResponse(entity.getId(), entity.getName(), clientId, clientSecret);
    }

    @Transactional
    public ServiceAccountView updateServiceAccount(
            UUID organizationId, UUID accountId, UpdateServiceAccountRequest request) {
        IdentityServiceAccountEntity account = requireOrgAccount(organizationId, accountId);
        Instant now = Instant.now();
        if (request.name() != null) {
            account.setName(request.name());
        }
        if (request.enabled() != null) {
            account.setEnabled(request.enabled());
        }
        account.touch(now);
        return IdentityEntityMapper.toServiceAccountView(serviceAccountRepository.save(account));
    }

    @Transactional
    public void deleteServiceAccount(UUID organizationId, UUID accountId) {
        serviceAccountRepository.delete(requireOrgAccount(organizationId, accountId));
    }

    private IdentityServiceAccountEntity requireOrgAccount(UUID organizationId, UUID accountId) {
        return serviceAccountRepository
                .findById(accountId)
                .filter(a -> a.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        IdentityErrorCodes.SERVICE_ACCOUNT_NOT_FOUND,
                        "Service account not found"));
    }
}
