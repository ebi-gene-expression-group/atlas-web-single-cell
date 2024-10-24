package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MarkerGenesDao {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    static final String CELL_TYPE_ONTOLOGY_LABELS = "inferred cell type - ontology labels";
    static final String CELL_TYPE_AUTHOR_LABELS = "inferred cell type - authors labels";

    public MarkerGenesDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    private static final String SELECT_MARKER_GENES_WITH_AVERAGES_PER_CLUSTER =
            "SELECT " +
                    "g.experiment_accession, " +
                    "m.gene_id, " +
                    "m.marker_probability AS marker_p_value, " +
                    "g.variable AS k_where_marker, " +
                    "g.value AS cluster_id, " +
                    "h.value AS cluster_id_where_marker, " +
                    "s.mean_expression, " +
                    "s.median_expression, " +
                    "e.expression_unit " +
            "FROM " +
                    "scxa_cell_group_marker_gene_stats s, " +
                    "scxa_cell_group_marker_genes m, " +
                    "scxa_cell_group g, " +
                    "scxa_cell_group h, " +
                    "experiment e " +
            "WHERE " +
                    "s.cell_group_id = g.id AND " +
                    "s.marker_id = m.id AND " +
                    "m.cell_group_id = h.id AND " +
                    "g.experiment_accession = :experiment_accession AND " +
                    "m.marker_probability < 0.05 AND " +
                    "g.variable = :k AND " +
                    "s.expression_type = 0 AND " +
                    "e.accession = g.experiment_accession " +
            "ORDER BY " +
                    "m.marker_probability";

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
                        resultSet.getDouble("mean_expression"),
                        resultSet.getString("expression_unit")));
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
                    "g.variable AS cell_group_type, " +
                    "h.value AS cell_group_value_where_marker, " +
                    "g.value AS cell_group_value, " +
                    "m.marker_probability AS marker_p_value, " +
                    "s.mean_expression, " +
                    "s.median_expression, " +
                    "e.expression_unit " +
            "FROM " +
                    "scxa_cell_group_marker_gene_stats s, " +
                    "scxa_cell_group_marker_genes m, " +
                    "scxa_cell_group g, " +
                    "scxa_cell_group h, " +
                    "experiment e " +
            "WHERE " +
                    "s.cell_group_id = g.id AND " +
                    "s.marker_id = m.id AND " +
                    "m.cell_group_id = h.id AND " +
                    "g.experiment_accession = :experiment_accession AND " +
                    "m.marker_probability < 0.05 AND " +
                    "g.variable = :variable AND " +
                    "g.value IN (:values) AND " +
                    "s.expression_type = 0 AND " +
                    "e.accession = g.experiment_accession " +
            "ORDER BY " +
                    "m.marker_probability ";

    public List<MarkerGene> getCellTypeMarkerGenesOntologyLabels(String experimentAccession,
                                                                 ImmutableCollection<String> cellGroupValues) {
        return getCellTypeMarkerGenes(experimentAccession, CELL_TYPE_ONTOLOGY_LABELS, cellGroupValues);
    }

    public List<MarkerGene> getCellTypeMarkerGenesAuthorsLabels(String experimentAccession,
                                                                ImmutableCollection<String> cellGroupValues) {
        return getCellTypeMarkerGenes(experimentAccession, CELL_TYPE_AUTHOR_LABELS, cellGroupValues);
    }

    public List<MarkerGene> getCellTypeMarkerGenes(String experiment_accession,
                                                   String cellTypeVariable,
                                                   ImmutableCollection<String> cellGroupValues) {
        if (cellGroupValues.isEmpty()) {
            return getCellTypeMarkerGenes(experiment_accession, cellTypeVariable);
        }

        var namedParameters =
                ImmutableMap.of(
                        "experiment_accession", experiment_accession,
                        "variable", cellTypeVariable,
                        "values", cellGroupValues);

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
                        resultSet.getDouble("mean_expression"),
                        resultSet.getString("expression_unit")));
    }

    private static final String SELECT_MARKER_GENES_WITH_AVERAGES_PER_CELL_GROUP_ALL =
            "SELECT " +
                    "g.experiment_accession, " +
                    "m.gene_id, " +
                    "g.variable AS cell_group_type, " +
                    "h.value AS cell_group_value_where_marker, " +
                    "g.value AS cell_group_value, " +
                    "m.marker_probability AS marker_p_value, " +
                    "s.mean_expression, " +
                    "s.median_expression, " +
                    "e.expression_unit " +
            "FROM " +
                    "scxa_cell_group_marker_gene_stats s, " +
                    "scxa_cell_group_marker_genes m, " +
                    "scxa_cell_group g, " +
                    "scxa_cell_group h, " +
                    "experiment e " +
            "WHERE " +
                    "s.cell_group_id = g.id AND " +
                    "s.marker_id = m.id AND " +
                    "m.cell_group_id = h.id AND " +
                    "g.experiment_accession = :experiment_accession AND " +
                    "m.marker_probability < 0.05 AND " +
                    "g.variable = :variable AND " +
                    "s.expression_type = 0 AND " +
                    "e.accession = g.experiment_accession " +
            "ORDER BY " +
                    "m.marker_probability ";
    public List<MarkerGene> getCellTypeMarkerGenes(String experiment_accession,
                                                   String cellTypeVariable) {
        var namedParameters =
                ImmutableMap.of(
                        "experiment_accession", experiment_accession,
                        "variable", cellTypeVariable);

        return namedParameterJdbcTemplate.query(
                SELECT_MARKER_GENES_WITH_AVERAGES_PER_CELL_GROUP_ALL,
                namedParameters,
                (resultSet, rowNumber) -> MarkerGene.create(
                        resultSet.getString("gene_id"),
                        resultSet.getString("cell_group_type"),
                        resultSet.getString("cell_group_value_where_marker"),
                        resultSet.getDouble("marker_p_value"),
                        resultSet.getString("cell_group_value"),
                        resultSet.getDouble("median_expression"),
                        resultSet.getDouble("mean_expression"),
                        resultSet.getString("expression_unit")));
    }
}
