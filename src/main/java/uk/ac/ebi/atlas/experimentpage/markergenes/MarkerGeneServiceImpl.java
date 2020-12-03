package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Service;


@Service
public class MarkerGeneServiceImpl implements MarkerGeneService {
    private final MarkerGenesDao markerGenesDao;

    public MarkerGeneServiceImpl(MarkerGenesDao markerGenesDao) {
        this.markerGenesDao = markerGenesDao;
    }


    /**
     * @param experimentAccession - Id of the experiment
     * @param organismPart        - ontology label or authors label(ontology URL or authors URL)
     * @return Map<String, ImmutableSet < MarkerGene>> of cell type MarkerGenes
     * Objects as a value
     */
    @Override
    public ImmutableList<MarkerGene> getCellTypeMarkerGeneProfile(String experimentAccession, String organismPart) {
        return ImmutableList.copyOf(markerGenesDao.getCellTypeMarkerGenes(experimentAccession, organismPart));
    }

    /**
     * @param experimentAccession - Id of the experiment
     * @param k                   - no of clusters
     * @return Map<String, ImmutableSet < MarkerGene>> of cluster MarkerGenes
     * Objects as a value
     */
    @Override
    public ImmutableList<MarkerGene> getMarkerGenesPerCluster(String experimentAccession, String k) {
        return ImmutableList.copyOf(markerGenesDao.getMarkerGenesWithAveragesPerCluster(experimentAccession, k));
    }
}
