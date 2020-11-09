package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableSet;
import java.util.Map;

public interface MarkerGeneService {
    /**
     * Get marker genes for an experiment of that organism part.
     * @param experimentAccession
     * @param organismPart
     * @return ImmutableList of CellTypeMarkerGene Objects
     */
    Map<String, ImmutableSet<CellTypeMarkerGene>> getCellTypeMarkerGeneProfile(String experimentAccession, String organismPart);
}
