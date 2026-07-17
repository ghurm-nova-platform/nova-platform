package ai.nova.platform.project;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ai.nova.platform.organization.OrganizationRepository;
import ai.nova.platform.project.ProjectDtos.ProjectRequest;
import ai.nova.platform.project.ProjectDtos.ProjectResponse;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ConflictException;
import ai.nova.platform.web.error.ForbiddenException;
import ai.nova.platform.web.error.ResourceNotFoundException;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;

    public ProjectService(ProjectRepository projectRepository, OrganizationRepository organizationRepository) {
        this.projectRepository = projectRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> list(AuthenticatedUser user, String search, Pageable pageable) {
        return projectRepository
                .searchByOrganization(user.getOrganizationId(), normalizeSearch(search), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID id, AuthenticatedUser user) {
        return toResponse(requireInOrganization(id, user));
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request, AuthenticatedUser user) {
        requireProjectAdmin(user);
        ensureOrganizationExists(user.getOrganizationId());

        String name = request.name().trim();
        if (projectRepository.existsByOrganizationIdAndNameIgnoreCase(user.getOrganizationId(), name)) {
            throw new ConflictException("Project name already exists in this organization");
        }

        Instant now = Instant.now();
        Project project = new Project(
                UUID.randomUUID(),
                user.getOrganizationId(),
                name,
                trimToNull(request.description()),
                request.status(),
                request.visibility(),
                now,
                now,
                user.getUserId(),
                user.getUserId());
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse update(UUID id, ProjectRequest request, AuthenticatedUser user) {
        requireProjectAdmin(user);
        Project project = requireInOrganization(id, user);

        String name = request.name().trim();
        if (projectRepository.existsByOrganizationIdAndNameIgnoreCaseAndIdNot(
                user.getOrganizationId(), name, id)) {
            throw new ConflictException("Project name already exists in this organization");
        }

        project.setName(name);
        project.setDescription(trimToNull(request.description()));
        project.setStatus(request.status());
        project.setVisibility(request.visibility());
        project.setUpdatedAt(Instant.now());
        project.setUpdatedBy(user.getUserId());
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public void delete(UUID id, AuthenticatedUser user) {
        requireProjectAdmin(user);
        Project project = requireInOrganization(id, user);
        project.setStatus(ProjectStatus.ARCHIVED);
        project.setUpdatedAt(Instant.now());
        project.setUpdatedBy(user.getUserId());
        projectRepository.save(project);
    }

    private Project requireInOrganization(UUID id, AuthenticatedUser user) {
        return projectRepository.findByIdAndOrganizationId(id, user.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private void ensureOrganizationExists(UUID organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found");
        }
    }

    private void requireProjectAdmin(AuthenticatedUser user) {
        if (!user.getRoles().contains("PROJECT_ADMIN") && !user.getRoles().contains("ORG_ADMIN")) {
            throw new ForbiddenException("PROJECT_ADMIN role required");
        }
    }

    private String normalizeSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        return search.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getOrganizationId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getVisibility(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                project.getCreatedBy(),
                project.getUpdatedBy());
    }
}
