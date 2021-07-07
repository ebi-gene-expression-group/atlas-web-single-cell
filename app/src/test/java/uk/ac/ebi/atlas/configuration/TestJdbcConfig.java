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

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class TestJdbcConfig {
    private final HikariConfig hikariConfig;

    public TestJdbcConfig() {
        hikariConfig = new HikariConfig();
        hikariConfig.setMaximumPoolSize(20);
        hikariConfig.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
        hikariConfig.setPoolName("scxa");
        hikariConfig.setMaxLifetime(30000L);    // EBI policy requires URL requests to be resolved within 30 seconds

        Properties dataSourceProperties = new Properties();
        dataSourceProperties.setProperty("url", "jdbc:postgresql://scxa-postgres:5432/scxa");
        dataSourceProperties.setProperty("user", "scxa");
        dataSourceProperties.setProperty("password", "scxa");

        hikariConfig.setDataSourceProperties(dataSourceProperties);

        hikariConfig.setConnectionTestQuery("SELECT 1");

        var dataSource = new HikariDataSource(hikariConfig);
        Flyway.configure()
                .dataSource(dataSource)
                .locations("filesystem:./src/test/resources/schemas/flyway/scxa")
                .load()
                .migrate();
    }

    @Bean
    public DataSource dataSource() {
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
