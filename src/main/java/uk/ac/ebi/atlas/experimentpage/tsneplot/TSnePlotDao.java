package uk.ac.ebi.atlas.experimentpage.tsneplot;

import com.google.common.collect.ImmutableMap;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.atlas.experimentpage.tsne.TSnePoint;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TSnePlotDao {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public TSnePlotDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    private static final String SELECT_T_SNE_PLOT_WITH_EXPRESSION_STATEMENT =
            "SELECT tsne.cell_id, tsne.x, tsne.y, analytics.expression_level " +
                    "FROM scxa_tsne AS tsne " +
                    "LEFT JOIN " +
                    "(SELECT * FROM scxa_analytics WHERE gene_id=:gene_id and experiment_accession=:experiment_accession) AS analytics " +
                    "ON analytics.cell_id=tsne.cell_id " +
                    "WHERE tsne.experiment_accession=:experiment_accession AND tsne.perplexity=:perplexity";

    @Transactional(transactionManager = "txManager", readOnly = true)
    public List<TSnePoint.Dto> fetchTSnePlotWithExpression(String experimentAccession, int perplexity, String geneId) {
        var namedParameters =
                ImmutableMap.of(
                        "experiment_accession", experimentAccession,
                        "perplexity", perplexity,
                        "gene_id", geneId);

        return namedParameterJdbcTemplate.query(
                SELECT_T_SNE_PLOT_WITH_EXPRESSION_STATEMENT,
                namedParameters,
                (rs, rowNum) -> TSnePoint.Dto.create(
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("expression_level"),
                        rs.getString("cell_id")));
    }

    private static final String SELECT_T_SNE_PLOT_WITH_CLUSTERS_STATEMENT =
			"SELECT tsne.cell_id, tsne.x, tsne.y, clusters.cluster_id" +
					"            FROM scxa_tsne AS tsne" +
					"                LEFT JOIN" +
					"                (SELECT mem.*, g.value as cluster_id FROM scxa_cell_group_membership as mem JOIN  scxa_cell_group g" +
					"                    on mem.cell_group_id = g.id WHERE g.variable = :k AND mem.experiment_accession = :experiment_accession) AS clusters" +
					"                ON clusters.cell_id=tsne.cell_id " +
					"            WHERE tsne.experiment_accession = :experiment_accession AND tsne.perplexity = :perplexity";

    @Transactional(transactionManager = "txManager", readOnly = true)
    public List<TSnePoint.Dto> fetchTSnePlotWithClusters(String experimentAccession, int perplexity, int k) {
        var namedParameters =
                ImmutableMap.of(
                        "experiment_accession", experimentAccession,
                        "perplexity", perplexity,
                        "k", String.valueOf(k));

        return namedParameterJdbcTemplate.query(
                SELECT_T_SNE_PLOT_WITH_CLUSTERS_STATEMENT,
                namedParameters,
                (rs, rowNum) -> TSnePoint.Dto.create(
                        rs.getDouble("x"), rs.getDouble("y"), rs.getInt("cluster_id"), rs.getString("cell_id")));
    }

    private static final String SELECT_T_SNE_PLOT_WITHOUT_CLUSTERS_STATEMENT =
            "SELECT tsne.cell_id, tsne.x, tsne.y " +
                    "FROM scxa_tsne AS tsne " +
                    "WHERE tsne.experiment_accession=:experiment_accession AND tsne.perplexity=:perplexity";

    public List<TSnePoint.Dto> fetchTSnePlotForPerplexity(String experimentAccession, int perplexity) {
        var namedParameters =
                ImmutableMap.of(
                        "experiment_accession", experimentAccession,
                        "perplexity", perplexity);

        return namedParameterJdbcTemplate.query(
                SELECT_T_SNE_PLOT_WITHOUT_CLUSTERS_STATEMENT,
                namedParameters,
                (rs, rowNum) -> TSnePoint.Dto.create(
                        rs.getDouble("x"), rs.getDouble("y"), rs.getString("cell_id")));
    }

    private static final String SELECT_DISTINCT_PERPLEXITIES_STATEMENT =
            "SELECT DISTINCT perplexity FROM scxa_tsne WHERE experiment_accession=:experiment_accession";

    public List<Integer> fetchPerplexities(String experimentAccession) {
        var namedParameters = ImmutableMap.of("experiment_accession", experimentAccession);

        return namedParameterJdbcTemplate.queryForList(
                SELECT_DISTINCT_PERPLEXITIES_STATEMENT,
                namedParameters,
                Integer.class);
    }

    private static final String COUNT_CELLS_BY_EXPERIMENT_ACCESSION =
            "SELECT COUNT(DISTINCT(cell_id)) FROM scxa_tsne WHERE experiment_accession=:experiment_accession";

    public Integer fetchNumberOfCellsByExperimentAccession(String experimentAccession) {
        var namedParameters = ImmutableMap.of("experiment_accession", experimentAccession);

        return namedParameterJdbcTemplate.queryForObject(
                COUNT_CELLS_BY_EXPERIMENT_ACCESSION,
                namedParameters,
                Integer.class);
    }

    private static final String SELECT_DISTINCT_T_SNE_PLOT_TYPES_AND_OPTIONS =
            "SELECT DISTINCT method,option FROM scxa_coords AS coords " +
                    ",jsonb_array_elements(coords.parameterisation) option " +
                    "WHERE experiment_accession =:experiment_accession ORDER BY option ASC";

    /**
     * Gets response of the TSne PlotTypes and TSne Plot Options for the experiment
     * @param experimentAccession
     * @return Map<PlotType, List<String>PlotOptions> -
     * {
     *   "tsne": [{ "perplexity": 10 }, { "perplexity": 20 } ...],
     *   "umap": [{ "n_neighbours": 3 }, ... ]|
     * }
     */
    public Map<String, List<String>> fetchTSnePlotTypesAndOptions(String experimentAccession) {
        var namedParameters = ImmutableMap.of("experiment_accession", experimentAccession);

        return namedParameterJdbcTemplate.query(SELECT_DISTINCT_T_SNE_PLOT_TYPES_AND_OPTIONS,
                namedParameters,
                (ResultSet resultSet) -> {
                    Map<String, List<String>> plotTypeAndOptions = new HashMap<>();
                    while (resultSet.next()) {
                        var projectionMethod = resultSet.getString("method");
                        var plotOption = resultSet.getString("option");
                        var plotOptions = plotTypeAndOptions.getOrDefault(projectionMethod, new ArrayList<>());
                        plotOptions.add(plotOption);
                        plotTypeAndOptions.put(projectionMethod, plotOptions);
                    }
                    return plotTypeAndOptions;
                });
    }
}
