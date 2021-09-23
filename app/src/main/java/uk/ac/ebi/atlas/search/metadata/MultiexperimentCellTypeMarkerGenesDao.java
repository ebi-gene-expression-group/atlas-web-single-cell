package uk.ac.ebi.atlas.search.metadata;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.atlas.experimentpage.markergenes.MarkerGene;

import java.util.List;

@Repository
public class MultiexperimentCellTypeMarkerGenesDao {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public MultiexperimentCellTypeMarkerGenesDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

	private static final String SELECT_MARKER_GENES_WITH_AVERAGES_PER_CELL_TYPE_ALL_EXPS =
			"SELECT " +
					"g.experiment_accession, " +
					"m.gene_id, " +
					"g.variable AS cell_group_type, " +
					"h.value AS cell_group_value_where_marker, " +
					"g.value AS cell_group_value," +
					"m.marker_probability AS marker_p_value, " +
					"s.mean_expression, " +
					"s.median_expression " +
					"FROM " +
					"scxa_cell_group_marker_gene_stats s, " +
					"scxa_cell_group_marker_genes m, " +
					"scxa_cell_group g, " +
					"scxa_cell_group h " +
					"WHERE " +
					"s.cell_group_id = g.id AND " +
					"s.marker_id = m.id AND " +
					"m.cell_group_id = h.id AND " +
					"m.marker_probability < 0.05 AND " +
					"h.value = :cell_type AND " +
					"g.value = :cell_type AND " +
					"expression_type = 0 " +
					"ORDER BY " +
					"experiment_accession, " +
					"marker_p_value";

    private static final String SELECT_MARKER_GENES_WITH_AVERAGES_PER_CELL_TYPE =
            "SELECT " +
					"g.experiment_accession, " +
					"m.gene_id, " +
					"g.variable AS cell_group_type, " +
					"h.value AS cell_group_value_where_marker, " +
					"g.value AS cell_group_value," +
					"m.marker_probability AS marker_p_value, " +
					"s.mean_expression, " +
					"s.median_expression " +
			"FROM " +
					"scxa_cell_group_marker_gene_stats s, " +
					"scxa_cell_group_marker_genes m, " +
					"scxa_cell_group g, " +
					"scxa_cell_group h " +
			"WHERE " +
					"s.cell_group_id = g.id AND " +
					"s.marker_id = m.id AND " +
					"m.cell_group_id = h.id AND " +
					"g.experiment_accession IN (:experiment_accessions) AND " +
					"m.marker_probability < 0.05 AND " +
					"h.value = :cell_type AND " +
					"g.value = :cell_type AND " +
					"expression_type = 0 " +
			"ORDER BY " +
					"experiment_accession, " +
					"marker_p_value";

	public List<MarkerGene> getCellTypeMarkerGenes(ImmutableCollection<String> experimentAccessions, String cellType) {
		var namedParameters =
				ImmutableMap.of(
						"experiment_accessions", experimentAccessions,
						"cell_type", cellType);

		return namedParameterJdbcTemplate.query(
				SELECT_MARKER_GENES_WITH_AVERAGES_PER_CELL_TYPE,
				namedParameters,
				(resultSet, rowNumber) -> MarkerGene.create(
						resultSet.getString("gene_id"),
						resultSet.getString("cell_group_type"),
						resultSet.getString("experiment_accession"),
						resultSet.getDouble("marker_p_value"),
						resultSet.getString("experiment_accession"),
						resultSet.getDouble("median_expression"),
						resultSet.getDouble("mean_expression")));
	}

	public List<MarkerGene> getCellTypeMarkerGenes(String cellType) {
		var namedParameters = ImmutableMap.of("cell_type", cellType);

		return namedParameterJdbcTemplate.query(
				SELECT_MARKER_GENES_WITH_AVERAGES_PER_CELL_TYPE_ALL_EXPS,
				namedParameters,
				(resultSet, rowNumber) -> MarkerGene.create(
						resultSet.getString("gene_id"),
						resultSet.getString("cell_group_type"),
						resultSet.getString("experiment_accession"),
						resultSet.getDouble("marker_p_value"),
						resultSet.getString("experiment_accession"),
						resultSet.getDouble("median_expression"),
						resultSet.getDouble("mean_expression")));
	}
}
