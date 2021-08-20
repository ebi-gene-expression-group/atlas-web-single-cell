package uk.ac.ebi.atlas.experimentpage.cellplot;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.atlas.experimentpage.tsne.TSnePoint;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                        "ON m.cell_id=c.cell_id " +
                        "AND m.experiment_accession=c.experiment_accession " +
                    "WHERE c.method=:method " +
                        "AND c.parameterisation @> :parameterisation::jsonb " +
                        "AND c.experiment_accession=:experiment_accession " +
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
                    "FROM scxa_coords " +
                    "WHERE method=:method " +
                        "AND parameterisation @> :parameterisation::jsonb " +
                        "AND experiment_accession=:experiment_accession";
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
                    "LEFT JOIN scxa_analytics a " +
                        "ON c.experiment_accession=a.experiment_accession " +
                        "AND c.cell_id=a.cell_id AND a.gene_id=:gene_id " +
                    "WHERE c.experiment_accession=:experiment_accession " +
                        "AND c.method=:method " +
                        "AND c.parameterisation @> :parameterisation::jsonb " +
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
