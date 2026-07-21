package ai.nova.platform.dashboard.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.audit.dto.AuditDtos.AuditSearchRequest;
import ai.nova.platform.dashboard.config.DashboardProperties;
import ai.nova.platform.dashboard.dto.DashboardDtos.ApprovalsSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.AuditSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.CiSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.CostSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.DashboardConfigResponse;
import ai.nova.platform.dashboard.dto.DashboardDtos.DashboardMeta;
import ai.nova.platform.dashboard.dto.DashboardDtos.DashboardRefreshResponse;
import ai.nova.platform.dashboard.dto.DashboardDtos.DashboardSnapshot;
import ai.nova.platform.dashboard.dto.DashboardDtos.DeploymentsSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.EnvironmentsSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.OverviewSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.PipelineSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.ReleasesSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.RollbacksSection;
import ai.nova.platform.dashboard.security.DashboardAuthorizationService;
import ai.nova.platform.dashboard.service.DashboardCacheService.CachedSnapshot;
import ai.nova.platform.dashboard.service.DashboardExportService.ExportPayload;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class DashboardService {

    private final DashboardProperties properties;
    private final DashboardAuthorizationService authorizationService;
    private final DashboardCacheService cacheService;
    private final DashboardAggregationService aggregationService;
    private final DashboardExportService exportService;

    public DashboardService(
            DashboardProperties properties,
            DashboardAuthorizationService authorizationService,
            DashboardCacheService cacheService,
            DashboardAggregationService aggregationService,
            DashboardExportService exportService) {
        this.properties = properties;
        this.authorizationService = authorizationService;
        this.cacheService = cacheService;
        this.aggregationService = aggregationService;
        this.exportService = exportService;
    }

    @Transactional(readOnly = true)
    public DashboardSnapshot getSnapshot(AuthenticatedUser user, UUID projectId) {
        authorizationService.requireRead(user);
        requireEnabled();
        return resolveSnapshot(user, projectId).snapshot();
    }

    @Transactional(readOnly = true)
    public OverviewSection getOverview(AuthenticatedUser user, UUID projectId) {
        authorizationService.requireRead(user);
        requireEnabled();
        return resolveSnapshot(user, projectId).snapshot().overview();
    }

    @Transactional(readOnly = true)
    public PipelineSection getPipeline(AuthenticatedUser user, UUID projectId) {
        authorizationService.requireRead(user);
        requireEnabled();
        return resolveSnapshot(user, projectId).snapshot().pipeline();
    }

    @Transactional(readOnly = true)
    public DeploymentsSection getDeployments(AuthenticatedUser user, UUID projectId) {
        authorizationService.requireRead(user);
        requireEnabled();
        return resolveSnapshot(user, projectId).snapshot().deployments();
    }

    @Transactional(readOnly = true)
    public ReleasesSection getReleases(AuthenticatedUser user, UUID projectId) {
        authorizationService.requireRead(user);
        requireEnabled();
        return resolveSnapshot(user, projectId).snapshot().releases();
    }

    @Transactional(readOnly = true)
    public EnvironmentsSection getEnvironments(AuthenticatedUser user, UUID projectId) {
        authorizationService.requireRead(user);
        requireEnabled();
        return resolveSnapshot(user, projectId).snapshot().environments();
    }

    @Transactional(readOnly = true)
    public AuditSection getAudit(AuthenticatedUser user, UUID projectId, AuditSearchRequest filters) {
        authorizationService.requireRead(user);
        requireEnabled();
        return aggregationService.aggregateAudit(user, projectId, filters);
    }

    @Transactional(readOnly = true)
    public ApprovalsSection getApprovals(AuthenticatedUser user, UUID projectId) {
        authorizationService.requireRead(user);
        requireEnabled();
        return resolveSnapshot(user, projectId).snapshot().approvals();
    }

    @Transactional(readOnly = true)
    public CiSection getCi(AuthenticatedUser user, UUID projectId) {
        authorizationService.requireRead(user);
        requireEnabled();
        return resolveSnapshot(user, projectId).snapshot().ci();
    }

    @Transactional(readOnly = true)
    public RollbacksSection getRollbacks(AuthenticatedUser user, UUID projectId) {
        authorizationService.requireRead(user);
        requireEnabled();
        return resolveSnapshot(user, projectId).snapshot().rollbacks();
    }

    @Transactional(readOnly = true)
    public CostSection getCost(AuthenticatedUser user, UUID projectId) {
        authorizationService.requireRead(user);
        requireEnabled();
        return resolveSnapshot(user, projectId).snapshot().cost();
    }

    @Transactional(readOnly = true)
    public DashboardConfigResponse getConfig(AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return new DashboardConfigResponse(
                properties.isEnabled(),
                properties.getRefreshRateSeconds(),
                properties.getCache().getTtlSeconds());
    }

    public DashboardRefreshResponse refresh(AuthenticatedUser user, UUID projectId) {
        authorizationService.requireAdmin(user);
        requireEnabled();
        cacheService.invalidate(user.getOrganizationId(), projectId);
        cacheService.invalidateOrganization(user.getOrganizationId());
        CachedSnapshot cached = buildAndCache(user, projectId);
        return new DashboardRefreshResponse(Instant.now(), cached.expiresAt());
    }

    @Transactional(readOnly = true)
    public ExportPayload export(AuthenticatedUser user, UUID projectId, String format, String section) {
        authorizationService.requireRead(user);
        requireEnabled();
        DashboardSnapshot snapshot = resolveSnapshot(user, projectId).snapshot();
        return exportService.export(snapshot, format, section);
    }

    private CachedSnapshot resolveSnapshot(AuthenticatedUser user, UUID projectId) {
        return cacheService
                .get(user.getOrganizationId(), projectId)
                .orElseGet(() -> buildAndCache(user, projectId));
    }

    private CachedSnapshot buildAndCache(AuthenticatedUser user, UUID projectId) {
        Instant generatedAt = Instant.now();
        DashboardSnapshot snapshot = new DashboardSnapshot(
                new DashboardMeta(
                        user.getOrganizationId(),
                        projectId,
                        generatedAt,
                        generatedAt.plusSeconds(Math.max(properties.getCache().getTtlSeconds(), 1)),
                        properties.getRefreshRateSeconds(),
                        false),
                aggregationService.aggregateOverview(user, projectId),
                aggregationService.aggregatePipeline(user, projectId),
                aggregationService.aggregateDeployments(user, projectId),
                aggregationService.aggregateReleases(user, projectId),
                aggregationService.aggregateEnvironments(user, projectId),
                aggregationService.aggregateAudit(user, projectId, defaultAuditFilters(projectId)),
                aggregationService.aggregateApprovals(user, projectId),
                aggregationService.aggregateCi(user, projectId),
                aggregationService.aggregateRollbacks(user, projectId),
                aggregationService.aggregateCost(user, projectId));
        CachedSnapshot cached = cacheService.put(user.getOrganizationId(), projectId, snapshot);
        DashboardSnapshot withMeta = new DashboardSnapshot(
                new DashboardMeta(
                        snapshot.meta().organizationId(),
                        snapshot.meta().projectId(),
                        snapshot.meta().generatedAt(),
                        cached.expiresAt(),
                        snapshot.meta().refreshRateSeconds(),
                        cached.fromCache()),
                snapshot.overview(),
                snapshot.pipeline(),
                snapshot.deployments(),
                snapshot.releases(),
                snapshot.environments(),
                snapshot.audit(),
                snapshot.approvals(),
                snapshot.ci(),
                snapshot.rollbacks(),
                snapshot.cost());
        if (cached.fromCache()) {
            return new CachedSnapshot(withMeta, cached.expiresAt(), true);
        }
        cacheService.put(user.getOrganizationId(), projectId, withMeta);
        return new CachedSnapshot(withMeta, cached.expiresAt(), false);
    }

    private AuditSearchRequest defaultAuditFilters(UUID projectId) {
        return new AuditSearchRequest(null, null, projectId, null, null, null, null, null, null, null, null, 0, 25);
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "DASHBOARD_DISABLED", "Dashboard is disabled");
        }
    }
}
