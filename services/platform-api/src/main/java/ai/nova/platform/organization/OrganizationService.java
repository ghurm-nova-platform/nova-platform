package ai.nova.platform.organization;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ai.nova.platform.organization.OrganizationDtos.OrganizationRequest;
import ai.nova.platform.organization.OrganizationDtos.OrganizationResponse;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ConflictException;
import ai.nova.platform.web.error.ForbiddenException;
import ai.nova.platform.web.error.ResourceNotFoundException;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Transactional(readOnly = true)
    public Page<OrganizationResponse> list(AuthenticatedUser user, String search, Pageable pageable) {
        String normalized = normalizeSearch(search);
        Page<Organization> page = user.getRoles().contains("ORG_ADMIN")
                ? organizationRepository.search(normalized, pageable)
                : organizationRepository.searchForMember(user.getOrganizationId(), normalized, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse get(UUID id, AuthenticatedUser user) {
        Organization organization = requireAccessible(id, user);
        return toResponse(organization);
    }

    @Transactional
    public OrganizationResponse create(OrganizationRequest request, AuthenticatedUser user) {
        requireOrgAdmin(user);
        String name = request.name().trim();
        String slug = resolveSlug(request.slug(), name);

        if (organizationRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Organization name already exists");
        }
        if (organizationRepository.existsBySlugIgnoreCase(slug)) {
            throw new ConflictException("Organization slug already exists");
        }

        Instant now = Instant.now();
        Organization organization = new Organization(
                UUID.randomUUID(),
                name,
                slug,
                now,
                now,
                user.getUserId(),
                user.getUserId());
        return toResponse(organizationRepository.save(organization));
    }

    @Transactional
    public OrganizationResponse update(UUID id, OrganizationRequest request, AuthenticatedUser user) {
        requireOrgAdmin(user);
        Organization organization = requireAccessible(id, user);

        String name = request.name().trim();
        String slug = resolveSlug(request.slug(), name);

        if (organizationRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Organization name already exists");
        }
        if (organizationRepository.existsBySlugIgnoreCaseAndIdNot(slug, id)) {
            throw new ConflictException("Organization slug already exists");
        }

        organization.setName(name);
        organization.setSlug(slug);
        organization.setUpdatedAt(Instant.now());
        organization.setUpdatedBy(user.getUserId());
        return toResponse(organizationRepository.save(organization));
    }

    @Transactional
    public void delete(UUID id, AuthenticatedUser user) {
        requireOrgAdmin(user);
        Organization organization = requireAccessible(id, user);
        organizationRepository.delete(organization);
    }

    private Organization requireAccessible(UUID id, AuthenticatedUser user) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        if (!user.getRoles().contains("ORG_ADMIN") && !organization.getId().equals(user.getOrganizationId())) {
            throw new ForbiddenException("Not allowed to access this organization");
        }
        return organization;
    }

    private void requireOrgAdmin(AuthenticatedUser user) {
        if (!user.getRoles().contains("ORG_ADMIN")) {
            throw new ForbiddenException("ORG_ADMIN role required");
        }
    }

    private String resolveSlug(String requestedSlug, String name) {
        String base = StringUtils.hasText(requestedSlug) ? requestedSlug : name;
        String slug = base.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (!StringUtils.hasText(slug)) {
            throw new IllegalArgumentException("Organization slug cannot be empty");
        }
        if (slug.length() > 100) {
            slug = slug.substring(0, 100);
        }
        return slug;
    }

    private String normalizeSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        return search.trim();
    }

    private OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getCreatedAt(),
                organization.getUpdatedAt(),
                organization.getCreatedBy(),
                organization.getUpdatedBy());
    }
}
