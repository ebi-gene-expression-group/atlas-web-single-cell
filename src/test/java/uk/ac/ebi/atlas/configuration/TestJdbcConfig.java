package uk.ac.ebi.atlas.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;

@Configuration
public class TestJdbcConfig {
    static final PostgreSQLContainer TEST_DATABASE;
    static final String DOCKER_IMAGE_VERSION = "postgres:10-alpine";
    static final String FLY_WAY_PATH = "filesystem:./src/test/resources/schemas/flyway/scxa";

    @Bean
    public DataSource dataSource() {
        return getDataSource(TEST_DATABASE);
    }

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

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(dataSource());
    }

    @Bean
    public PlatformTransactionManager txManager() {
        return new DataSourceTransactionManager(dataSource());
    }
}
