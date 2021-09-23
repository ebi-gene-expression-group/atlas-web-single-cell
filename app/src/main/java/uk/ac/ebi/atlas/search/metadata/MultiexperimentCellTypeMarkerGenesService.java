package uk.ac.ebi.atlas.search.metadata;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.experimentpage.markergenes.MarkerGene;

import static com.google.common.collect.ImmutableList.toImmutableList;

@Service
public class MultiexperimentCellTypeMarkerGenesService {
    private final MultiexperimentCellTypeMarkerGenesDao multiexperimentCellTypeMarkerGenesDao;

    public MultiexperimentCellTypeMarkerGenesService(
            MultiexperimentCellTypeMarkerGenesDao multiexperimentCellTypeMarkerGenesDao) {
        this.multiexperimentCellTypeMarkerGenesDao = multiexperimentCellTypeMarkerGenesDao;
    }

    public ImmutableList<MarkerGene> getCellTypeMarkerGeneProfile(ImmutableCollection<String> experimentAccessions,
                                                                  String cellType) {
        var cellTypeMarkerGenes =
                multiexperimentCellTypeMarkerGenesDao.getCellTypeMarkerGenes(experimentAccessions, cellType);

        return cellTypeMarkerGenes.stream()
                .filter(markerGene -> !markerGene.cellGroupValue().equalsIgnoreCase("Not available"))
                .filter(markerGene -> !markerGene.cellGroupValueWhereMarker().equalsIgnoreCase("Not available"))
                .collect(toImmutableList());
    }

    public ImmutableList<MarkerGene> getCellTypeMarkerGeneProfile(String cellType) {
        var cellTypeMarkerGenes = multiexperimentCellTypeMarkerGenesDao.getCellTypeMarkerGenes(cellType);

        return cellTypeMarkerGenes.stream()
                .filter(markerGene -> !markerGene.cellGroupValue().equalsIgnoreCase("Not available"))
                .filter(markerGene -> !markerGene.cellGroupValueWhereMarker().equalsIgnoreCase("Not available"))
                .collect(toImmutableList());
    }
}
