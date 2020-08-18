package uk.ac.ebi.atlas.experimentpage.markergenes;

import java.util.List;

public interface MarkerGeneService {
    /**
     * Get marker genes for an experiment of that organism part.
     *
     * @param experimentAccession
     * @param organismPart
     * @return List of CellTypeMarkerGene Objects
     */
    List<CellTypeMarkerGene> getCellTypeMarkerGenes(String experimentAccession, String organismPart);
}
