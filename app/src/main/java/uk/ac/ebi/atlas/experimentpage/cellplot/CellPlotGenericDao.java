package uk.ac.ebi.atlas.experimentpage.cellplot;

import com.google.common.collect.ImmutableMap;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(transactionManager = "txManager", readOnly = true)
public class CellPlotGenericDao {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public CellPlotGenericDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    private static final String SELECT_CELL_PLOT_PARAMETERS =
            "SELECT parameterisation " +
                    "FROM scxa_dimension_reduction " +
                    "WHERE method=:method AND experiment_accession=:experiment_accession ";
    public List<String> getQueryParams(String plotMethod, String experiment_accession) {
        var namedParameters =
                ImmutableMap.of(
                        "method", plotMethod,
                        "experiment_accession", experiment_accession);

        return namedParameterJdbcTemplate.query(
                SELECT_CELL_PLOT_PARAMETERS,
                namedParameters,
                (rs, rowNum) -> rs.getString("parameterisation"));
    }
}
