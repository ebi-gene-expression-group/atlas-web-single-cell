package uk.ac.ebi.atlas.search;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;
import uk.ac.ebi.atlas.experimentpage.ExperimentAttributesService;
import uk.ac.ebi.atlas.model.experiment.singlecell.SingleCellBaselineExperiment;
import uk.ac.ebi.atlas.search.geneids.GeneIdSearchService;
import uk.ac.ebi.atlas.search.geneids.GeneQuery;
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
import static java.util.stream.Collectors.toList;
import static uk.ac.ebi.atlas.search.FacetGroupName.MARKER_GENE;
import static uk.ac.ebi.atlas.search.FacetGroupName.ORGANISM;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.BIOENTITY_PROPERTY_NAMES;
import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
@RequiredArgsConstructor
public class JsonGeneSearchController extends JsonExceptionHandlingController {
    private final static ImmutableSet<String> VALID_QUERY_FIELDS =
            ImmutableSet.<String>builder()
                    .add("q")
                    .addAll(
                            BIOENTITY_PROPERTY_NAMES.stream()
                                    .map(propertyName -> propertyName.name)
                                    .collect(toImmutableSet()))
                    .build();
    public static final String ALL_CATEGORIES = "*";

    private final GeneIdSearchService geneIdSearchService;
    private final GeneSearchService geneSearchService;
    private final ExperimentTrader experimentTrader;
    private final ExperimentAttributesService experimentAttributesService;

    private final SpeciesSearchService speciesSearchService;

    @RequestMapping(value = "/json/search",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String search(@RequestParam MultiValueMap<String, String> requestParams) {
        GeneQuery geneQuery = geneIdSearchService.getGeneQueryByRequestParams(requestParams);

        var geneIds = geneIdSearchService.search(geneQuery);

        String emptyGeneIdError = geneIdEmptyValidation(geneIds);
        if (emptyGeneIdError != null) {
            return emptyGeneIdError;
        }

        // We found expressed gene IDs, let’s get to it now...
        List<Map.Entry<String, Map<String, List<String>>>> expressedGeneIdEntries =
                getMarkerGeneProfileByGeneIds(geneIds);

        var markerGeneFacets =
                geneSearchService.getMarkerGeneProfile(
                        expressedGeneIdEntries.stream()
                                .map(Map.Entry::getKey)
                                .toArray(String[]::new));

        // geneSearchServiceDao guarantees that values in the inner maps can’t be empty. The map itself may be empty
        // but if there’s an entry the list will have at least on element
        var results =
                expressedGeneIdEntries.stream()
                        // TODO Measure in production if parallelising the stream results in any speedup
                        //      (the more experiments we have the better). BEWARE: just adding parallel() throws! (?)
                        .flatMap(entry -> entry.getValue().entrySet().stream().map(exp2cells -> {

                            // Inside this map-within-a-flatMap we unfold expressedGeneIdEntries to triplets of...
                            var geneId = entry.getKey();
                            var experimentAccession = exp2cells.getKey();
                            var cellIds = exp2cells.getValue();

                            var experimentAttributes =
                                    ImmutableMap.<String, Object>builder().putAll(
                                            getExperimentInformation(experimentAccession, geneId));
                            var facets =
                                    ImmutableList.<Map<String, String>>builder().addAll(
                                            unfoldFacets(geneSearchService.getFacets(cellIds)
                                                    .getOrDefault(experimentAccession, ImmutableMap.of())));

                            if (markerGeneFacets.containsKey(geneId) &&
                                    markerGeneFacets.get(geneId).containsKey(experimentAccession)) {
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
                            } else {
                                experimentAttributes.put(
                                        "markerGenes", ImmutableList.of());
                            }

                            return ImmutableMap.of("element", experimentAttributes.build(), "facets", facets.build());

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

    @RequestMapping(value = "/json/search/marker-genes",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Boolean isMarkerGene(@RequestParam MultiValueMap<String, String> requestParams) {
        if (isRequestParamsEmpty(requestParams)) {
            return false;
        }

        GeneQuery geneQuery = geneIdSearchService.getGeneQueryByRequestParams(requestParams);
        var geneIds = geneIdSearchService.search(geneQuery);

        String emptyGeneIdError = geneIdEmptyValidation(geneIds);
        if (emptyGeneIdError != null) {
            return false;
        }

        List<Map.Entry<String, Map<String, List<String>>>> expressedGeneIdEntries =
                getMarkerGeneProfileByGeneIds(geneIds);

        var markerGeneFacets =
                geneSearchService.getMarkerGeneProfile(
                        expressedGeneIdEntries.stream()
                                .map(Map.Entry::getKey)
                                .toArray(String[]::new));

        return markerGeneFacets != null && markerGeneFacets.size() > 0;
    }

    @RequestMapping(value = "/json/search/species",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Set<String> getSpeciesByGeneId(@RequestParam MultiValueMap<String, String> requestParams) {
        if (isRequestParamsEmpty(requestParams)) {
            return Set.of();
        }

        var category = geneIdSearchService.getCategoryFromRequestParams(requestParams);
        var searchText = requestParams.getFirst(category);

        if (category.equals("q")) {
            category = ALL_CATEGORIES;
        }

        var species = speciesSearchService.search(searchText, category);

        return species.orElse(ImmutableSet.of());
    }

    private List<Map.Entry<String, Map<String, List<String>>>> getMarkerGeneProfileByGeneIds(Optional<ImmutableSet<String>> geneIds) {
        // We found expressed gene IDs, let’s get to it now...
        var geneIds2ExperimentAndCellIds =
                geneSearchService.getCellIdsInExperiments(geneIds.get().toArray(new String[0]));

        return geneIds2ExperimentAndCellIds.entrySet().stream()
                        .filter(entry -> !entry.getValue().isEmpty())
                        .collect(toList());
    }

    private boolean isRequestParamsEmpty(MultiValueMap<String, String> requestParams) {
        return requestParams == null
                || requestParams.size() == 0
                || (requestParams.containsKey("q") && Objects.equals(requestParams.getFirst("q"), ""));
    }

    private String geneIdEmptyValidation(Optional<ImmutableSet<String>> geneIds) {
        if (geneIds.isEmpty()) {
            return GSON.toJson(
                    ImmutableMap.of(
                            "results", ImmutableList.of(),
                            "reason", "Gene unknown"));
        }

        if (geneIds.get().isEmpty()) {
            return GSON.toJson(
                    ImmutableMap.of(
                            "results", ImmutableList.of(),
                            "reason", "No expression found"));
        }

        return null;
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
