package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.bioentity.properties.BioEntityPropertyDao;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.groupingBy;

@Component
public class HighchartsHeatmapAdapter {
    private static final Function<MarkerGene, Pair<String, String>> MARKER_GENE_ID_TO_CELL_GROUP_VALUE_WHERE_MARKER =
            markerGene -> Pair.of(markerGene.geneId(), markerGene.cellGroupValueWhereMarker());

    private static final Comparator<MarkerGene> CELL_GROUP_VALUE_WHERE_MARKER_LEXICOGRAPHICAL =
            comparing(MarkerGene::cellGroupValueWhereMarker).thenComparing(MarkerGene::pValue);

    private static final ToIntFunction<MarkerGene> CELL_GROUP_VALUE_WHERE_MARKER_AS_NUMBERS =
            markerGene -> Integer.parseInt(markerGene.cellGroupValueWhereMarker());
    private static final Comparator<MarkerGene> CELL_GROUP_VALUE_WHERE_MARKER_NUMERICAL =
            comparingInt(CELL_GROUP_VALUE_WHERE_MARKER_AS_NUMBERS).thenComparing(MarkerGene::pValue);

    private static final Comparator<MarkerGene> MARKER_NATURALLY_ORDERED_BY_CELL_GROUP_VALUE =
            new MarkerGeneComparatorByCellGroupValueByMarker();

    private static final Comparator<MarkerGene> MARKER_GENE_COMPARATOR =
            MARKER_NATURALLY_ORDERED_BY_CELL_GROUP_VALUE.thenComparing(MarkerGene::pValue);

    private final BioEntityPropertyDao bioEntityPropertyDao;

    public HighchartsHeatmapAdapter(BioEntityPropertyDao bioEntityPropertyDao) {
        this.bioEntityPropertyDao = bioEntityPropertyDao;
    }

    public ImmutableList<ImmutableMap<String, Object>> getMarkerGeneHeatmapDataSortedNaturally
            (Collection<MarkerGene> markerGenes) {
        var sortedMarkerGenes = getSortedMarkerGenes(markerGenes, MARKER_GENE_COMPARATOR);

        var rows = getRowsFromSortedMarkerGenes(sortedMarkerGenes);

        var columns = getColumnsFromSortedMarkerGenes(sortedMarkerGenes);

        return getMarkerGeneHeatmapData(sortedMarkerGenes, rows, columns);
    }

    /**
     * Given a list of marker genes, this method returns a Highcharts data series object
     * (<a href="https://api.highcharts.com/highcharts/series.heatmap.data">heatmap data for series</a>), where gene IDs/symbols are
     * the rows (y values), and the cell types are the columns (x values).
     * The cells contain the median average expression of the gene in the cell group.
     * The rows of the heatmap are ordered by the cell type, i.e. genes for celltype 1, 2, etc.
     * If there are no marker genes for a cell group, then no rows will be present in the data.
     */
    public ImmutableList<ImmutableMap<String, Object>> getMarkerGeneHeatmapDataSortedLexicographically(
            Collection<MarkerGene> markerGenes) {
        // Whether the comparison by p-value should or shouldn’t be reversed depends on the yAxis.reversed property in
        // the heatmap component. In our case it’s set to true, so lower p-value is displayed at the top without
        // reversing the comparator
        var sortedMarkerGenes = getSortedMarkerGenes(markerGenes, CELL_GROUP_VALUE_WHERE_MARKER_LEXICOGRAPHICAL);

        var rows = getRowsFromSortedMarkerGenes(sortedMarkerGenes);

        var columns = getColumnsFromSortedMarkerGenes(sortedMarkerGenes);

        return getMarkerGeneHeatmapData(sortedMarkerGenes, rows, columns);
    }

