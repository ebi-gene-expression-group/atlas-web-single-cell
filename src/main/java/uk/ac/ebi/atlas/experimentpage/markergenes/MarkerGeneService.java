package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Map;

public interface MarkerGeneService {
    /**
     * Get marker genes for an experiment of that organism part.
     *
     * @param experimentAccession
     * @param organismPart
     * @return Map<String, ImmutableSet<MarkerGene>> of cell type as a key and top 5 MarkerGene
     * Objects as a value
     */
    public Map<String, ImmutableSet<MarkerGene>> getCellTypeMarkerGeneProfile(String experimentAccession, String organismPart);

    /**
     * @param experimentAccession - Id of the experiment
     * @param k - no of clusters
     * @return Map<String, ImmutableSet<MarkerGene>> of cluster id as a key and top 5 MarkerGene
     * Objects as a value
     */
    public Map<String, ImmutableSet<MarkerGene>> getMarkerGenesPerCluster(String experimentAccession, String k);
}
