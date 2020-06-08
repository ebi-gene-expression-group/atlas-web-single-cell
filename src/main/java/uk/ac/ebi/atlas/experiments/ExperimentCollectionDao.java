package uk.ac.ebi.atlas.experiments;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ExperimentCollectionDao implements ExperimentCollectionsRepository {
    private JdbcTemplate jdbcTemplate;

    public ExperimentCollectionDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<String> getExperimentCollections(String experimentAccession) {
        return jdbcTemplate.queryForList(
                "SELECT c.name " +
                "FROM collections AS c " +
                "INNER JOIN experiment2collection AS e2c on c.coll_id = e2c.coll_id " +
                "INNER JOIN experiment AS e on e2c.exp_acc = e.accession " +
                "WHERE e.private=false AND e.accession=?",
                String.class,
                experimentAccession);
    }
}