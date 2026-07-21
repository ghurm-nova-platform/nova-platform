package ai.nova.platform.dashboard.support;

import java.util.List;
import java.util.UUID;

import ai.nova.platform.audit.support.AuditTestFixture;
import ai.nova.platform.dashboard.security.DashboardAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;

public final class DashboardTestFixture {

    public static final UUID ORG_ID = AuditTestFixture.ORG_ID;
    public static final UUID USER_ID = AuditTestFixture.USER_ID;
    public static final UUID PROJECT_ID = AuditTestFixture.PROJECT_ID;

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
}
