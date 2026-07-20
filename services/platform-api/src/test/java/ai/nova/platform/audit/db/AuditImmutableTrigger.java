package ai.nova.platform.audit.db;

import java.sql.Connection;
import java.sql.SQLException;

import org.h2.api.Trigger;

/**
 * H2 append-only guard for audit tables. INSERT is allowed; UPDATE and DELETE raise AUDIT_IMMUTABLE.
 * Lives in test scope because H2 is test-only; referenced by V51 migration when running against H2.
 */
public class AuditImmutableTrigger implements Trigger {

    @Override
    public void init(
            Connection connection, String schemaName, String triggerName, String tableName, boolean before, int type)
            throws SQLException {
        // no-op
    }

    @Override
    public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
        throw new SQLException("AUDIT_IMMUTABLE");
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public void remove() {
        // no-op
    }
}
