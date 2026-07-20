package db.migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Extends audit CHECK constraints for Sprint 4 subsystem coverage and enforces append-only
 * semantics on audit_events, audit_correlation, and audit_indexes at the database layer.
 */
public class V51__AuditDatabaseImmutability extends BaseJavaMigration {

    private static final String ACTION_CHECK = """
            CONSTRAINT chk_audit_events_action CHECK (action IN (
                'CREATE', 'UPDATE', 'DELETE', 'ENABLE', 'DISABLE', 'ARCHIVE', 'APPROVE', 'REJECT',
                'MERGE', 'VALIDATE', 'OBSERVE', 'START', 'COMPLETE', 'FAIL', 'PREPARE', 'READY', 'PUBLISH',
                'LOGIN', 'LOGOUT', 'ACCESS'
            ))""";

    private static final String SOURCE_CHECK = """
            CONSTRAINT chk_audit_events_source CHECK (source IN (
                'PORTAL', 'REST_API', 'SYSTEM', 'SCHEDULER', 'MERGE_AGENT', 'RELEASE_MANAGER',
                'DEPLOYMENT_OBSERVATION', 'ROLLBACK_MANAGER', 'RELEASE_POLICIES', 'ENVIRONMENT_MANAGEMENT',
                'ORCHESTRATION', 'PLANNER', 'CODING', 'REVIEW', 'TESTING', 'PATCH', 'GIT_INTEGRATION',
                'PULL_REQUEST', 'CI_OBSERVATION', 'REPAIR', 'APPROVAL_GATE'
            ))""";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        updateCheckConstraints(connection);
        if (isPostgreSql(connection)) {
            installPostgreSqlImmutability(connection);
        } else if (isH2(connection)) {
            installH2Immutability(connection);
        }
    }

    private void updateCheckConstraints(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE audit_events DROP CONSTRAINT IF EXISTS chk_audit_events_action");
            statement.execute("ALTER TABLE audit_events DROP CONSTRAINT IF EXISTS chk_audit_events_source");
            statement.execute("ALTER TABLE audit_events ADD " + ACTION_CHECK);
            statement.execute("ALTER TABLE audit_events ADD " + SOURCE_CHECK);
        }
    }

    private void installPostgreSqlImmutability(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE OR REPLACE FUNCTION reject_audit_mutation() RETURNS trigger AS $$
                    BEGIN
                      RAISE EXCEPTION 'AUDIT_IMMUTABLE';
                    END;
                    $$ LANGUAGE plpgsql""");
            createPostgreSqlTrigger(statement, "audit_events", "trg_audit_events_immutable");
            createPostgreSqlTrigger(statement, "audit_correlation", "trg_audit_correlation_immutable");
            createPostgreSqlTrigger(statement, "audit_indexes", "trg_audit_indexes_immutable");
        }
    }

    private void createPostgreSqlTrigger(Statement statement, String table, String triggerName) throws SQLException {
        statement.execute("DROP TRIGGER IF EXISTS " + triggerName + " ON " + table);
        statement.execute("""
                CREATE TRIGGER %s
                BEFORE UPDATE OR DELETE ON %s
                FOR EACH ROW EXECUTE PROCEDURE reject_audit_mutation()"""
                .formatted(triggerName, table));
    }

    private void installH2Immutability(Connection connection) throws SQLException {
        String triggerClass = "ai.nova.platform.audit.db.AuditImmutableTrigger";
        try (Statement statement = connection.createStatement()) {
            createH2Trigger(statement, "audit_events", "trg_audit_events_update", "UPDATE", triggerClass);
            createH2Trigger(statement, "audit_events", "trg_audit_events_delete", "DELETE", triggerClass);
            createH2Trigger(statement, "audit_correlation", "trg_audit_correlation_update", "UPDATE", triggerClass);
            createH2Trigger(statement, "audit_correlation", "trg_audit_correlation_delete", "DELETE", triggerClass);
            createH2Trigger(statement, "audit_indexes", "trg_audit_indexes_update", "UPDATE", triggerClass);
            createH2Trigger(statement, "audit_indexes", "trg_audit_indexes_delete", "DELETE", triggerClass);
        }
    }

    private void createH2Trigger(
            Statement statement, String table, String triggerName, String event, String triggerClass)
            throws SQLException {
        statement.execute("DROP TRIGGER IF EXISTS " + triggerName);
        statement.execute("""
                CREATE TRIGGER %s BEFORE %s ON %s
                FOR EACH ROW CALL "%s\""""
                .formatted(triggerName, event, table, triggerClass));
    }

    private boolean isPostgreSql(Connection connection) throws SQLException {
        return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
    }

    private boolean isH2(Connection connection) throws SQLException {
        return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("h2");
    }
}
