package uk.ac.ebi.atlas.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Configuration
abstract class AbstractContainerBaseTest {

    static final PostgreSQLContainer POSTGRES_SQL_CONTAINER;

    static {
        POSTGRES_SQL_CONTAINER =  new PostgreSQLContainer();

        POSTGRES_SQL_CONTAINER.start();
        POSTGRES_SQL_CONTAINER.withInitScript("resources/schemas/db/gxasc-schema.sql");
        POSTGRES_SQL_CONTAINER.withInitScript("resources/schemas/db/shared-schema.sql");
    }

    protected DataSource getDataSource(JdbcDatabaseContainer<?> container) {
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
        String url = POSTGRES_SQL_CONTAINER.getJdbcUrl();
        System.out.println("url:"+url);
        System.out.println("username:"+POSTGRES_SQL_CONTAINER.getUsername());
        System.out.println("host:"+POSTGRES_SQL_CONTAINER.getContainerIpAddress());
        System.out.println("flyway:"+POSTGRES_SQL_CONTAINER.getLogs());
        System.out.println("datasource:"+getDataSource(POSTGRES_SQL_CONTAINER));
//         create a connection and run test as normal
        assertTrue(POSTGRES_SQL_CONTAINER.isRunning());

    }
}
