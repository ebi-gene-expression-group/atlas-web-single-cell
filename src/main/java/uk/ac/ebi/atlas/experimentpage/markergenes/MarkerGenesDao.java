package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MarkerGenesDao {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private static final String CELL_GROUP_TYPE = "inferred cell type";

    public MarkerGenesDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    private static final String SELECT_MARKER_GENES_WITH_AVERAGES_PER_CLUSTER =
            "SELECT g.experiment_accession, m.gene_id, " +
                    "g.variable as k_where_marker, h.value as cluster_id_where_marker, " +
                    "g.value as cluster_id, m.marker_probability as marker_p_value, " +
                    "s.mean_expression, s.median_expression " +
                    "FROM " +
                    "scxa_cell_group_marker_gene_stats s, scxa_cell_group_marker_genes m, " +
                    "scxa_cell_group g, scxa_cell_group h " +
                    "WHERE s.cell_group_id = g.id and s.marker_id = m.id and " +
                    "m.cell_group_id = h.id and g.experiment_accession = :experiment_accession and " +
                    "m.marker_probability < 0.05 and g.variable = :k and " +
                    "expression_type = 0 order by m.marker_probability ";

    public List<MarkerGene> getMarkerGenesWithAveragesPerCluster(String experimentAccession, String k) {
        var namedParameters =
                ImmutableMap.of(
                        "experiment_accession", experimentAccession,
                        "k", k);

        return namedParameterJdbcTemplate.query(
                SELECT_MARKER_GENES_WITH_AVERAGES_PER_CLUSTER,
                namedParameters,
                (resultSet, rowNumber) -> MarkerGene.create(
                        resultSet.getString("gene_id"),
                        resultSet.getString("k_where_marker"),
                        resultSet.getString("cluster_id_where_marker"),
                        resultSet.getDouble("marker_p_value"),
                        resultSet.getString("cluster_id"),
                        resultSet.getDouble("median_expression"),
                        resultSet.getDouble("mean_expression")));
    }

    private static final String SELECT_DISTINCT_KS_WITH_MARKER_GENES =
            "SELECT DISTINCT h.variable as k_where_marker " +
                    "FROM scxa_cell_group_marker_genes m, scxa_cell_group h " +
                    "WHERE m.cell_group_id = h.id AND " +
                    "h.experiment_accession = :experiment_accession AND m.marker_probability < 0.05 " +
                    "ORDER BY k_where_marker ASC";

    public List<String> getKsWithMarkerGenes(String experimentAccession) {
        var namedParameters = ImmutableMap.of("experiment_accession", experimentAccession);
        return namedParameterJdbcTemplate.queryForList(
                SELECT_DISTINCT_KS_WITH_MARKER_GENES,
                namedParameters,
                String.class);
    }

    private static final String SELECT_MARKER_GENES_WITH_AVERAGES_PER_CELL_GROUP =
            "SELECT " +
                    "g.experiment_accession, m.gene_id, g.variable as cell_group_type, " +
                    "h.value as cell_group_value_where_marker, g.value as cell_group_value, " +
                    "m.marker_probability as marker_p_value, s.mean_expression, s.median_expression " +
                    "FROM " +
                    "scxa_cell_group_marker_gene_stats s, scxa_cell_group_marker_genes m, " +
                    "scxa_cell_group g, scxa_cell_group h " +
                    "WHERE " +
                    "s.cell_group_id = g.id and s.marker_id = m.id and " +
                    "m.cell_group_id = h.id and g.experiment_accession = :experiment_accession and " +
                    "m.marker_probability < 0.05 and g.variable = :variable and " +
                    "g.value IN (:values) and expression_type = 0 order by m.marker_probability ";

	public List<MarkerGene> getCellTypeMarkerGenes(String experiment_accession, ImmutableSet<String> cellGroupValues) {

		var namedParameters =
				ImmutableMap.of(
						"experiment_accession", experiment_accession,
						"variable", CELL_GROUP_TYPE,
						"values", cellGroupValues.isEmpty() ? "" : cellGroupValues);

		return namedParameterJdbcTemplate.query(
				SELECT_MARKER_GENES_WITH_AVERAGES_PER_CELL_GROUP,
				namedParameters,
				(resultSet, rowNumber) -> MarkerGene.create(
						resultSet.getString("gene_id"),
						resultSet.getString("cell_group_type"),
						resultSet.getString("cell_group_value_where_marker"),
						resultSet.getDouble("marker_p_value"),
						resultSet.getString("cell_group_value"),
						resultSet.getDouble("median_expression"),
						resultSet.getDouble("mean_expression")));
	}
}
