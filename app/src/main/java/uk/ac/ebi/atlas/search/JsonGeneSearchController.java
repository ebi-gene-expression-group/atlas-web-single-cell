package uk.ac.ebi.atlas.search;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;
import uk.ac.ebi.atlas.experimentpage.ExperimentAttributesService;
import uk.ac.ebi.atlas.model.experiment.singlecell.SingleCellBaselineExperiment;
import uk.ac.ebi.atlas.search.celltype.CellTypeSearchService;
import uk.ac.ebi.atlas.search.geneids.GeneIdSearchService;
import uk.ac.ebi.atlas.search.geneids.QueryParsingException;
import uk.ac.ebi.atlas.search.organismpart.OrganismPartSearchService;
import uk.ac.ebi.atlas.search.species.SpeciesSearchService;
import uk.ac.ebi.atlas.trader.ExperimentTrader;
import uk.ac.ebi.atlas.utils.StringUtil;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static uk.ac.ebi.atlas.search.FacetGroupName.MARKER_GENE;
import static uk.ac.ebi.atlas.search.FacetGroupName.ORGANISM;
import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
@RequiredArgsConstructor
public class JsonGeneSearchController extends JsonExceptionHandlingController {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonGeneSearchController.class);

    private final GeneIdSearchService geneIdSearchService;
    private final GeneSearchService geneSearchService;
    private final ExperimentTrader experimentTrader;
    private final ExperimentAttributesService experimentAttributesService;

    private final OrganismPartSearchService organismPartSearchService;
    private final CellTypeSearchService cellTypeSearchService;
    private final SpeciesSearchService speciesSearchService;

    @GetMapping(value = "/json/search", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String search(@RequestParam MultiValueMap<String, String> requestParams) {
        var stopWatch = new StopWatch();
        stopWatch.start("geneQuery = geneIdSearchService.getGeneQueryByRequestParams(requestParams)");
        var geneQuery = geneIdSearchService.getGeneQueryByRequestParams(requestParams);
        stopWatch.stop();

        stopWatch.start("geneIds = geneIdSearchService.search(geneQuery)");
        var geneIds = geneIdSearchService.search(geneQuery);
        stopWatch.stop();

        var geneIdValidationError = hasGeneIdNotFoundOrNotExpressed(geneIds);
        if (geneIdValidationError.isPresent()) {
            return geneIdValidationError.get();
        }

        // We found expressed gene IDs, let’s get to it now...
        stopWatch.start("expressedGeneIdEntries = getMarkerGeneProfileByGeneIds(geneIds)");
        var expressedGeneIdEntries = getMarkerGeneProfileByGeneIds(geneIds);
        stopWatch.stop();

        stopWatch.start("markerGeneFacets = geneSearchService.getMarkerGeneProfile(...)");
        var markerGeneFacets =
                geneSearchService.getMarkerGeneProfile(
                        expressedGeneIdEntries.stream()
                                .map(Map.Entry::getKey)
                                .collect(toImmutableSet()));
        stopWatch.stop();

        // geneSearchServiceDao guarantees that values in the inner maps can’t be empty; the map itself may be empty
        // but if there’s an entry the list will have at least on element
        var resultsBuilder = ImmutableList.builder();
        // This was a very expressive stream chain (look it up in the history), but to filter by results where the gene
        // is a marker, making it a for loop is more readable IMHO; I also noticed a 10% performance improvement
        for (var expressedGeneIdEntry : expressedGeneIdEntries) {
            var geneId = expressedGeneIdEntry.getKey();

            for (var exp2cells : expressedGeneIdEntry.getValue().entrySet()) {
                var experimentAccession = exp2cells.getKey();

                stopWatch.start("Get facets iteration for " + geneId + " in " + experimentAccession);
                if (markerGeneFacets.containsKey(geneId) &&
                        markerGeneFacets.get(geneId).containsKey(experimentAccession)) {
                    var cellIds = exp2cells.getValue();

                    var experimentAttributes =
                            ImmutableMap.<String, Object>builder().putAll(
                                    getExperimentInformation(experimentAccession, geneId));

                    var facets =
                            ImmutableList.<Map<String, String>>builder()
                                    .addAll(
                                            unfoldFacets(
                                                    geneSearchService.getFacets(cellIds)
                                                            .getOrDefault(experimentAccession, ImmutableMap.of())));

                    facets.add(
                            ImmutableMap.of(
                                    "group", MARKER_GENE.getTitle(),
                                    "value", "experiments with marker genes",
                                    "label", "Experiments with marker genes",
                                    "description", MARKER_GENE.getTooltip()));
                    experimentAttributes.put(
                            "markerGenes",
                            convertMarkerGeneModel(
                                    experimentAccession,
                                    geneId,
                                    markerGeneFacets.get(geneId).get(experimentAccession)));

                    resultsBuilder.add(ImmutableMap.of("element", experimentAttributes.build(), "facets", facets.build()));
                }
                stopWatch.stop();
            }
        }

        stopWatch.start("Build results and serialize to JSON");
        var results = resultsBuilder.build();

        // If the query term matches a single Ensembl ID different to the query term, we return it in the response.
        // The most common case is a non-Ensembl gene identifier (e.g. Entrez, MGI, ...).
        var matchingGeneIds =
                (Objects.requireNonNull(geneIds.orElse(null)).size() == 1 && !geneIds.get().iterator().next().equals(geneQuery.queryTerm())) ?
                        "(" + String.join(", ", geneIds.get()) + ")" :
                        "";

        var json = GSON.toJson(
                ImmutableMap.of(
                        "matchingGeneId", matchingGeneIds,
                        "results", results,
                        "checkboxFacetGroups", ImmutableList.of(MARKER_GENE.getTitle(), ORGANISM.getTitle())));
        stopWatch.stop();

        LOGGER.debug("Search results for {} in {} ms: {}",
                geneQuery.queryTerm(),
                stopWatch.getTotalTimeMillis(),
                stopWatch.prettyPrint());

        return json;
    }

    // TODO: this contains duplicated code with the `/json/search` endpoint,
    // when the 'gene search' epic has been finished that endpoint - and with that the duplicated code - will be removed
    @GetMapping(value = "/json/gene-search", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String searchForGene(@RequestParam MultiValueMap<String, String> requestParams) {
        var geneQuery = geneIdSearchService.getGeneQueryByRequestParams(requestParams);

        var geneIds = geneIdSearchService.search(geneQuery);

        var geneIdValidationError = hasGeneIdNotFoundOrNotExpressed(geneIds);
        if (geneIdValidationError.isPresent()) {
            return geneIdValidationError.get();
        }

        // We found expressed gene IDs, let’s get to it now...
        var expressedGeneIdEntries = getMarkerGeneProfileByGeneIds(geneIds);

        var markerGeneFacets =
                geneSearchService.getMarkerGeneProfile(
                        expressedGeneIdEntries.stream()
                                .map(Map.Entry::getKey)
                                .collect(toImmutableSet()));

        // geneSearchServiceDao guarantees that values in the inner maps can’t be empty. The map itself may be empty
        // but if there’s an entry the list will have at least one element
        var results =
                expressedGeneIdEntries.stream()
                        // TODO Measure in production if parallelising the stream results in any speedup
                        //      (the more experiments we have the better). BEWARE: just adding parallel() throws! (?)
                        .flatMap(entry -> entry.getValue().entrySet().stream().map(exp2cells -> {

                            // Inside this map-within-a-flatMap we unfold expressedGeneIdEntries to triplets of...
                            var geneId = entry.getKey();
                            var experimentAccession = exp2cells.getKey();

                            var experimentAttributes =
                                    ImmutableMap.<String, Object>builder().putAll(
                                            getExperimentInformation(experimentAccession, geneId));

                            if (markerGeneFacets.containsKey(geneId) &&
                                    markerGeneFacets.get(geneId).containsKey(experimentAccession)) {
                                experimentAttributes.put(
                                        "markerGenes",
                                        convertMarkerGeneModel(
                                                experimentAccession,
                                                geneId,
                                                markerGeneFacets.get(geneId).get(experimentAccession)));
                            } else {
                                experimentAttributes.put(
                                        "markerGenes", ImmutableList.of());
                            }

                            return experimentAttributes.build();

                        })).collect(toImmutableList());

        var matchingGeneIds = "";
        if (geneIds.get().size() == 1 && !geneIds.get().iterator().next().equals(geneQuery.queryTerm())) {
            matchingGeneIds = "(" + String.join(", ", geneIds.get()) + ")";
        }

        return GSON.toJson(
                ImmutableMap.of(
                        "matchingGeneId", matchingGeneIds,
                        "results", results,
                        "checkboxFacetGroups", ImmutableList.of(MARKER_GENE.getTitle(), ORGANISM.getTitle())));
    }

    @GetMapping(value = "/json/gene-search/marker-genes", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Boolean isMarkerGene(@RequestParam MultiValueMap<String, String> requestParams) {
        var geneQuery = geneIdSearchService.getGeneQueryByRequestParams(requestParams);
        var geneIds = geneIdSearchService.search(geneQuery);

        var emptyGeneIdError = hasGeneIdNotFoundOrNotExpressed(geneIds);
        if (emptyGeneIdError.isPresent()) {
            return false;
        }

        var expressedGeneIdEntries =
                getMarkerGeneProfileByGeneIds(geneIds);

        var markerGeneFacets =
                geneSearchService.getMarkerGeneProfile(
                        expressedGeneIdEntries.stream()
                                .map(Map.Entry::getKey)
                                .collect(toImmutableSet()));

        return markerGeneFacets != null && !markerGeneFacets.isEmpty();
    }

    @GetMapping(value = "/json/gene-search/organism-parts",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Set<String> getOrganismPartBySearchTerm(@RequestParam MultiValueMap<String, String> requestParams) {
        var geneQuery = geneIdSearchService.getGeneQueryByRequestParams(requestParams);
        var geneIds = geneIdSearchService.search(geneQuery);

        if (geneIds.isEmpty()) {
            return ImmutableSet.of();
        }

        var cellTypes = requestParams.get("cellTypes");

        return organismPartSearchService.search(geneIds.get(),
                cellTypes != null ? ImmutableSet.copyOf(cellTypes) : ImmutableSet.of());
    }

    @GetMapping(value = "/json/gene-search/cell-types",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Set<String> getCellTypeBySearchTerm(@RequestParam MultiValueMap<String, String> requestParams) {
        var geneQuery = geneIdSearchService.getGeneQueryByRequestParams(requestParams);
        var geneIds = geneIdSearchService.search(geneQuery);

        if (geneIds.isEmpty()) {
            return ImmutableSet.of();
        }

        var organismParts = requestParams.get("organismParts");

        return cellTypeSearchService.search(geneIds.get(),
                organismParts != null ? ImmutableSet.copyOf(organismParts) : ImmutableSet.of());
    }

    @GetMapping(value = "/json/gene-search/species", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ImmutableSet<String> getSpeciesByGeneId(@RequestParam MultiValueMap<String, String> requestParams) {
        var category = geneIdSearchService.getCategoryFromRequestParams(requestParams);
        var queryTerm =
                geneIdSearchService.getFirstNotBlankQueryField(requestParams.get(category))
                        .orElseThrow(() -> new QueryParsingException(
                                String.format("All fields are blank for category: %s", category)));

        return speciesSearchService.search(queryTerm, category);
    }

    private ImmutableList<Map.Entry<String, Map<String, List<String>>>> getMarkerGeneProfileByGeneIds(Optional<ImmutableSet<String>> geneIds) {
        // We found expressed gene IDs, let’s get to it now...
        var geneIds2ExperimentAndCellIds =
                geneSearchService.getCellIdsInExperiments(geneIds.get());

        return geneIds2ExperimentAndCellIds.entrySet().stream()
                        .filter(entry -> !entry.getValue().isEmpty())
                        .collect(toImmutableList());
    }

    private Optional<String> hasGeneIdNotFoundOrNotExpressed(Optional<ImmutableSet<String>> geneIds) {
        if (geneIds.isEmpty()) {
            return Optional.of(GSON.toJson(
                    ImmutableMap.of(
                            "results", ImmutableList.of(),
                            "reason", "Gene unknown")));
        }

        if (geneIds.get().isEmpty()) {
            return Optional.of(GSON.toJson(
                    ImmutableMap.of(
                            "results", ImmutableList.of(),
                            "reason", "No expression found")));
        }

        return Optional.empty();
    }

    private <K, V> ImmutableList<SimpleEntry<K, V>> unfoldListMultimap(Map<K, List<V>> multimap) {
        return multimap.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(value -> new SimpleEntry<>(entry.getKey(), value)))
                .collect(toImmutableList());
    }

    private ImmutableList<ImmutableMap<String, String>> unfoldFacets(Map<String, List<String>> model) {
        var unfoldedModel = unfoldListMultimap(model);
        var results = ImmutableList.<ImmutableMap<String, String>>builder();

        for (var entry : unfoldedModel) {
            var mapBuilder = ImmutableMap.<String, String>builder()
                    .put("value", entry.getValue())
                    .put("label", StringUtils.capitalize(entry.getValue()));

            var facetGroupName = FacetGroupName.fromName(entry.getKey());

            // If this facet is "known", i.e. needs a particular title or tooltip
            if (facetGroupName != null) {
                mapBuilder.put("group", facetGroupName.getTitle());
                if (!isNullOrEmpty(facetGroupName.getTooltip())) {
                    mapBuilder.put("description", facetGroupName.getTooltip());
                }
            }
            else {
                mapBuilder.put("group", StringUtil.snakeCaseToDisplayName(entry.getKey()));
            }

            results.add(mapBuilder.build());
        }
        return results.build();
    }

    private ImmutableMap<String, Object> getExperimentInformation(String experimentAccession, String geneId) {
        var experiment = (SingleCellBaselineExperiment) experimentTrader.getPublicExperiment(experimentAccession);
        var experimentAttributes =
                ImmutableMap.<String, Object>builder().putAll(experimentAttributesService.getAttributes(experiment));
        experimentAttributes.put("url", createExperimentPageURL(experimentAccession, geneId));

        return experimentAttributes.build();
    }

    private ImmutableList<ImmutableMap<String, Object>> convertMarkerGeneModel(String experimentAccession,
                                                                               String geneId,
                                                                               Map<Integer, List<Integer>> model) {
        return model.entrySet().stream()
                .map(entry ->
                        ImmutableMap.of(
                                "k", entry.getKey(),
                                "clusterIds", entry.getValue(),
                                "url", createResultsPageURL(
                                        experimentAccession, geneId, entry.getKey(), entry.getValue())))
                .collect(toImmutableList());
    }

    private static String createExperimentPageURL(String experimentAccession, String geneId) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/experiments/{experimentAccession}")
                .query("geneId={geneId}")
                .buildAndExpand(experimentAccession, geneId)
                .toUriString();
    }

    private static String createResultsPageURL(String experimentAccession,
                                               String geneId,
                                               Integer k,
                                               List<Integer> clusterId) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/experiments/{experimentAccession}/results")
                .query("geneId={geneId}")
                .query("k={k}")
                .query("clusterId={clusterId}")
                .buildAndExpand(experimentAccession, geneId, k, clusterId)
                .encode()
                .toUriString();
    }
}
