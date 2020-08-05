package uk.ac.ebi.atlas.experimentpage.markergenes;

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
     *
     * @param organismPart
     * @param experimentAccession
     * @return List of CellTypeMarkerGene Objects
     */
    @Override
    public List<CellTypeMarkerGene> getMarkerGenes(String experimentAccession, String organismPart) {
        List<CellTypeMarkerGene> cellTypeMarkerGenes = markerGenesDao.getMarkerGenes(experimentAccession, organismPart);
        System.out.println("output: " + cellTypeMarkerGenes.toString());
        return cellTypeMarkerGenes;
    }
}
