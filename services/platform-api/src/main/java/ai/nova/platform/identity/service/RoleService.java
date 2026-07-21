package ai.nova.platform.identity.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.entity.IdentityRoleEntity;
import ai.nova.platform.identity.repository.IdentityRoleRepository;

@Service
public class RoleService {

    private final IdentityRoleRepository roleRepository;

    public RoleService(IdentityRoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<IdentityRoleEntity> listRoles(UUID organizationId) {
        return roleRepository.findByOrganizationIdOrderByCodeAsc(organizationId);
    }
}
