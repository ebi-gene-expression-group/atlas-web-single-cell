package uk.ac.ebi.atlas.search;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experimentpage.tsneplot.TSnePlotSettingsService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

@Component
public class GeneSearchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeneSearchService.class);

    private final GeneSearchDao geneSearchDao;
    private final TSnePlotSettingsService tsnePlotSettingsService;

    public GeneSearchService(GeneSearchDao geneSearchDao,
                             TSnePlotSettingsService tsnePlotSettingsService) {
        this.geneSearchDao = geneSearchDao;
        this.tsnePlotSettingsService = tsnePlotSettingsService;
    }

    // Map<Gene ID, Map<Experiment accession, List<Cell IDs>>>
    public Map<String, Map<String, List<String>>> getCellIdsInExperiments(ImmutableCollection<String> geneIds) {
        return fetchInParallel(geneIds, geneSearchDao::fetchCellIds);
    }

    // Returns inferred cell types and organism parts for each experiment accession
    public ImmutableMap<String, Map<String, List<String>>> getFacets(List<String> cellIds) {
        return geneSearchDao.getFacets(
                cellIds,
                "inferred_cell_type_-_ontology_labels", "organism", "organism_part");
    }

    // Map<Gene ID, Map<Experiment accession, Map<K, Cluster ID>>>
    public ImmutableMap<String, Map<String, Map<Integer, List<Integer>>>>
    getMarkerGeneProfile(ImmutableCollection<String> geneIds) {
        return fetchInParallel(
                geneIds,
                geneId -> fetchClusterIDWithPreferredKAndMinPForGeneID(
                        geneSearchDao.fetchExperimentAccessionsWhereGeneIsMarker(geneId),
                        geneId));
    }

    public ImmutableSet<String> getCellIdsFromGeneIds(ImmutableSet<String> geneIds) {
        return geneIds.stream()
                .map(geneSearchDao::fetchCellIds)
                .flatMap(geneIdsToCellIds -> geneIdsToCellIds.values().stream())
                .flatMap(Collection::stream)
                .collect(toImmutableSet());
    }

    private <T> ImmutableMap<String, T>
    fetchInParallel(ImmutableCollection<String> geneIds, Function<String, T> geneIdInfoProvider) {
        // If this becomes a resource hog, consider having the pool as a member of the class and reuse it every time
        var forkJoinPool = new ForkJoinPool();
        try {

            return
                    forkJoinPool.submit(
                            () -> geneIds.stream().parallel()
                                    .map(geneId -> ImmutableMap.of(geneId, geneIdInfoProvider.apply(geneId)))
                                    .map(Map::entrySet)
                                    .flatMap(Set::stream)
                                    .collect(toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))
                            .get();

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            forkJoinPool.shutdown();
        }
    }

    private ImmutableMap<String, Map<Integer, List<Integer>>> fetchClusterIDWithPreferredKAndMinPForGeneID(
            List<String> experimentAccessions, String geneId) {
        var result = ImmutableMap.<String, Map<Integer, List<Integer>>>builder();

        for (var experimentAccession : experimentAccessions) {
            var geneIds2MinimumProbability = geneSearchDao.fetchMinimumMarkerProbability(experimentAccession);
            var preferredK = tsnePlotSettingsService.getExpectedClusters(experimentAccession);

            var clusterIDWithPreferredKAndMinP =
                    geneSearchDao.fetchClusterIdsWithPreferredKAndMinPForExperimentAccession(
                            geneId,
                            experimentAccession,
                            // If there’s no preferred k we use 0, which won’t match any row
                            preferredK.orElse(0),
                            geneIds2MinimumProbability.getOrDefault(geneId, 0.0));
            if (!clusterIDWithPreferredKAndMinP.isEmpty()) {
                result.put(experimentAccession, clusterIDWithPreferredKAndMinP);
            }
        }

        return result.build();
    }
}
