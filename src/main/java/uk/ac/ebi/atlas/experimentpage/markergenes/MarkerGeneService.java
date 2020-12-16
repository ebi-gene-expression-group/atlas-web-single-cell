package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.search.CellTypeSearchDao;

import static com.google.common.collect.ImmutableList.toImmutableList;

@Service
public class MarkerGeneService {
    private final MarkerGenesDao markerGenesDao;
    private final CellTypeSearchDao cellTypeSearchDao;

    public MarkerGeneService(MarkerGenesDao markerGenesDao, CellTypeSearchDao cellTypeSearchDao) {
        this.markerGenesDao = markerGenesDao;
        this.cellTypeSearchDao = cellTypeSearchDao;
    }

    /**
     * Get ImmutableList of marker genes objects for an experiment of that organism part.
     *
     * @param organismPart
     * @param experimentAccession
     * @return ImmutableList of CellTypeMarkerGene Objects
     * @return ImmutableList<MarkerGene> of cell type marker genes
     * Objects as a value
     */
    public ImmutableList<CellTypeMarkerGene> getCellTypeMarkerGeneProfile(String experimentAccession, String organismPart) {

        var ontologyLabelsCellTypeValues = cellTypeSearchDao.getInferredCellTypeOntologyLabels(experimentAccession, organismPart);

        var cellTypeMarkerGenes = ontologyLabelsCellTypeValues.isEmpty() ?
                markerGenesDao.getCellTypeMarkerGenes(experimentAccession,
                        cellTypeSearchDao.getInferredCellTypeAuthorsLabels(experimentAccession, organismPart)) :
                markerGenesDao.getCellTypeMarkerGenes(experimentAccession, ontologyLabelsCellTypeValues);

        return cellTypeMarkerGenes.stream()
                .filter(markerGene -> !markerGene.cellGroupValue().equalsIgnoreCase("Not available"))
                .collect(toImmutableList());
    }
    ImmutableList<MarkerGene> getCellTypeMarkerGeneProfile(String experimentAccession, String organismPart);

    /**
     * @param experimentAccession - Id of the experiment
     * @param k                   - no of clusters
     * @return ImmutableList<MarkerGene> of marker genes
     * Objects as a value
     */
    ImmutableList<MarkerGene> getMarkerGenesPerCluster(String experimentAccession, String k);
}
