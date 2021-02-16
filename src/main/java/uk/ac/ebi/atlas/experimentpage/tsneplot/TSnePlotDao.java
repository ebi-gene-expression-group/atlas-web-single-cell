package uk.ac.ebi.atlas.experimentpage.tsneplot;

import com.google.common.collect.ImmutableMap;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
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
            "SELECT coords.cell_id, coords.x, coords.y, analytics.expression_level " +
                    "FROM scxa_coords AS coords " +
                    "LEFT JOIN " +
                    "(SELECT * FROM scxa_analytics WHERE gene_id=:gene_id and experiment_accession=:experiment_accession) AS analytics " +
                    "ON analytics.cell_id=coords.cell_id " +
                    "WHERE coords.experiment_accession=:experiment_accession AND coords.parameterisation=:perplexity";

    @Transactional(transactionManager = "txManager", readOnly = true)
    public List<TSnePoint.Dto> fetchTSnePlotWithExpression(String experimentAccession, int perplexity, String geneId) {
        var namedParameters =
                ImmutableMap.of(
                        "experiment_accession", experimentAccession,
                        "perplexity", "perplexity=" + perplexity,
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
			"SELECT coords.cell_id, coords.x, coords.y, clusters.cluster_id" +
					"            FROM scxa_coords AS coords" +
					"                LEFT JOIN" +
					"                (SELECT mem.*, g.value as cluster_id FROM scxa_cell_group_membership as mem JOIN  scxa_cell_group g" +
					"                    on mem.cell_group_id = g.id WHERE g.variable = :k AND mem.experiment_accession = :experiment_accession) AS clusters" +
					"                ON clusters.cell_id=tsne.cell_id " +
					"            WHERE coords.experiment_accession = :experiment_accession AND coords.perplexity = :perplexity";

    @Transactional(transactionManager = "txManager", readOnly = true)
    public List<TSnePoint.Dto> fetchTSnePlotWithClusters(String experimentAccession, int perplexity, int k) {
        var namedParameters =
                ImmutableMap.of(
                        "experiment_accession", experimentAccession,
                        "perplexity", "perplexity=" + perplexity,
                        "k", String.valueOf(k));

        return namedParameterJdbcTemplate.query(
                SELECT_T_SNE_PLOT_WITH_CLUSTERS_STATEMENT,
                namedParameters,
                (rs, rowNum) -> TSnePoint.Dto.create(
                        rs.getDouble("x"), rs.getDouble("y"), rs.getInt("cluster_id"), rs.getString("cell_id")));
    }

    private static final String SELECT_T_SNE_PLOT_WITHOUT_CLUSTERS_STATEMENT =
            "SELECT coords.cell_id, coords.x, coords.y " +
                    "FROM scxa_coords AS coords " +
                    "WHERE coords.experiment_accession=:experiment_accession AND coords.parameterisation=:perplexity";

    public List<TSnePoint.Dto> fetchTSnePlotForPerplexity(String experimentAccession, int perplexity) {
        var namedParameters =
                ImmutableMap.of(
                        "experiment_accession", experimentAccession,
                        "perplexity", "perplexity=" + perplexity);

        return namedParameterJdbcTemplate.query(
                SELECT_T_SNE_PLOT_WITHOUT_CLUSTERS_STATEMENT,
                namedParameters,
                (rs, rowNum) -> TSnePoint.Dto.create(
                        rs.getDouble("x"), rs.getDouble("y"), rs.getString("cell_id")));
    }

    private static final String SELECT_DISTINCT_PERPLEXITIES_STATEMENT =
            "SELECT DISTINCT parameterisation FROM scxa_coords AS coords " +
                    "WHERE coords.experiment_accession=:experiment_accession AND coords.method=:method";

    public List<Integer> fetchPerplexities(String experimentAccession) {
        var namedParameters = ImmutableMap.of(
                "experiment_accession", experimentAccession,
                "method", TSNE_METHOD);

        return namedParameterJdbcTemplate.queryForList(
                SELECT_DISTINCT_PERPLEXITIES_STATEMENT,
                namedParameters,
                String.class)
                .stream()
                .map(perplexity -> Integer.parseInt(perplexity.substring(11)))
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
