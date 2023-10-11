package uk.ac.ebi.atlas.experimentpage.tabs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.model.experiment.ExperimentDesignTable;
import uk.ac.ebi.atlas.trader.ExperimentDesignDao;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@Component
public class ExperimentPageContentSerializer {
    private final ExperimentTrader experimentTrader;
    private final ExperimentDesignDao experimentDesignDao;
    private final ExperimentPageContentService experimentPageContentService;

    public ExperimentPageContentSerializer(ExperimentTrader experimentTrader,
                                           ExperimentDesignDao experimentDesignDao,
                                           ExperimentPageContentService experimentPageContentService) {
        this.experimentTrader = experimentTrader;
        this.experimentPageContentService = experimentPageContentService;
        this.experimentDesignDao = experimentDesignDao;
    }

    @Cacheable(cacheNames = "jsonExperimentPageTabs", key = "#experimentAccession")
    public String experimentPageContentForExperiment(String experimentAccession, final String accessKey) {
        var experiment = experimentTrader.getExperiment(experimentAccession, accessKey);
        var result = new JsonObject();

        result.addProperty("experimentAccession", experiment.getAccession());
        result.addProperty("accessKey", accessKey);
        result.addProperty("species", experiment.getSpecies().getReferenceName());
        result.addProperty("disclaimer", experiment.getDisclaimer());

        var availableTabs = new JsonArray();

        var results = experimentPageContentService.getTsnePlotData(experiment.getAccession());
        var experimentDesignJson =
                experimentPageContentService.getExperimentDesign(
                        experiment.getAccession(), new ExperimentDesignTable(experiment, experimentDesignDao).asJson(), accessKey);
        availableTabs.add(customContentTab("results", "Results", results));
        availableTabs.add(customContentTab("experiment-design", "Experiment Design", experimentDesignJson));

        var sections = new JsonObject();
        sections.add("sections", experimentPageContentService.getSupplementaryInformation(experiment.getAccession()));
        availableTabs.add(customContentTab("supplementary-information", "Supplementary Information", sections));

        var data = new JsonObject();
        data.add("data", experimentPageContentService.getDownloads(experiment.getAccession(), accessKey));
        availableTabs.add(customContentTab("downloads", "Downloads", data));

        result.add("tabs", availableTabs);
        return GSON.toJson(result);
    }

    private JsonObject customContentTab(String tabType, String name, JsonObject props) {
        var result = new JsonObject();
        result.addProperty("type", tabType);
        result.addProperty("name", name);
        result.add("props", props);
        return result;
    }
}
