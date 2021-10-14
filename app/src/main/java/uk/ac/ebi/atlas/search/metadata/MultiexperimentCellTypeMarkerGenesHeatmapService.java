package uk.ac.ebi.atlas.search.metadata;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.experimentpage.markergenes.MarkerGene;

import static com.google.common.collect.ImmutableList.toImmutableList;

@Service
public class MultiexperimentCellTypeMarkerGenesHeatmapService {
    private final MultiexperimentCellTypeMarkerGenesHeatmapDao multiexperimentCellTypeMarkerGenesHeatmapDao;

    public MultiexperimentCellTypeMarkerGenesHeatmapService(
            MultiexperimentCellTypeMarkerGenesHeatmapDao multiexperimentCellTypeMarkerGenesHeatmapDao) {
        this.multiexperimentCellTypeMarkerGenesHeatmapDao = multiexperimentCellTypeMarkerGenesHeatmapDao;
    }

    public ImmutableList<MarkerGene> getCellTypeMarkerGeneProfile(ImmutableCollection<String> experimentAccessions,
                                                                  String cellType) {
        var cellTypeMarkerGenes =
                multiexperimentCellTypeMarkerGenesHeatmapDao.getCellTypeMarkerGenes(experimentAccessions, cellType);

        return cellTypeMarkerGenes.stream()
                .filter(markerGene -> !markerGene.cellGroupValue().equalsIgnoreCase("Not available"))
                .filter(markerGene -> !markerGene.cellGroupValueWhereMarker().equalsIgnoreCase("Not available"))
                //.collect(groupingBy(MarkerGene::geneId))
                .collect(toImmutableList());
    }

    public ImmutableList<MarkerGene> getCellTypeMarkerGeneProfile(String cellType) {
        var cellTypeMarkerGenes = multiexperimentCellTypeMarkerGenesHeatmapDao.getCellTypeMarkerGenes(cellType);

        return cellTypeMarkerGenes.stream()
                .filter(markerGene -> !markerGene.cellGroupValue().equalsIgnoreCase("Not available"))
                .filter(markerGene -> !markerGene.cellGroupValueWhereMarker().equalsIgnoreCase("Not available"))
                .collect(toImmutableList());
    }
}
