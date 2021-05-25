package uk.ac.ebi.atlas.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class AbstractContainerBaseTest {
    static final PostgreSQLContainer TEST_DATABASE;
    static final String DOCKER_IMAGE_VERSION = "postgres:10-alpine";
    static final String FLY_WAY_PATH = "filesystem:./src/test/resources/schemas/flyway/scxa";

    static {
        TEST_DATABASE = new PostgreSQLContainer(DOCKER_IMAGE_VERSION);
        TEST_DATABASE.start();
        Flyway.configure().dataSource(getDataSource(TEST_DATABASE))
                .locations(FLY_WAY_PATH)
                .load()
                .migrate();
    }

    protected static DataSource getDataSource(JdbcDatabaseContainer<?> container) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(container.getJdbcUrl());
        hikariConfig.setUsername(container.getUsername());
        hikariConfig.setPassword(container.getPassword());
        hikariConfig.setDriverClassName(container.getDriverClassName());
        return new HikariDataSource(hikariConfig);
    }


}	

class FirstTest extends AbstractContainerBaseTest {	

    @Test	
    void someTestMethod() {	
        String url = TEST_DATABASE.getJdbcUrl();
        System.out.println("url:"+url);	
        System.out.println("username:"+TEST_DATABASE.getUsername());
        System.out.println("host:"+TEST_DATABASE.getContainerIpAddress());
        System.out.println("flyway:"+TEST_DATABASE.getLogs());
//         create a connection and run test as normal	
        assertTrue(TEST_DATABASE.isRunning());
    }	
}	

