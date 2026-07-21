package db.migration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Flyway entrypoint for {@code classpath:db/scripts/V51__audit_database_immutability.sql}.
 *
 * <p>PostgreSQL executes the SQL script (CHECK extensions + {@code reject_audit_mutation} triggers).
 * H2 (tests) applies the same CHECK extensions and installs equivalent Java triggers because H2
 * cannot run PL/pgSQL.
 */
public class V51__audit_database_immutability extends BaseJavaMigration {

    private static final String SCRIPT = "/db/scripts/V51__audit_database_immutability.sql";

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
        if (isPostgreSql(connection)) {
            executeSqlScript(connection, loadScript());
            return;
        }
        updateCheckConstraints(connection);
        if (isH2(connection)) {
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

    private void executeSqlScript(Connection connection, String script) throws SQLException {
        for (String statementSql : splitStatements(script)) {
            if (statementSql.isBlank()) {
                continue;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(statementSql);
            }
        }
    }

    private List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inDollar = false;
        for (String line : script.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--") && !inDollar) {
                continue;
            }
            if (trimmed.contains("$$")) {
                inDollar = !inDollar;
            }
            current.append(line).append('\n');
            if (!inDollar && trimmed.endsWith(";")) {
                statements.add(current.toString().trim());
                current.setLength(0);
            }
        }
        if (!current.toString().isBlank()) {
            statements.add(current.toString().trim());
        }
        return statements;
    }

    private String loadScript() throws Exception {
        try (var in = getClass().getResourceAsStream(SCRIPT)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource " + SCRIPT);
            }
            try (var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }

    private boolean isPostgreSql(Connection connection) throws SQLException {
        return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
    }

    private boolean isH2(Connection connection) throws SQLException {
        return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("h2");
    }
}
