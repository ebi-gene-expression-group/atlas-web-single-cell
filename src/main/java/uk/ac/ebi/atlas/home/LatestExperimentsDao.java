package uk.ac.ebi.atlas.home;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LatestExperimentsDao {
    private final JdbcTemplate jdbcTemplate;

    public LatestExperimentsDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> fetchLatestExperimentAccessions() {
        return jdbcTemplate.queryForList(
                "SELECT accession FROM experiment WHERE private=FALSE ORDER BY last_update DESC LIMIT 5",
                String.class);
    }

    public long fetchPublicExperimentsCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM experiment WHERE private=FALSE",
                Long.class);
    }
}
