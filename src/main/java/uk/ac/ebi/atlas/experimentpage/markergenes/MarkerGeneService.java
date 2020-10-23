package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.search.CellTypeSearchDao;
import java.util.List;
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
     * @param organismPart
     * @param experimentAccession
     * @return ImmutableList of CellTypeMarkerGene Objects
     */
    public ImmutableList<CellTypeMarkerGene> getCellTypeMarkerGeneProfile(String experimentAccession, String organismPart) {
        List<CellTypeMarkerGene> cellTypeMarkerGenes = null;
        ImmutableSet<String> ontologyLabelsCellTypeValues = cellTypeSearchDao.getInferredCellTypeOntologyLabels(experimentAccession, organismPart);
        if (ontologyLabelsCellTypeValues.isEmpty()) {
            ImmutableSet<String> authorsLabelsCellTypeValues = cellTypeSearchDao.getInferredCellTypeAuthorsLabels(experimentAccession, organismPart);
            cellTypeMarkerGenes = markerGenesDao.getCellTypeMarkerGenes(experimentAccession, authorsLabelsCellTypeValues);
        }
        cellTypeMarkerGenes = markerGenesDao.getCellTypeMarkerGenes(experimentAccession, ontologyLabelsCellTypeValues);
        //If both(OntologyLabels & AuthorsLabels) returns empty cell types, front end handles this with user friendly message.
        return cellTypeMarkerGenes.stream()
                .filter(markerGene -> !markerGene.cellGroupValue().equals("Not available"))
                .collect(toImmutableList());
    }
}
