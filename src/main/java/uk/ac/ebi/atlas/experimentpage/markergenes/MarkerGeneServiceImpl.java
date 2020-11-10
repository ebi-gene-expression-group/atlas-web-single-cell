package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;


@Service
public class MarkerGeneServiceImpl implements MarkerGeneService {
    private MarkerGenesDao markerGenesDao;

    public MarkerGeneServiceImpl(MarkerGenesDao markerGenesDao) {
        this.markerGenesDao = markerGenesDao;
    }


     /**
     *
     * @param experimentAccession - Id of the experiment
     * @param organismPart - ontology label or authors label(ontology URL or authors URL)
     * @return Map<String, ImmutableSet<MarkerGene>> of cell type as a key and top 5 MarkerGene
     * Objects as a value
     */
    @Override
    public Map<String, ImmutableSet<MarkerGene>> getCellTypeMarkerGeneProfile(String experimentAccession, String organismPart) {
        return markerGenesDao.getCellTypeMarkerGenes(experimentAccession, organismPart)
                .stream()
                .collect(groupingBy(MarkerGene::cellGroupValue, toImmutableSet())).entrySet().stream()
                .collect(toMap(cellTypeMarkerGenes -> cellTypeMarkerGenes.getKey(),
                        cellTypeMarkerGenes -> cellTypeMarkerGenes.getValue()
                                .stream()
                                .limit(5)
                                .collect(toImmutableSet())));
    }

    /**
     * @param experimentAccession - Id of the experiment
     * @param k                   - no of clusters
     * @return Map<String, ImmutableSet < MarkerGene>> of cluster id as a key and top 5 MarkerGene
     * Objects as a value
     */
    @Override
    public Map<String, ImmutableSet<MarkerGene>> getMarkerGenesPerCluster(String experimentAccession, String k) {
        return markerGenesDao.getMarkerGenesWithAveragesPerCluster(experimentAccession, k)
                .stream()
                .collect(groupingBy(MarkerGene::cellGroupValue, toImmutableSet())).entrySet().stream()
                .collect(toMap(markerGenes -> markerGenes.getKey(), // cluster id as a key
                        markerGenes -> markerGenes.getValue()// Gets only first 5 marker genes for each cluster id
                                .stream()
                                .limit(5)
                                .collect(toImmutableSet())));
    }
}
