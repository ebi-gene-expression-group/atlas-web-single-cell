package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Service;
import java.util.List;
import static com.google.common.collect.ImmutableList.toImmutableList;

@Service
public class MarkerGeneService {
    private MarkerGenesDao markerGenesDao;

    public MarkerGeneService(MarkerGenesDao markerGenesDao) {
        this.markerGenesDao = markerGenesDao;
    }

    /**
     * Get ImmutableList of marker genes objects for an experiment of that organism part.
     * @param organismPart
     * @param experimentAccession
     * @return ImmutableList of CellTypeMarkerGene  Objects
     */
    public ImmutableList<CellTypeMarkerGene> getCellTypeMarkerGeneProfile(String experimentAccession, String organismPart) {
        List<CellTypeMarkerGene> cellTypeMarkerGenes = markerGenesDao.getCellTypeMarkerGenes(experimentAccession, organismPart);
        return cellTypeMarkerGenes.stream()
                .filter(markerGene -> !markerGene.cellGroupValue().equals("Not available"))
                //'cellTypeWhereMarker' attribute is not feasible to filter 'Not available' cells as it
                // contains descriptive value ,so selected 'cellType' attribute to filter 'Not available' cells.
                .collect(toImmutableList());
    }
}
