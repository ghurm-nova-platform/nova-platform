package ai.nova.platform.identity.service;

import org.springframework.stereotype.Service;

import ai.nova.platform.identity.repository.IdentityServiceAccountRepository;

@Service
public class ServiceAccountService {

    private final IdentityServiceAccountRepository serviceAccountRepository;

    public ServiceAccountService(IdentityServiceAccountRepository serviceAccountRepository) {
        this.serviceAccountRepository = serviceAccountRepository;
    }
}
