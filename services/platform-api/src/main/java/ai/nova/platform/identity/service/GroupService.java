package ai.nova.platform.identity.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.dto.IdentityDtos.CreateGroupRequest;
import ai.nova.platform.identity.dto.IdentityDtos.GroupView;
import ai.nova.platform.identity.dto.IdentityDtos.UpdateGroupRequest;
import ai.nova.platform.identity.entity.IdentityGroupEntity;
import ai.nova.platform.identity.error.IdentityErrorCodes;
import ai.nova.platform.identity.mapper.IdentityEntityMapper;
import ai.nova.platform.identity.repository.IdentityGroupRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class GroupService {

    private final IdentityGroupRepository groupRepository;
    private final UserSynchronizationService userSynchronizationService;

    public GroupService(
            IdentityGroupRepository groupRepository, UserSynchronizationService userSynchronizationService) {
        this.groupRepository = groupRepository;
        this.userSynchronizationService = userSynchronizationService;
    }

    @Transactional(readOnly = true)
    public List<GroupView> listGroups(UUID organizationId) {
        return groupRepository.findByOrganizationIdOrderByNameAsc(organizationId).stream()
                .map(IdentityEntityMapper::toGroupView)
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupView getGroup(UUID organizationId, UUID groupId) {
        return IdentityEntityMapper.toGroupView(requireOrgGroup(organizationId, groupId));
    }

    @Transactional
    public GroupView createGroup(UUID organizationId, CreateGroupRequest request) {
        groupRepository
                .findByOrganizationIdAndNameIgnoreCase(organizationId, request.name())
                .ifPresent(g -> {
                    throw new ApiException(HttpStatus.CONFLICT, "GROUP_EXISTS", "Group already exists");
                });
        Instant now = Instant.now();
        IdentityGroupEntity group = new IdentityGroupEntity(
                UUID.randomUUID(), organizationId, request.name(), request.externalId(), now);
        if (request.description() != null) {
            group.setDescription(request.description());
        }
        return IdentityEntityMapper.toGroupView(groupRepository.save(group));
    }

    @Transactional
    public GroupView updateGroup(UUID organizationId, UUID groupId, UpdateGroupRequest request) {
        IdentityGroupEntity group = requireOrgGroup(organizationId, groupId);
        Instant now = Instant.now();
        if (request.name() != null) {
            group.setName(request.name());
        }
        if (request.externalId() != null) {
            group.setExternalId(request.externalId());
        }
        if (request.description() != null) {
            group.setDescription(request.description());
        }
        group.touch(now);
        return IdentityEntityMapper.toGroupView(groupRepository.save(group));
    }

    @Transactional
    public void deleteGroup(UUID organizationId, UUID groupId) {
        groupRepository.delete(requireOrgGroup(organizationId, groupId));
    }

    @Transactional
    public void syncGroup(UUID organizationId, UUID groupId) {
        requireOrgGroup(organizationId, groupId);
        userSynchronizationService.synchronizeGroup(organizationId, groupId);
    }

    @Transactional(readOnly = true)
    public long countGroups(UUID organizationId) {
        return groupRepository.findByOrganizationIdOrderByNameAsc(organizationId).size();
    }

    private IdentityGroupEntity requireOrgGroup(UUID organizationId, UUID groupId) {
        return groupRepository
                .findById(groupId)
                .filter(g -> g.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, IdentityErrorCodes.GROUP_NOT_FOUND, "Group not found"));
    }
}
