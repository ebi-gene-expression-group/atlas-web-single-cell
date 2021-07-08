package uk.ac.ebi.atlas.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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
        hikariConfig.setJdbcUrl("jdbc:postgresql://scxa-postgres:5432/gxpatlasloc");
        hikariConfig.setUsername("atlas3dev");
        hikariConfig.setPassword("atlas3dev");
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        var dataSource = new HikariDataSource(hikariConfig);
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
