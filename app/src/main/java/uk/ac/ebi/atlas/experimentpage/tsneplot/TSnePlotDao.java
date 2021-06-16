package uk.ac.ebi.atlas.experimentpage.tsneplot;

import com.google.common.collect.ImmutableMap;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.atlas.experimentpage.tsne.TSnePoint;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class TSnePlotDao {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final String TSNE_METHOD = "tsne";

    public TSnePlotDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    private static final String SELECT_T_SNE_PLOT_WITH_EXPRESSION_STATEMENT =
            "SELECT c.cell_id, c.x, c.y, a.expression_level " +
                    "FROM scxa_coords c LEFT JOIN scxa_analytics a ON " +
                    "c.experiment_accession=a.experiment_accession AND " +
                    "c.cell_id=a.cell_id AND a.gene_id=:gene_id " +
                    "WHERE c.experiment_accession=:experiment_accession AND " +
                    "c.method=:method AND c.parameterisation->0->> :parameter_name =:parameter " +
                    "ORDER BY c.cell_id";

    @Transactional(transactionManager = "txManager", readOnly = true)
    public List<TSnePoint.Dto> fetchTSnePlotWithExpression(String experimentAccession, String method, int parameter, String geneId) {
        var namedParameters =
                ImmutableMap.of(
                        "experiment_accession", experimentAccession,
                        "parameter", String.valueOf(parameter),
                        "method", method,
                        "parameter_name", method == TSNE_METHOD ? "perplexity" : "n_neighbors",
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
            "SELECT c.cell_id,c.x,c.y, g.value as cluster_id " +
                    "FROM scxa_cell_group g " +
                    "JOIN scxa_cell_group_membership m ON " +
                    "g.id = m.cell_group_id AND " +
                    "g.experiment_accession = :experiment_accession AND " +
                    "g.variable = :variable " +
                    "RIGHT JOIN scxa_coords c ON " +
                    "m.cell_id=c.cell_id AND " +
                    "m.experiment_accession=c.experiment_accession " +
                    "WHERE c.method=:method AND c.parameterisation->0->>:parameter_name=:parameter " +
                    "AND c.experiment_accession=:experiment_accession";

    @Transactional(transactionManager = "txManager", readOnly = true)
    public List<TSnePoint.Dto> fetchTSnePlotWithClusters(String experimentAccession, String method, int parameter, String variable) {
        var namedParameters =
                ImmutableMap.of(
                        "experiment_accession", experimentAccession,
                        "parameter", String.valueOf(parameter),
                        "parameter_name", method == TSNE_METHOD ? "perplexity" : "n_neighbors",
                        "method", method,
                        "variable", variable);

        return namedParameterJdbcTemplate.query(
                SELECT_T_SNE_PLOT_WITH_CLUSTERS_STATEMENT,
                namedParameters,
                (rs, rowNum) -> TSnePoint.Dto.create(
                        rs.getDouble("x"), rs.getDouble("y"),
                        rs.getString("cluster_id"), rs.getString("cell_id")));
    }

    private static final String SELECT_T_SNE_PLOT_WITHOUT_CLUSTERS_STATEMENT =
            "SELECT coords.cell_id, coords.x, coords.y " +
                    "FROM scxa_coords AS coords " +
                    "WHERE coords.experiment_accession=:experiment_accession AND coords.method=:method" +
                    "AND coords.parameterisation->0->>'perplexity'= :perplexity";

    public List<TSnePoint.Dto> fetchTSnePlotForPerplexity(String experimentAccession, int perplexity) {
        var namedParameters =
                ImmutableMap.of(
                        "experiment_accession", experimentAccession,
                        "method", TSNE_METHOD,
                        "perplexity", String.valueOf(perplexity));

        return namedParameterJdbcTemplate.query(
                SELECT_T_SNE_PLOT_WITHOUT_CLUSTERS_STATEMENT,
                namedParameters,
                (rs, rowNum) -> TSnePoint.Dto.create(
                        rs.getDouble("x"), rs.getDouble("y"), rs.getString("cell_id")));
    }

    private static final String SELECT_DISTINCT_PERPLEXITIES_STATEMENT =
            "SELECT DISTINCT value " +
                    "FROM scxa_coords, lateral jsonb_each(parameterisation->0) " +
                    "WHERE experiment_accession=:experiment_accession AND method=:method";

    public List<Integer> fetchPerplexities(String experimentAccession) {
        var namedParameters = ImmutableMap.of(
                "experiment_accession", experimentAccession,
                "method", TSNE_METHOD);

        return namedParameterJdbcTemplate.queryForList(
                SELECT_DISTINCT_PERPLEXITIES_STATEMENT,
                namedParameters,
                String.class)
                .stream()
                .map(perplexity -> Integer.valueOf(perplexity))
                .collect(Collectors.toList());
    }

    private static final String COUNT_CELLS_BY_EXPERIMENT_ACCESSION =
            "SELECT COUNT(DISTINCT(cell_id)) FROM scxa_coords WHERE experiment_accession=:experiment_accession";

    public Integer fetchNumberOfCellsByExperimentAccession(String experimentAccession) {
        var namedParameters = ImmutableMap.of("experiment_accession", experimentAccession);

        return namedParameterJdbcTemplate.queryForObject(
                COUNT_CELLS_BY_EXPERIMENT_ACCESSION,
                namedParameters,
                Integer.class);
    }
}