    public ImmutableList<ImmutableMap<String, Object>> getMarkerGeneHeatmapDataSortedNumerically(
            Collection<MarkerGene> markerGenes) {
        // Whether the comparison by p-value should or shouldn’t be reversed depends on the yAxis.reversed property in
        // the heatmap component. In our case it’s set to true, so lower p-value is displayed at the top without
        // reversing the comparator
        var sortedMarkerGenes = getSortedMarkerGenes(markerGenes, CELL_GROUP_VALUE_WHERE_MARKER_NUMERICAL);

        var rows = getRowsFromSortedMarkerGenes(sortedMarkerGenes);

        var columns = getColumnsForNumericallySortedMarkerGenes(sortedMarkerGenes);

        return getMarkerGeneHeatmapData(sortedMarkerGenes, rows, columns);
    }

    private ImmutableList<MarkerGene> getSortedMarkerGenes(Collection<MarkerGene> markerGenes, Comparator<MarkerGene> markerGeneComparator) {
        return mergeSameGeneIdIntoSingleGroup(markerGenes).stream()
                .parallel()
                .sorted(markerGeneComparator)
                .collect(toImmutableList());
    }

    private static ImmutableList<Pair<String, String>> getRowsFromSortedMarkerGenes(ImmutableList<MarkerGene> sortedMarkerGenes) {
        return sortedMarkerGenes.stream()
                .map(MARKER_GENE_ID_TO_CELL_GROUP_VALUE_WHERE_MARKER)
                .distinct()
                .collect(toImmutableList());
    }

    private static ImmutableList<String> getColumnsFromSortedMarkerGenes(ImmutableList<MarkerGene> sortedMarkerGenes) {
        return sortedMarkerGenes.stream()
                .map(MarkerGene::cellGroupValue)
                .distinct()
                .sorted()
                .collect(toImmutableList());
    }

    private ImmutableList<String> getColumnsForNumericallySortedMarkerGenes(ImmutableList<MarkerGene> sortedMarkerGenes) {
        return sortedMarkerGenes.stream()
                .map(MarkerGene::cellGroupValue)
                .map(Integer::parseInt)
                .distinct()
                .sorted()
                .map(Object::toString)
                .collect(toImmutableList());
    }

    private ImmutableList<ImmutableMap<String, Object>> getMarkerGeneHeatmapData(ImmutableCollection<MarkerGene> sortedMarkerGenes,
                                                                                 ImmutableList<Pair<String, String>> rows,
                                                                                 ImmutableList<String> columns) {
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
                                .put("expressionUnit", markerGene.expressionUnit())
                                .build())
                .collect(toImmutableList());
    }

    // When the same marker gene occurs in different groups, we want to see it only once in the heatmap
    private ImmutableList<MarkerGene> mergeSameGeneIdIntoSingleGroup(Collection<MarkerGene> cellTypeMarkerGenes) {
        return cellTypeMarkerGenes.stream()
                .collect(groupingBy(MarkerGene::geneId))
                .values().stream()
                .flatMap(sameGeneIdMarkerGenesAcrossAllGroups -> {
                    // From all the occurrences of the same gene ID across groups, we choose the group in which it has
                    // the lowest p-value
                    var referenceMarkerGene =
                            sameGeneIdMarkerGenesAcrossAllGroups.stream()
                                    .min(Comparator.comparingDouble(MarkerGene::pValue))
                                    // This should never happen, every gene ID must have at least one entry
                                    .orElseThrow(IllegalArgumentException::new);
                    return sameGeneIdMarkerGenesAcrossAllGroups.stream()
                            .map(sameGeneIdMarkerGene ->
                                    MarkerGene.create(
                                            sameGeneIdMarkerGene.geneId(),
                                            sameGeneIdMarkerGene.cellGroupType(),
                                            referenceMarkerGene.cellGroupValueWhereMarker(),
                                            referenceMarkerGene.pValue(),
                                            sameGeneIdMarkerGene.cellGroupValue(),
                                            sameGeneIdMarkerGene.medianExpression(),
                                            sameGeneIdMarkerGene.meanExpression(),
                                            sameGeneIdMarkerGene.expressionUnit()));
                })
                .collect(toImmutableList());
    }
}
