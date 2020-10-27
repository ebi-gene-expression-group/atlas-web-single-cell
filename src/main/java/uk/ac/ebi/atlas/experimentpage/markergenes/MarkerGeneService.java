package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableList;

public interface MarkerGeneService {
    /**
     * Get marker genes for an experiment of that organism part.
     *
     * @param experimentAccession
     * @param organismPart
     * @return ImmutableList of CellTypeMarkerGene Objects
     */
    ImmutableList<MarkerGene> getCellTypeMarkerGeneProfile(String experimentAccession, String organismPart);
}
