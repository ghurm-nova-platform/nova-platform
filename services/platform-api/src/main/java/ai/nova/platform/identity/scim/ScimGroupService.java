package ai.nova.platform.identity.scim;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.entity.IdentityGroupEntity;
import ai.nova.platform.identity.repository.IdentityGroupRepository;

@Service
public class ScimGroupService {

    private final IdentityGroupRepository groupRepository;

    public ScimGroupService(IdentityGroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    @Transactional
    public IdentityGroupEntity createGroup(UUID organizationId, String name) {
        Instant now = Instant.now();
        IdentityGroupEntity group = new IdentityGroupEntity(UUID.randomUUID(), organizationId, name, name, now);
        return groupRepository.save(group);
    }

    @Transactional(readOnly = true)
    public List<IdentityGroupEntity> listGroups(UUID organizationId) {
        return groupRepository.findByOrganizationIdOrderByNameAsc(organizationId);
    }
}
