package db.migration;

import java.sql.Connection;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * V53 — deployment execution concurrency + cancel_requested.
 *
 * PostgreSQL creates the architecture-review partial unique index on
 * (organization_id, environment_id) WHERE status is active.
 *
 * H2 (tests) does not support partial indexes; it uses nullable
 * {@code active_environment_slot} with the same index name for equivalent
 * one-active-execution-per-environment semantics.
 *
 * {@code active_environment_slot} is added on all databases so JPA mapping is stable.
 */
public class V53__deployment_execution_concurrency extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String product = connection.getMetaData().getDatabaseProductName();
        boolean postgres = product != null && product.toLowerCase().contains("postgresql");

        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "ALTER TABLE deployment_executions ADD COLUMN cancel_requested BOOLEAN NOT NULL DEFAULT FALSE");
            statement.execute("ALTER TABLE deployment_executions ADD COLUMN active_environment_slot UUID");

            if (postgres) {
                statement.execute(
                        """
                        CREATE UNIQUE INDEX uq_deploy_exec_one_active_environment
                        ON deployment_executions (organization_id, environment_id)
                        WHERE status IN (
                          'READY',
                          'QUEUED',
                          'STARTING',
                          'DEPLOYING',
                          'VERIFYING'
                        )
                        """);
            } else {
                statement.execute(
                        """
                        CREATE UNIQUE INDEX uq_deploy_exec_one_active_environment
                        ON deployment_executions (organization_id, active_environment_slot)
                        """);
            }
        }
    }
}
