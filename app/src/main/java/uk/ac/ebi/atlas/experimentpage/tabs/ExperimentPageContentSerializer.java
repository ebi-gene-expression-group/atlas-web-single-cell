package uk.ac.ebi.atlas.experimentpage.tabs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import uk.ac.ebi.atlas.experimentpage.ExperimentController;
import uk.ac.ebi.atlas.model.experiment.ExperimentDesignTable;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@Component
public class ExperimentPageContentSerializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentPageContentSerializer.class);

    private final ExperimentTrader experimentTrader;
    private final ExperimentPageContentService experimentPageContentService;

    public ExperimentPageContentSerializer(ExperimentTrader experimentTrader,
                                           ExperimentPageContentService experimentPageContentService) {
        this.experimentTrader = experimentTrader;
        this.experimentPageContentService = experimentPageContentService;
    }

    @Cacheable(cacheNames = "jsonExperimentPageTabs", key = "#experimentAccession")
    public String experimentPageContentForExperiment(String experimentAccession, final String accessKey) {
        var stopwatch = new StopWatch(this.getClass().getSimpleName());

        var experiment = experimentTrader.getExperiment(experimentAccession, accessKey);
        var result = new JsonObject();

        result.addProperty("experimentAccession", experiment.getAccession());
        result.addProperty("accessKey", accessKey);
        result.addProperty("species", experiment.getSpecies().getReferenceName());
        result.addProperty("disclaimer", experiment.getDisclaimer());

        var availableTabs = new JsonArray();

        stopwatch.start("Add tab Results");
        var results = experimentPageContentService.getTsnePlotData(experiment.getAccession());
        availableTabs.add(customContentTab("results", "Results", results));
        stopwatch.stop();

        stopwatch.start("Add tab Experiment Design");
        var experimentDesignJson =
                experimentPageContentService.getExperimentDesign(
                        experiment.getAccession(), new ExperimentDesignTable(experiment).asJson(), accessKey);
        availableTabs.add(customContentTab("experiment-design", "Experiment Design", experimentDesignJson));
        stopwatch.stop();

        stopwatch.start("Add tab Supplementary Information");
        var sections = new JsonObject();
        sections.add("sections", experimentPageContentService.getSupplementaryInformation(experiment.getAccession()));
        availableTabs.add(customContentTab("supplementary-information", "Supplementary Information", sections));
        stopwatch.stop();

        stopwatch.start("Add tab Downloads");
        var data = new JsonObject();
        data.add("data", experimentPageContentService.getDownloads(experiment.getAccession(), accessKey));
        availableTabs.add(customContentTab("downloads", "Downloads", data));
        stopwatch.stop();

        result.add("tabs", availableTabs);

        stopwatch.start("Serialize tabs to JSON");
        var jsonResult = GSON.toJson(result);
        stopwatch.stop();

        LOGGER.debug(stopwatch.prettyPrint());

        return jsonResult;
    }

    private JsonObject customContentTab(String tabType, String name, JsonObject props) {
        var result = new JsonObject();
        result.addProperty("type", tabType);
        result.addProperty("name", name);
        result.add("props", props);
        return result;
    }
}
