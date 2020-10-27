package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Service;
import java.util.List;


@Service
public class MarkerGeneServiceImpl implements MarkerGeneService {
    private MarkerGenesDao markerGenesDao;

    public MarkerGeneServiceImpl(MarkerGenesDao markerGenesDao) {
        this.markerGenesDao = markerGenesDao;
    }

    /**
     * Get ImmutableList of marker genes objects for an experiment of that organism part.
     * @param organismPart
     * @param experimentAccession
     * @return ImmutableList of CellTypeMarkerGene Objects
     */
    @Override
    public ImmutableList<MarkerGene> getCellTypeMarkerGeneProfile(String experimentAccession, String organismPart) {
        List<MarkerGene> cellTypeMarkerGenes = markerGenesDao.getCellTypeMarkerGenes(experimentAccession, organismPart);
        return ImmutableList.copyOf(cellTypeMarkerGenes);
    }
}
