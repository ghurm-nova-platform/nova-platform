package ai.nova.platform.identity.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.dto.IdentityDtos.CreatePermissionRequest;
import ai.nova.platform.identity.dto.IdentityDtos.PermissionView;
import ai.nova.platform.identity.dto.IdentityDtos.UpdatePermissionRequest;
import ai.nova.platform.identity.entity.IdentityPermissionEntity;
import ai.nova.platform.identity.error.IdentityErrorCodes;
import ai.nova.platform.identity.mapper.IdentityEntityMapper;
import ai.nova.platform.identity.repository.IdentityPermissionRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class PermissionService {

    private final IdentityPermissionRepository permissionRepository;

    public PermissionService(IdentityPermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<PermissionView> listPermissions(UUID organizationId) {
        return permissionRepository.findByOrganizationIdOrderByCodeAsc(organizationId).stream()
                .map(IdentityEntityMapper::toPermissionView)
                .toList();
    }

    @Transactional(readOnly = true)
    public PermissionView getPermission(UUID organizationId, UUID permissionId) {
        return IdentityEntityMapper.toPermissionView(requireOrgPermission(organizationId, permissionId));
    }

    @Transactional
    public PermissionView createPermission(UUID organizationId, CreatePermissionRequest request) {
        permissionRepository
                .findByOrganizationIdAndCodeIgnoreCase(organizationId, request.code())
                .ifPresent(p -> {
                    throw new ApiException(HttpStatus.CONFLICT, "PERMISSION_EXISTS", "Permission already exists");
                });
        Instant now = Instant.now();
        IdentityPermissionEntity permission =
                new IdentityPermissionEntity(UUID.randomUUID(), organizationId, request.code(), request.name(), now);
        if (request.description() != null) {
            permission.setDescription(request.description());
        }
        return IdentityEntityMapper.toPermissionView(permissionRepository.save(permission));
    }

    @Transactional
    public PermissionView updatePermission(UUID organizationId, UUID permissionId, UpdatePermissionRequest request) {
        IdentityPermissionEntity permission = requireOrgPermission(organizationId, permissionId);
        Instant now = Instant.now();
        if (request.name() != null) {
            permission.setName(request.name());
        }
        if (request.description() != null) {
            permission.setDescription(request.description());
        }
        permission.touch(now);
        return IdentityEntityMapper.toPermissionView(permissionRepository.save(permission));
    }

    @Transactional
    public void deletePermission(UUID organizationId, UUID permissionId) {
        permissionRepository.delete(requireOrgPermission(organizationId, permissionId));
    }

    @Transactional(readOnly = true)
    public List<IdentityPermissionEntity> listPermissionEntities(UUID organizationId) {
        return permissionRepository.findByOrganizationIdOrderByCodeAsc(organizationId);
    }

    private IdentityPermissionEntity requireOrgPermission(UUID organizationId, UUID permissionId) {
        return permissionRepository
                .findById(permissionId)
                .filter(p -> p.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, IdentityErrorCodes.PERMISSION_NOT_FOUND, "Permission not found"));
    }
}
