package ai.nova.platform.identity.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.entity.IdentityPermissionEntity;
import ai.nova.platform.identity.repository.IdentityPermissionRepository;

@Service
public class PermissionService {

    private final IdentityPermissionRepository permissionRepository;

    public PermissionService(IdentityPermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<IdentityPermissionEntity> listPermissions(UUID organizationId) {
        return permissionRepository.findByOrganizationIdOrderByCodeAsc(organizationId);
    }
}
