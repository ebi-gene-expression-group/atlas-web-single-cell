package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.search.CellTypeSearchDao;

import static com.google.common.collect.ImmutableList.toImmutableList;

@Service
public class MarkerGeneService {
    private final MarkerGenesDao markerGenesDao;
    private final CellTypeSearchDao cellTypeSearchDao;
    private static final String CELL_GROUP_TYPE = "inferred cell type - ontology labels";

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
     */
    public ImmutableList<MarkerGene> getCellTypeMarkerGeneProfile(String experimentAccession, ImmutableSet<String> organismPart) {

        var ontologyLabelsCellTypeValues = cellTypeSearchDao.getInferredCellTypeOntologyLabels(experimentAccession, organismPart);

        var cellTypeMarkerGenes = ontologyLabelsCellTypeValues.isEmpty() ?
                markerGenesDao.getCellTypeMarkerGenes(experimentAccession, CELL_GROUP_TYPE,
                        cellTypeSearchDao.getInferredCellTypeAuthorsLabels(experimentAccession, organismPart)) :
                markerGenesDao.getCellTypeMarkerGenes(experimentAccession, CELL_GROUP_TYPE, ontologyLabelsCellTypeValues);

        return cellTypeMarkerGenes.stream()
                .filter(markerGene -> !markerGene.cellGroupValue().equalsIgnoreCase("Not available"))
                .filter(markerGene -> !markerGene.cellGroupValueWhereMarker().equalsIgnoreCase("Not available"))
                .collect(toImmutableList());
    }

    public ImmutableList<MarkerGene> getCellTypeMarkerGeneHeatmap(String experimentAccession, String cellGroupType, ImmutableSet<String> cellType) {


        var cellTypeMarkerGenes = markerGenesDao.getCellTypeMarkerGenes(experimentAccession, cellGroupType, cellType);

        return cellTypeMarkerGenes.stream()
                .filter(markerGene -> !markerGene.cellGroupValue().equalsIgnoreCase("Not available"))
                .filter(markerGene -> !markerGene.cellGroupValueWhereMarker().equalsIgnoreCase("Not available"))
                .collect(toImmutableList());
    }
    /**
     * @param experimentAccession - Id of the experiment
     * @param k                   - no of clusters
     * @return Map<String, ImmutableSet < MarkerGene>> of cluster MarkerGenes
     * Objects as a value
     */
    public ImmutableList<MarkerGene> getMarkerGenesPerCluster(String experimentAccession, String k) {
        return ImmutableList.copyOf(markerGenesDao.getMarkerGenesWithAveragesPerCluster(experimentAccession, k));
    }
}
