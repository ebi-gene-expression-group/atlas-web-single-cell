package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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

    private static final String SELECT_MARKER_GENES_WITH_AVERAGES_PER_CELL_TYPE =
            "SELECT " +
                    "g.experiment_accession, " +
                    "m.gene_id, " +
                    "g.variable as k_where_marker, " +
                    "h.value as cluster_id_where_marker, " +
                    "g.value as cluster_id, " +
                    "m.marker_probability as marker_p_value, " +
                    "s.mean_expression, s.median_expression " +
                    "FROM " +
                    "scxa_cell_group_marker_gene_stats s, " +
                    "scxa_cell_group_marker_genes m, " +
                    "scxa_cell_group g, " +
                    "scxa_cell_group h " +
                    "WHERE " +
                    "s.cell_group_id=g.id and " +
                    "s.marker_id=m.id and " +
                    "m.cell_group_id = h.id and g.experiment_accession= :experiment_accession and " +
                    "m.marker_probability < 0.05 and " +
                    "g.variable = :variable and " +
                    "g.value IN (:values) and " +
                    "expression_type=0 order by m.marker_probability ";

    public List<CellTypeMarkerGene> getMarkerGenes(String experiment_accession, String organismPart) {
        String cellType = "inferred cell type";

        //These temporary hardcoded celltypes(values) we will get from CellMetaDataDao class which is implemented by @Lingyun
        // We will call this DAO class by passing two inputs: experiment_accession(Param1) and organismPart(Param2)
        //We would get return type as ImmutableSet<String> celltypes(@return ImmutableSet<String> celltypes)
        ImmutableSet<String> cellTypeValues = ImmutableSet.of("T cell", "lymph node T cell", "not available", "tumor T cell");

        var namedParameters =
                ImmutableMap.of(
                        "experiment_accession", experiment_accession,
                        "variable", cellType,   //k_where_marker
                        "values", cellTypeValues); //cluster_id_where_marker

        return namedParameterJdbcTemplate.query(
                SELECT_MARKER_GENES_WITH_AVERAGES_PER_CELL_TYPE,
                namedParameters,
                (resultSet, rowNumber) -> CellTypeMarkerGene.create(
                        resultSet.getString("gene_id"),
                        resultSet.getString("k_where_marker"),
                        resultSet.getString("cluster_id_where_marker"),
                        resultSet.getDouble("marker_p_value"),
                        resultSet.getString("cluster_id"),
                        resultSet.getDouble("median_expression"),
                        resultSet.getDouble("mean_expression")));
    }
}
