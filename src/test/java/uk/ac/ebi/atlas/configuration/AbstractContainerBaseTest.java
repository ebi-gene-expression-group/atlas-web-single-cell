package uk.ac.ebi.atlas.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.PostgreSQLContainer;

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
}

class FirstTest extends AbstractContainerBaseTest {

    @Test
    void someTestMethod() {
        String url = POSTGRES_SQL_CONTAINER.getJdbcUrl();
        System.out.println("url:"+url);
        System.out.println("username:"+POSTGRES_SQL_CONTAINER.getUsername());
        System.out.println("host:"+POSTGRES_SQL_CONTAINER.getContainerIpAddress());
        System.out.println("flyway:"+POSTGRES_SQL_CONTAINER.getLogs());
//         create a connection and run test as normal
        assertTrue(POSTGRES_SQL_CONTAINER.isRunning());

    }
}
