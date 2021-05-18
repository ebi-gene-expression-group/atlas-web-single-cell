package uk.ac.ebi.atlas.search;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Return a list of species names sorted by how many times they appear in experiments
@Component
@Transactional(transactionManager = "txManager", readOnly = true)
public class FeaturedSpeciesDao {
    private final JdbcTemplate jdbcTemplate;

    public FeaturedSpeciesDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> fetchSpeciesSortedByExperimentCount() {
        return jdbcTemplate.queryForList(
                "SELECT n.species FROM " +
                        "(SELECT species, COUNT(species) AS count FROM experiment " +
                        "WHERE private=FALSE GROUP BY species ORDER BY count DESC) n",
                String.class);
    }
}
