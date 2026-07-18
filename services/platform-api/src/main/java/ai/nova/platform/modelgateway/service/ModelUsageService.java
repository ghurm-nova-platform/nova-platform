package ai.nova.platform.modelgateway.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.ModelUsageDailyResponse;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.ModelUsageResponse;
import ai.nova.platform.modelgateway.mapper.ModelGatewayMapper;
import ai.nova.platform.modelgateway.repository.ModelUsageDailyRepository;
import ai.nova.platform.modelgateway.security.ModelGatewayAuthorizationService;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class ModelUsageService {

    private final ModelUsageDailyRepository usageRepository;
    private final ProjectRepository projectRepository;
    private final ModelGatewayMapper mapper;
    private final ModelGatewayAuthorizationService authorizationService;

    public ModelUsageService(
            ModelUsageDailyRepository usageRepository,
            ProjectRepository projectRepository,
            ModelGatewayMapper mapper,
            ModelGatewayAuthorizationService authorizationService) {
        this.usageRepository = usageRepository;
        this.projectRepository = projectRepository;
        this.mapper = mapper;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public ModelUsageResponse getUsage(
            UUID projectId, LocalDate from, LocalDate to, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_USAGE_READ);
        requireProject(projectId, user.getOrganizationId());
        LocalDate start = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate end = to != null ? to : LocalDate.now();
        List<ModelUsageDailyResponse> entries = usageRepository
                .findByProjectIdAndOrganizationIdAndUsageDateBetweenOrderByUsageDateDescModelIdAsc(
                        projectId, user.getOrganizationId(), start, end)
                .stream()
                .map(mapper::toUsageResponse)
                .toList();
        return new ModelUsageResponse(entries);
    }

    private void requireProject(UUID projectId, UUID organizationId) {
        projectRepository
                .findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));
    }
}
