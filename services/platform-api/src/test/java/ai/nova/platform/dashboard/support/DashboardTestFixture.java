package ai.nova.platform.dashboard.support;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ai.nova.platform.audit.dto.AuditDtos.AuditEvent;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSeverity;
import ai.nova.platform.audit.entity.AuditSource;
import ai.nova.platform.audit.support.AuditTestFixture;
import ai.nova.platform.dashboard.dto.DashboardDtos.ApprovalsSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.AuditSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.CiSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.CostSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.DashboardKpis;
import ai.nova.platform.dashboard.dto.DashboardDtos.DashboardMeta;
import ai.nova.platform.dashboard.dto.DashboardDtos.DashboardSnapshot;
import ai.nova.platform.dashboard.dto.DashboardDtos.DeploymentsSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.EnvironmentsSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.OverviewSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.PipelineSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.ReleaseSnapshot;
import ai.nova.platform.dashboard.dto.DashboardDtos.ReleasesSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.RollbacksSection;
import ai.nova.platform.dashboard.security.DashboardAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;

public final class DashboardTestFixture {

    public static final UUID ORG_ID = AuditTestFixture.ORG_ID;
    public static final UUID USER_ID = AuditTestFixture.USER_ID;
    public static final UUID PROJECT_ID = AuditTestFixture.PROJECT_ID;
    public static final UUID OTHER_ORG_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    public static final UUID OTHER_USER_ID = UUID.fromString("99999999-9999-9999-9999-999999999998");
    public static final UUID OTHER_PROJECT_ID = UUID.fromString("99999999-9999-9999-9999-999999999997");

    private DashboardTestFixture() {
    }

    public static AuthenticatedUser dashboardAdminUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "dashboard-admin@nova.local",
                "Dashboard Admin",
                List.of("ORG_ADMIN"),
                List.of(
                        DashboardAuthorizationService.DASHBOARD_READ,
                        DashboardAuthorizationService.DASHBOARD_ADMIN,
                        "AUDIT_READ",
                        "RELEASE_READ",
                        "ENVIRONMENT_READ"),
                true);
    }

    public static AuthenticatedUser dashboardReadOnlyUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "dashboard-reader@nova.local",
                "Dashboard Reader",
                List.of("USER"),
                List.of(DashboardAuthorizationService.DASHBOARD_READ, "AUDIT_READ"),
                true);
    }

    public static AuthenticatedUser dashboardOtherOrgAdminUser() {
        return new AuthenticatedUser(
                OTHER_USER_ID,
                OTHER_ORG_ID,
                "dashboard-admin-b@nova.local",
                "Dashboard Admin B",
                List.of("ORG_ADMIN"),
                List.of(
                        DashboardAuthorizationService.DASHBOARD_READ,
                        DashboardAuthorizationService.DASHBOARD_ADMIN,
                        "AUDIT_READ"),
                true);
    }

    public static AuthenticatedUser dashboardOtherOrgReadOnlyUser() {
        return new AuthenticatedUser(
                OTHER_USER_ID,
                OTHER_ORG_ID,
                "dashboard-reader-b@nova.local",
                "Dashboard Reader B",
                List.of("USER"),
                List.of(DashboardAuthorizationService.DASHBOARD_READ, "AUDIT_READ"),
                true);
    }

    public static AuthenticatedUser dashboardNoPermissionUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "dashboard-noperm@nova.local",
                "No Dashboard Perm",
                List.of("USER"),
                List.of("ENVIRONMENT_READ"),
                true);
    }

    public static DashboardSnapshot sampleSnapshot(String releaseName, String auditLabel, int auditCount) {
        Instant now = Instant.parse("2026-07-21T09:00:00Z");
        return new DashboardSnapshot(
                new DashboardMeta(ORG_ID, PROJECT_ID, now, now.plusSeconds(30), 30, false),
                new OverviewSection(
                        1, 2, 3, 4, 5, 6, 7, 8, auditCount, 9, 1, 2,
                        new DashboardKpis(90.0, 80.0, 70.0, 60.0, 50.0, 40.0, 100L, 200L, 300L, 400L, 500L, 600L)),
                new PipelineSection(List.of(), 0),
                new DeploymentsSection(List.of(), 0, 0, 0),
                new ReleasesSection(
                        1,
                        1,
                        0,
                        0,
                        0,
                        1,
                        List.of(new ReleaseSnapshot(
                                UUID.randomUUID(), PROJECT_ID, releaseName, "1.0.0", "PUBLISHED", now, now))),
                new EnvironmentsSection(List.of(), List.of()),
                new AuditSection(List.of(sampleAuditEvent(auditLabel, now)), auditCount),
                new ApprovalsSection(0, 0, 0, 0, List.of()),
                new CiSection(List.of(), 0, 0, 0, 0),
                new RollbacksSection(0, 0, 0, 0.0, 0, List.of()),
                new CostSection(0.0, List.of(), 0.0, "placeholder only"));
    }

    public static AuditEvent sampleAuditEvent(String label, Instant createdAt) {
        return new AuditEvent(
                UUID.randomUUID(),
                ORG_ID,
                PROJECT_ID,
                USER_ID,
                "Dashboard User",
                null,
                AuditEntityType.RELEASE,
                UUID.randomUUID(),
                label,
                AuditAction.ACCESS,
                AuditResult.SUCCESS,
                AuditSeverity.MEDIUM,
                AuditSource.PORTAL,
                "corr",
                "req",
                "127.0.0.1",
                "test",
                Map.of("label", label),
                createdAt);
    }
}
