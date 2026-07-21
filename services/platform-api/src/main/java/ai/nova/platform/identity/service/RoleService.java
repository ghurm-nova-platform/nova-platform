package ai.nova.platform.identity.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.dto.IdentityDtos.CreateRoleRequest;
import ai.nova.platform.identity.dto.IdentityDtos.RoleView;
import ai.nova.platform.identity.dto.IdentityDtos.UpdateRoleRequest;
import ai.nova.platform.identity.entity.IdentityRoleEntity;
import ai.nova.platform.identity.error.IdentityErrorCodes;
import ai.nova.platform.identity.mapper.IdentityEntityMapper;
import ai.nova.platform.identity.repository.IdentityRoleRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class RoleService {

    private final IdentityRoleRepository roleRepository;

    public RoleService(IdentityRoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleView> listRoles(UUID organizationId) {
        return roleRepository.findByOrganizationIdOrderByCodeAsc(organizationId).stream()
                .map(IdentityEntityMapper::toRoleView)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleView getRole(UUID organizationId, UUID roleId) {
        return IdentityEntityMapper.toRoleView(requireOrgRole(organizationId, roleId));
    }

    @Transactional
    public RoleView createRole(UUID organizationId, CreateRoleRequest request) {
        roleRepository
                .findByOrganizationIdAndCodeIgnoreCase(organizationId, request.code())
                .ifPresent(r -> {
                    throw new ApiException(HttpStatus.CONFLICT, "ROLE_EXISTS", "Role already exists");
                });
        Instant now = Instant.now();
        IdentityRoleEntity role = new IdentityRoleEntity(
                UUID.randomUUID(), organizationId, request.code(), request.name(), now);
        if (request.description() != null) {
            role.setDescription(request.description());
        }
        return IdentityEntityMapper.toRoleView(roleRepository.save(role));
    }

    @Transactional
    public RoleView updateRole(UUID organizationId, UUID roleId, UpdateRoleRequest request) {
        IdentityRoleEntity role = requireOrgRole(organizationId, roleId);
        Instant now = Instant.now();
        if (request.name() != null) {
            role.setName(request.name());
        }
        if (request.description() != null) {
            role.setDescription(request.description());
        }
        role.touch(now);
        return IdentityEntityMapper.toRoleView(roleRepository.save(role));
    }

    @Transactional
    public void deleteRole(UUID organizationId, UUID roleId) {
        roleRepository.delete(requireOrgRole(organizationId, roleId));
    }

    @Transactional(readOnly = true)
    public List<IdentityRoleEntity> listRoleEntities(UUID organizationId) {
        return roleRepository.findByOrganizationIdOrderByCodeAsc(organizationId);
    }

    private IdentityRoleEntity requireOrgRole(UUID organizationId, UUID roleId) {
        return roleRepository
                .findById(roleId)
                .filter(r -> r.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, IdentityErrorCodes.ROLE_NOT_FOUND, "Role not found"));
    }
}
