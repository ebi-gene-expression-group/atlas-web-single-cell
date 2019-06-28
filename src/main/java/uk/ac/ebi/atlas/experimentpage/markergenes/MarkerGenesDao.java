package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableMap;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MarkerGenesDao {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public MarkerGenesDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    private static final String SELECT_MARKER_GENES_WITH_AVERAGES_PER_CLUSTER =
            "SELECT * " +
            "FROM scxa_marker_gene_stats " +
            "WHERE k_where_marker = :k and experiment_accession = :experiment_accession AND marker_p_value < 0.05";
    public List<MarkerGene> getMarkerGenesWithAveragesPerCluster(String experimentAccession, int k) {
        var namedParameters =
                ImmutableMap.of(
                        "experiment_accession", experimentAccession,
                        "k", k);

        return namedParameterJdbcTemplate.query(
                SELECT_MARKER_GENES_WITH_AVERAGES_PER_CLUSTER,
                namedParameters,
                (resultSet, rowNumber) -> MarkerGene.create(
                            resultSet.getString("gene_id"),
                            resultSet.getInt("k_where_marker"),
                            resultSet.getInt("cluster_id_where_marker"),
                            resultSet.getDouble("marker_p_value"),
                            resultSet.getInt("cluster_id"),
                            resultSet.getDouble("median_expression"),
                            resultSet.getDouble("mean_expression")));
    }

    private static final String SELECT_DISTINCT_KS_WITH_MARKER_GENES =
            "SELECT DISTINCT k_where_marker " +
            "FROM scxa_marker_gene_stats " +
            "WHERE experiment_accession = :experiment_accession AND marker_p_value < 0.05 " +
            "ORDER BY k_where_marker ASC";
    public List<Integer> getKsWithMarkerGenes(String experimentAccession) {
        var namedParameters = ImmutableMap.of("experiment_accession", experimentAccession);

        return namedParameterJdbcTemplate.queryForList(
                SELECT_DISTINCT_KS_WITH_MARKER_GENES,
                namedParameters,
                Integer.class);
    }
}
