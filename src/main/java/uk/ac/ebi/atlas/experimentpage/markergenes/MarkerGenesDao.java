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
                    "g.experiment_accession, " +
                    "m.gene_id, " +
                    "g.variable AS cell_group_type, " + //k_where_marker
                    "h.value AS cell_group_value_where_marker, " + //cluster_id_where_marker
                    "g.value AS cell_group_value, " +             //cluster_id
                    "m.marker_probability AS marker_p_value, " +
                    "s.mean_expression, " +
                    "s.median_expression " +
            "FROM " +
                    "scxa_cell_group_marker_gene_stats s, " +
                    "scxa_cell_group_marker_genes m, " +
                    "scxa_cell_group g, " +
                    "scxa_cell_group h " +
            "WHERE " +
                    "s.cell_group_id=g.id AND " +
                    "s.marker_id=m.id AND " +
                    "m.cell_group_id = h.id AND " +
                    "g.experiment_accession = :experiment_accession AND " +
                    "m.marker_probability < 0.05 AND " +
                    "g.variable = :variable AND " +
                    "g.value IN (:values) AND " +
                    "expression_type = 0 " +
            "ORDER BY " +
                    "m.marker_probability ";

    public List<CellTypeMarkerGene> getCellTypeMarkerGenes(String experiment_accession, ImmutableSet<String> cellGroupValues) {
        var namedParameters =
                ImmutableMap.of(
                        "experiment_accession", experiment_accession,
                        "variable", CELL_GROUP_TYPE,
                        "values", cellGroupValues.isEmpty()? "" : cellGroupValues);

        return namedParameterJdbcTemplate.query(
                SELECT_MARKER_GENES_WITH_AVERAGES_PER_CELL_GROUP,
                namedParameters,
                (resultSet, rowNumber) -> CellTypeMarkerGene.create(
                        resultSet.getString("gene_id"),
                        resultSet.getString("cell_group_type"),
                        resultSet.getString("cell_group_value_where_marker"),
                        resultSet.getDouble("marker_p_value"),
                        resultSet.getString("cell_group_value"),
                        resultSet.getDouble("median_expression"),
                        resultSet.getDouble("mean_expression")));
    }

    public List<CellTypeMarkerGene> getCellTypeMarkerGenes(String experiment_accession,
                                                           ImmutableCollection<String> cellTypes) {
        var namedParameters =
                ImmutableMap.of(
                        "experiment_accession", experiment_accession,
                        "variable", CELL_GROUP_TYPE,
                        "values", cellTypes);

        return namedParameterJdbcTemplate.query(
                SELECT_MARKER_GENES_WITH_AVERAGES_PER_CELL_GROUP,
                namedParameters,
                (resultSet, rowNumber) -> CellTypeMarkerGene.create(
                        resultSet.getString("gene_id"),
                        resultSet.getString("cell_group_type"),
                        resultSet.getString("cell_group_value_where_marker"),
                        resultSet.getDouble("marker_p_value"),
                        resultSet.getString("cell_group_value"),
                        resultSet.getDouble("median_expression"),
                        resultSet.getDouble("mean_expression")));
    }
}
