package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableList;

public interface MarkerGeneService {
    /**
     * Get marker genes for an experiment of that organism part.
     *
     * @param experimentAccession
     * @param organismPart
     * @return ImmutableList<MarkerGene> of cell type marker genes
     * Objects as a value
     */
    ImmutableList<MarkerGene> getCellTypeMarkerGeneProfile(String experimentAccession, String organismPart);

    /**
     * @param experimentAccession - Id of the experiment
     * @param k                   - no of clusters
     * @return ImmutableList<MarkerGene> of marker genes
     * Objects as a value
     */
    ImmutableList<MarkerGene> getMarkerGenesPerCluster(String experimentAccession, String k);
}
