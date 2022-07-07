package uk.ac.ebi.atlas.search;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.atlassian.util.concurrent.LazyReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;
import uk.ac.ebi.atlas.search.suggester.AnalyticsSuggesterService;
import uk.ac.ebi.atlas.search.suggester.SolrSuggestionReactSelectAdapter;
import uk.ac.ebi.atlas.search.suggester.SuggesterService;

import static java.util.stream.Collectors.toList;
import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class SuggestionsController extends JsonExceptionHandlingController {
    protected static final int FEATURED_SPECIES = 0;

    private final SuggesterService suggesterService;
    private final FeaturedSpeciesService featuredSpeciesService;
    private final AnalyticsSuggesterService analyticsSuggesterService;

    // This is the only state field in the controller, but it won’t vary throughout the lifecycle of the bean, so we
    // can go with the default @Scope which is SINGLETON for a @RestController
    private final LazyReference<String> speciesSelectJson = new LazyReference<>() {
        @Override
        protected String create() {
            var topSpeciesNames = featuredSpeciesService.getSpeciesNamesSortedByExperimentCount();

            return GSON.toJson(
                    ImmutableMap.of(
                            "topSpecies",
                            topSpeciesNames.subList(0, Math.min(topSpeciesNames.size(), FEATURED_SPECIES)),
                            "allSpecies",
                            ImmutableList.sortedCopyOf(topSpeciesNames)));
        }
    };

    public SuggestionsController(SuggesterService suggesterService, FeaturedSpeciesService featuredSpeciesService,
                                 AnalyticsSuggesterService analyticsSuggesterService) {
        this.suggesterService = suggesterService;
        this.featuredSpeciesService = featuredSpeciesService;
        this.analyticsSuggesterService = analyticsSuggesterService;
    }

    @GetMapping(value = "/json/suggestions")
    public String fetchTopSuggestions(
            @RequestParam(value = "query") String query,
            @RequestParam(value = "species", required = false, defaultValue = "") String species) {
        return GSON.toJson(
                suggesterService.fetchIdentifiers(query, species.split(",")).collect(toList()));
    }

    // If needed we can have request parameters to determine the format of the reply (for react-select or not) and
    // expose the highlight boolean as well
    @GetMapping(value = "/json/suggestions/gene_ids")
    public String fetchGeneIdSuggestions(
            @RequestParam(value = "query") String query,
            @RequestParam(value = "species", required = false, defaultValue = "") String species) {

        return GSON.toJson(
          SolrSuggestionReactSelectAdapter.serialize(
            suggesterService.aggregateGeneIdAndMetadataSuggestions(query, species.split(","))));
    }

    @RequestMapping(value = "/json/suggestions/species")
    public String fetchTopSuggestions() {
        return speciesSelectJson.get();
    }

    @GetMapping(value = "/json/suggestions/meta_data")
    public String fetchMetadataSuggestions(@RequestParam(value = "query") String query,
                                           @RequestParam(value = "species", required = false, defaultValue = "") String species){
      return GSON.toJson(SolrSuggestionReactSelectAdapter.serialize(
                analyticsSuggesterService.fetchMetadataSuggestions(
                query,species.split(","))));
    }

    @GetMapping(value = "/json/suggestions/gene_search")
    public String fetchGeneIdAndMetadataSuggestions(
            @RequestParam(value = "query") String query,
            @RequestParam(value = "species", required = false, defaultValue = "") String species) {
        return GSON.toJson(SolrSuggestionReactSelectAdapter.serialize(
          suggesterService.aggregateGeneIdAndMetadataSuggestions(query, species.split(","))));
    }
}
