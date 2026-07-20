package ai.nova.platform.audit.support;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import ai.nova.platform.audit.dto.AuditDtos.RecordAuditEventRequest;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSeverity;
import ai.nova.platform.audit.entity.AuditSource;
import ai.nova.platform.environment.support.EnvironmentTestFixture;
import ai.nova.platform.security.AuthenticatedUser;

public final class AuditTestFixture {

    public static final UUID ORG_ID = EnvironmentTestFixture.ORG_ID;
    public static final UUID USER_ID = EnvironmentTestFixture.USER_ID;
    public static final UUID PROJECT_ID = EnvironmentTestFixture.PROJECT_ID;

    private AuditTestFixture() {
    }

    public static AuthenticatedUser auditAdminUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "admin@nova.local",
                "Nova Admin",
                List.of("ORG_ADMIN"),
                List.of(
                        "AUDIT_READ",
                        "AUDIT_WRITE",
                        "ENVIRONMENT_RUN",
                        "ENVIRONMENT_READ",
                        "POLICY_RUN",
                        "POLICY_READ",
                        "RELEASE_RUN",
                        "ORCHESTRATION_RUN_CREATE"),
                true);
    }

    public static AuthenticatedUser auditReadOnlyUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "reader@nova.local",
                "Nova Reader",
                List.of("USER"),
                List.of("AUDIT_READ"),
                true);
    }

    public static AuthenticatedUser auditNoPermissionUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "noperm@nova.local",
                "No Perm",
                List.of("USER"),
                List.of("ENVIRONMENT_READ"),
                true);
    }

    public static RecordAuditEventRequest sampleEvent(UUID entityId, AuditAction action) {
        return new RecordAuditEventRequest(
                ORG_ID,
                PROJECT_ID,
                USER_ID,
                "Nova Admin",
                null,
                AuditEntityType.ENVIRONMENT,
                entityId,
                "test-env",
                action,
                AuditResult.SUCCESS,
                AuditSeverity.MEDIUM,
                AuditSource.ENVIRONMENT_MANAGEMENT,
                "corr-" + UUID.randomUUID(),
                "req-" + UUID.randomUUID(),
                "127.0.0.1",
                "audit-test",
                Map.of("note", "fixture"));
    }
}
