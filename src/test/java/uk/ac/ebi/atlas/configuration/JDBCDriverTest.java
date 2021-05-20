package uk.ac.ebi.atlas.configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JDBCDriverTest extends AbstractContainerBaseTest {

    public static void sampleInitFunction(Connection connection) throws SQLException {
        // e.g. run schema setup or Flyway/liquibase/etc DB migrations here...
        String jdbcUrl = POSTGRES_SQL_CONTAINER.getJdbcUrl();
        String username = POSTGRES_SQL_CONTAINER.getUsername();
        String password = POSTGRES_SQL_CONTAINER.getPassword();
        connection = DriverManager
                .getConnection(jdbcUrl, username, password);

    }
}
