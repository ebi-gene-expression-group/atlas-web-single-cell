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

@Configuration
public class TestJdbcConfig {
    @Bean
    public DataSource dataSource() {
        var hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:postgresql://scxa-postgres");
        hikariConfig.setUsername("scxa");
        hikariConfig.setPassword("scxa");
        hikariConfig.setDriverClassName("org.postgresql.ds.PGSimpleDataSource");

        var dataSource = new HikariDataSource(hikariConfig);
        Flyway.configure()
                .dataSource(dataSource)
                .locations("filesystem:./src/test/resources/schemas/flyway/scxa")
                .load()
                .migrate();

        return dataSource;
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
