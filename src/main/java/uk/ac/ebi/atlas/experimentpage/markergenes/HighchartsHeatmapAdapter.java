package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.bioentity.properties.BioEntityPropertyDao;

import java.util.Collection;
import java.util.function.Function;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Comparator.comparing;

@Component
public class HighchartsHeatmapAdapter {
    private static final Function<MarkerGene, Pair<String, String>> MARKER_GENE_ID_TO_CELL_GROUP_VALUE_WHERE_MARKER =
            markerGene -> Pair.of(markerGene.geneId(), markerGene.cellGroupValueWhereMarker());

    private final BioEntityPropertyDao bioEntityPropertyDao;

    public HighchartsHeatmapAdapter(BioEntityPropertyDao bioEntityPropertyDao) {
        this.bioEntityPropertyDao = bioEntityPropertyDao;
    }

    /**
     * Given a list of marker genes, this method returns a Highcharts data series object
     * (https://api.highcharts.com/highcharts/series.heatmap.data), where gene IDs/symbols are
     * the rows (y values), and the cell types are the columns (x values).
     * The cells contain the median average expression of the gene in the cell group.
     * The rows of the heatmap are ordered by the cell type, i.e. genes for celltype 1, 2, etc.
     * If there are no marker genes for a cell group, then no rows will be present in the data.
     */
    public ImmutableList<ImmutableMap<String, Object>> getMarkerGeneHeatmapData(Collection<MarkerGene> markerGenes) {
        // Whether the comparison by p-value should or shouldn’t be reversed depends on the yAxis.reversed property in
        // the heatmap component. In our case it’s set to true, so lower p-value is displayed at the top without
        // reversing the comparator
        var sortedMarkerGenes = markerGenes.stream()
                .parallel()
                .sorted(comparing(MarkerGene::cellGroupValueWhereMarker).thenComparing(MarkerGene::pValue))
                .collect(toImmutableList());

        var rows =
                sortedMarkerGenes.stream()
                        .map(MARKER_GENE_ID_TO_CELL_GROUP_VALUE_WHERE_MARKER)
                        .distinct()
                        .collect(toImmutableList());
        var columns =
                sortedMarkerGenes.stream()
                        .map(MarkerGene::cellGroupValue)
                        .distinct()
                        .collect(toImmutableList());

        var symbolsForGeneIds =
                bioEntityPropertyDao.getSymbolsForGeneIds(
                        sortedMarkerGenes.stream().map(MarkerGene::geneId).collect(toImmutableSet()));

        return sortedMarkerGenes.stream()
                .map(markerGene ->
                        ImmutableMap.<String, Object>builder()
                                // To get x co ordinates- extract all distinct cell types as a List(columns in our case)
                                // and get index of each cell type
                                .put("x", columns.indexOf(markerGene.cellGroupValue()))
                                .put("y", rows.indexOf(MARKER_GENE_ID_TO_CELL_GROUP_VALUE_WHERE_MARKER.apply(markerGene)))
                                .put("geneName", symbolsForGeneIds.getOrDefault(markerGene.geneId(), markerGene.geneId()))
                                .put("value", markerGene.medianExpression())
                                .put("cellGroupValue", markerGene.cellGroupValue())
                                .put("cellGroupValueWhereMarker", markerGene.cellGroupValueWhereMarker())
                                .put("pValue", markerGene.pValue())
                                .build())
                .collect(toImmutableList());
    }
}
