package uk.ac.ebi.atlas.configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JDBCDriverTest extends AbstractContainerBaseTest {

    public static void sampleInitFunction(Connection connection) throws SQLException {
        // e.g. run schema setup or Flyway/liquibase/etc DB migrations here...
        String jdbcUrl = testDatabase.getJdbcUrl();
        String username = testDatabase.getUsername();
        String password = testDatabase.getPassword();
        connection = DriverManager
                .getConnection(jdbcUrl, username, password);

    }
}
