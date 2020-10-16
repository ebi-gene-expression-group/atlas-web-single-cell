package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.search.CellTypeSearchDao;

@Service
public class MarkerGeneServiceImpl implements MarkerGeneService {
    private final MarkerGenesDao markerGenesDao;
    private final CellTypeSearchDao cellTypeSearchDao;

    public MarkerGeneServiceImpl(MarkerGenesDao markerGenesDao,
                                 CellTypeSearchDao cellTypeSearchDao) {
        this.markerGenesDao = markerGenesDao;
        this.cellTypeSearchDao = cellTypeSearchDao;
    }

    /**
     * Get ImmutableList of marker genes objects for an experiment of that organism part.
     * @param organismPart
     * @param experimentAccession
     * @return ImmutableList of CellTypeMarkerGene Objects
     */
    @Override
    public ImmutableList<CellTypeMarkerGene> getCellTypeMarkerGeneProfile(String experimentAccession, String organismPart) {
        var cellTypes = cellTypeSearchDao.getInferredCellTypeOntologyLabels(experimentAccession, organismPart);

        if (cellTypes.isEmpty()) {
            return ImmutableList.of();
        }

        return ImmutableList.copyOf(markerGenesDao.getCellTypeMarkerGenes(experimentAccession, cellTypes));
    }
}
