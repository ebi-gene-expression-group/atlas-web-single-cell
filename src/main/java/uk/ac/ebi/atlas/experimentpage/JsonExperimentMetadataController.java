package uk.ac.ebi.atlas.experimentpage;

import com.google.common.collect.ImmutableMap;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.experimentpage.json.JsonExperimentController;
import uk.ac.ebi.atlas.experimentpage.tabs.ExperimentPageContentService;
import uk.ac.ebi.atlas.trader.ScxaExperimentTrader;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class JsonExperimentMetadataController extends JsonExperimentController {
    private final ExperimentPageContentService experimentPageContentService;

    public JsonExperimentMetadataController(ScxaExperimentTrader experimentTrader,
                                            ExperimentPageContentService experimentPageContentService) {
        super(experimentTrader);
        this.experimentPageContentService = experimentPageContentService;
    }

    // TODO In time this should be part of a larger experiment API which we can query to get useful info about it
    @Cacheable(
            cacheNames = "jsonExperimentMetadata", key = "{#experimentAccession, 'tSnePlot'}", sync = true)
    @RequestMapping(value = "json/experiments/{experimentAccession}/metadata/tsneplot",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getTSnePlotMetadata(@PathVariable String experimentAccession,
                                      @RequestParam(value = "accessKey", defaultValue = "") String accessKey) {
        var experiment = experimentTrader.getExperiment(experimentAccession, accessKey);

        return GSON.toJson(
                ImmutableMap.of(
                        "perplexities", experimentPageContentService.getPerplexities(experiment.getAccession()),
                        "metadata", experimentPageContentService.getMetadata(experiment.getAccession())));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "experimentByAccession", allEntries = true),
            @CacheEvict(cacheNames = "experimentsByType", allEntries = true),
            @CacheEvict(cacheNames = "jsonExperimentMetadata", key = "{#experimentAccession, 'tSnePlot'}") })
    @RequestMapping(value = "json/experiments/{experimentAccession}/metadata/tsneplot/clearcache",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String foobar(@PathVariable String experimentAccession) {
        return "";
    }
}
