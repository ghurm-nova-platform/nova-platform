package ai.nova.platform.identity.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.repository.IdentityGroupRepository;

@Service
public class GroupService {

    private final IdentityGroupRepository groupRepository;

    public GroupService(IdentityGroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    @Transactional(readOnly = true)
    public long countGroups(UUID organizationId) {
        return groupRepository.findByOrganizationIdOrderByNameAsc(organizationId).size();
    }
}
