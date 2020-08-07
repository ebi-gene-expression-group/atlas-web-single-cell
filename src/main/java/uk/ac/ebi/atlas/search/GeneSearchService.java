package uk.ac.ebi.atlas.search;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import uk.ac.ebi.atlas.experimentpage.tsneplot.TSnePlotSettingsService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

@Component
public class GeneSearchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeneSearchService.class);

    private final GeneSearchDao geneSearchDao;
    private final TSnePlotSettingsService tsnePlotSettingsService;

    public GeneSearchService(GeneSearchDao geneSearchDao, TSnePlotSettingsService tsnePlotSettingsService) {
        this.geneSearchDao = geneSearchDao;
        this.tsnePlotSettingsService = tsnePlotSettingsService;

    }

    // Map<Gene ID, Map<Experiment accession, List<Cell IDs>>>
    public Map<String, Map<String, List<String>>> getCellIdsInExperiments(String... geneIds) {
        return fetchInParallel(
                ImmutableSet.copyOf(geneIds),
                geneSearchDao::fetchCellIds);
    }

    // Returns inferred cell types and organism parts for each experiment accession
    public ImmutableMap<String, Map<String, List<String>>> getFacets(List<String> cellIds) {
        return geneSearchDao.getFacets(
                cellIds,
                "inferred_cell_type", "organism", "organism_part");
    }

    // Map<Gene ID, Map<Experiment accession, Map<K, Cluster ID>>>
    public ImmutableMap<String, Map<String, Map<Integer, List<Integer>>>> getMarkerGeneProfile(String... geneIds) {

        return fetchInParallel(
                ImmutableSet.copyOf(geneIds),
                geneId -> fetchClusterIDWithPreferredKAndMinPForGeneID(
                        geneSearchDao.fetchExperimentAccessionsWhereGeneIsMarker(geneId),
                        geneId));
    }

    private <T> ImmutableMap<String, T> fetchInParallel(Set<String> geneIds, Function<String, T> geneIdInfoProvider) {
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

        var prefKStopWatch = new StopWatch("Preferred K");
        var clusterIdStopWatch = new StopWatch("Cluster ID with pref. K and min P");

        for (var experimentAccession : experimentAccessions) {
            prefKStopWatch.start(experimentAccession);
            var preferredK = tsnePlotSettingsService.getExpectedClusters(experimentAccession);
            prefKStopWatch.stop();

            clusterIdStopWatch.start(experimentAccession);
            var clusterIDWithPreferredKAndMinP =
                    geneSearchDao.fetchClusterIdsWithPreferredKAndMinPForExperimentAccession(
                            geneId,
                            experimentAccession,
                            // If there’s no preferred k we use 0, which won’t match any row
                            preferredK.orElse(0));
            clusterIdStopWatch.stop();
            if (!clusterIDWithPreferredKAndMinP.isEmpty()) {
                result.put(experimentAccession, clusterIDWithPreferredKAndMinP);
            }
        }

        LOGGER.debug(prefKStopWatch.prettyPrint());
        LOGGER.debug(clusterIdStopWatch.prettyPrint());

        return result.build();
    }
}
