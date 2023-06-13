package uk.ac.ebi.atlas.experiments;

import com.google.common.collect.ImmutableMap;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(transactionManager = "txManager", readOnly = true)
public class ExperimentCellCountDaoImpl implements ExperimentCellCountDao {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public ExperimentCellCountDaoImpl(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    //writing query in this manner is significiantly faster as compared to doing "COUNT(DISTINCT(cell_id))"
    //see here https://stackoverflow.com/questions/11250253/postgresql-countdistinct-very-slow
    private static final String COUNT_CELLS_BY_EXPERIMENT_ACCESSION =
            "SELECT COUNT(*) FROM " +
                    "(SELECT DISTINCT (cell_id) " +
                    "FROM scxa_cell_group_membership " +
                    "WHERE experiment_accession=:experiment_accession) AS cell_ids";

    @Override
    public Integer fetchNumberOfCellsByExperimentAccession(String experimentAccession) {
        var namedParameters = ImmutableMap.of("experiment_accession", experimentAccession);

        return namedParameterJdbcTemplate.queryForObject(COUNT_CELLS_BY_EXPERIMENT_ACCESSION, namedParameters,
                Integer.class);
    }
}
