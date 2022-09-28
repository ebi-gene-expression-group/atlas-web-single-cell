package uk.ac.ebi.atlas.experimentpage.cellplot;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.atlas.experimentpage.tsne.TSnePoint;

import java.util.List;
import java.util.Map;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@Repository
@Transactional(transactionManager = "txManager", readOnly = true)
public class CellPlotDao {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public CellPlotDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    // For some reason we need to explicitly cast :parameterisation or Postgres complains with:
    // ERROR:  operator does not exist: jsonb @> character varying at character 319
    // HINT:  No operator matches the given name and argument type(s). You might need to add explicit type casts.
    // I think NamedParameterJdbcTemplate is at fault here...
    private static final String SELECT_CELL_PLOT_WITH_K_STATEMENT =
            "SELECT c.cell_id, c.x, c.y, g.value AS cluster_id " +
                    "FROM scxa_cell_group g " +
                    "JOIN scxa_cell_group_membership m " +
                        "ON g.id=m.cell_group_id " +
                        "AND g.experiment_accession=:experiment_accession " +
                        "AND g.variable=:variable " +
                    "RIGHT JOIN scxa_coords c " +
                    "INNER JOIN scxa_dimension_reduction sdr on sdr.id = c.dimension_reduction_id " +
                        "ON m.cell_id=c.cell_id " +
                        "AND m.experiment_accession=sdr.experiment_accession " +
                        "WHERE sdr.method=:method " +
                        "AND sdr.parameterisation @> :parameterisation::jsonb " +
                        "AND sdr.experiment_accession=:experiment_accession " +
            "ORDER BY cast(g.value as integer) ASC";
    public List<TSnePoint.Dto> fetchCellPlotWithK(String experimentAccession,
                                                  int k,
                                                  String plotMethod,
                                                  Map<String, Integer> plotParameters) {
        var namedParameters =
                ImmutableMap.of(
                        "experiment_accession", experimentAccession,
                        "variable", String.valueOf(k),
                        "method", plotMethod,
                        // We wrap in a list because the parameterisation column is an array
                        "parameterisation", GSON.toJson(ImmutableList.of(plotParameters)));

        return namedParameterJdbcTemplate.query(
                SELECT_CELL_PLOT_WITH_K_STATEMENT,
                namedParameters,
                (rs, rowNum) ->
                        TSnePoint.Dto.create(
                                rs.getDouble("x"),
                                rs.getDouble("y"),
                                rs.getString("cluster_id"),
                                rs.getString("cell_id")));
    }

    private static final String SELECT_CELL_PLOT_STATEMENT =
            "SELECT cell_id, x, y " +
                    "FROM scxa_coords AS coords " +
                    "INNER JOIN scxa_dimension_reduction sdr on sdr.id = coords.dimension_reduction_id " +
                    "WHERE sdr.method=:method " +
                        "AND sdr.parameterisation @> :parameterisation::jsonb " +
                        "AND sdr.experiment_accession=:experiment_accession";
    public List<TSnePoint.Dto> fetchCellPlot(String experimentAccession,
                                             String plotMethod,
                                             Map<String, Integer> plotParameters) {
        var namedParameters =
                ImmutableMap.of(
                        "experiment_accession", experimentAccession,
                        "method", plotMethod,
                        // We wrap in a list because the parameterisation column is an array
                        "parameterisation", GSON.toJson(ImmutableList.of(plotParameters)));

        return namedParameterJdbcTemplate.query(
                SELECT_CELL_PLOT_STATEMENT,
                namedParameters,
                (rs, rowNum) ->
                        TSnePoint.Dto.create(
                                rs.getDouble("x"),
                                rs.getDouble("y"),
                                rs.getString("cell_id")));
    }

    private static final String SELECT_CELL_PLOT_WITH_EXPRESSION_STATEMENT =
            "SELECT c.cell_id, c.x, c.y, a.expression_level " +
                    "FROM scxa_coords c " +
                    "INNER JOIN scxa_dimension_reduction sdr on sdr.id = c.dimension_reduction_id " +
                    "LEFT JOIN scxa_analytics a " +
                        "ON sdr.experiment_accession=a.experiment_accession " +
                        "AND c.cell_id=a.cell_id AND a.gene_id=:gene_id " +
                    "WHERE sdr.experiment_accession=:experiment_accession " +
                        "AND sdr.method=:method " +
                        "AND sdr.parameterisation @> :parameterisation::jsonb " +
                    "ORDER BY c.cell_id";
    public List<TSnePoint.Dto> fetchCellPlotWithExpression(String experimentAccession,
                                                           String geneId,
                                                           String plotMethod,
                                                           Map<String, Integer> plotParameters) {
        var namedParameters = ImmutableMap.of(
                "experiment_accession", experimentAccession,
                "gene_id", geneId,
                "method", plotMethod,
                // We wrap in a list because the parameterisation column is an array
                "parameterisation", GSON.toJson(ImmutableList.of(plotParameters)));

        return namedParameterJdbcTemplate.query(
                SELECT_CELL_PLOT_WITH_EXPRESSION_STATEMENT,
                namedParameters,
                (rs, rowNum) ->
                        TSnePoint.Dto.create(
                                rs.getDouble("x"),
                                rs.getDouble("y"),
                                rs.getDouble("expression_level"),
                                rs.getString("cell_id")));
    }
}
