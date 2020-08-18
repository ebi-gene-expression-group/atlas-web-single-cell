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
     * Get List of marker genes objects for an experiment of that organism part.
     * @param organismPart
     * @param experimentAccession
     * @return List of CellTypeMarkerGene Objects
     */
    @Override
    public ImmutableList<CellTypeMarkerGene> getCellTypeMarkerGenes(String experimentAccession, String organismPart) {
        List<CellTypeMarkerGene> cellTypeMarkerGenes = markerGenesDao.getCellTypeMarkerGenes(experimentAccession, organismPart);
        return ImmutableList.copyOf(cellTypeMarkerGenes);
    }
}
